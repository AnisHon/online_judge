package com.anishan.content.service;

import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.api.util.AccountPolicy;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.dto.AdminCommentPageQuery;
import com.anishan.content.domain.dto.CommentsStateRequest;
import com.anishan.content.domain.entity.SolutionComment;
import com.anishan.content.domain.entity.SolutionModerationAction;
import com.anishan.content.domain.enumeration.CommentState;
import com.anishan.content.domain.vo.AdminCommentVo;
import com.anishan.content.domain.vo.ModerationActionVo;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.SolutionCommentMapper;
import com.anishan.content.mapper.SolutionExplanationMapper;
import com.anishan.content.mapper.SolutionModerationActionMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Comment deletion, moderation audit, and comment-open state within content-service. */
@Service
public class CommentModerationService {
    private static final long DEFAULT_PAGE_SIZE = 20L;
    private static final long MAX_PAGE_SIZE = 50L;
    private static final String SOLUTION_EDIT = "problem:solution:edit";
    private static final String COMMENT_LIST = "problem:comment:list";
    private static final String COMMENT_REMOVE = "problem:comment:remove";

    private final SolutionCommentMapper commentMapper;
    private final SolutionExplanationMapper solutionMapper;
    private final SolutionModerationActionMapper actionMapper;
    private final ContentEventOutboxService outbox;
    private final SolutionDomainMigrationGate migrationGate;
    private final Clock clock;

    public CommentModerationService(SolutionCommentMapper commentMapper,
                                    SolutionExplanationMapper solutionMapper,
                                    SolutionModerationActionMapper actionMapper,
                                    ContentEventOutboxService outbox,
                                    SolutionDomainMigrationGate migrationGate,
                                    @Qualifier("contentCommunityOutboxClock") Clock clock) {
        this.commentMapper = commentMapper;
        this.solutionMapper = solutionMapper;
        this.actionMapper = actionMapper;
        this.outbox = outbox;
        this.migrationGate = migrationGate;
        this.clock = clock;
    }

    /** The comment author may remove their own comment even when COMMENT_DENY is assigned. */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteOwn(Long commentId) {
        long actorId = requireActor();
        migrationGate.requireCutover();
        return delete(commentId, actorId, false, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAsAdmin(Long commentId, String rawReason) {
        AccountPolicy.requireAuthority(COMMENT_REMOVE);
        long operatorId = AccountPolicy.requireAllowed(AccountPolicy.SOLUTION_DENY);
        migrationGate.requireCutover();
        String reason = normalizeReason(rawReason);
        return delete(commentId, operatorId, true, reason);
    }

    @Transactional(readOnly = true)
    public PagedResult<AdminCommentVo> pageAdmin(AdminCommentPageQuery query) {
        AccountPolicy.requireAuthority(COMMENT_LIST);
        migrationGate.requireCutover();
        Long solutionId = query == null ? null : query.getSolutionId();
        CommentState state = query == null ? null : query.getState();
        long currentPage = query == null || query.getCurrentPage() == null
                ? 1L : query.getCurrentPage();
        long pageSize = query == null || query.getPageSize() == null
                ? DEFAULT_PAGE_SIZE : query.getPageSize();
        if ((solutionId != null && solutionId <= 0) || currentPage < 1
                || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ApiStatusException(400, "分页参数有误");
        }
        long offset;
        try {
            offset = Math.multiplyExact(currentPage - 1, pageSize);
        } catch (ArithmeticException invalid) {
            throw new ApiStatusException(400, "分页参数有误");
        }

        String stateValue = state == null ? null : state.name();
        long total = commentMapper.countAdminComments(solutionId, stateValue);
        List<SolutionComment> rows = commentMapper.selectAdminComments(
                solutionId, stateValue, offset, pageSize);
        List<Long> targetIds = new ArrayList<>();
        if (rows != null) {
            for (SolutionComment row : rows) {
                if (row != null && row.getCommentId() != null) targetIds.add(row.getCommentId());
            }
        }
        Map<Long, SolutionModerationAction> latestActions = loadLatestActions(targetIds);
        List<AdminCommentVo> data = new ArrayList<>();
        if (rows != null) {
            for (SolutionComment row : rows) {
                data.add(toAdminVo(row, latestActions.get(row.getCommentId())));
            }
        }
        PagedResult<AdminCommentVo> result = new PagedResult<>();
        result.setCurrentPage(currentPage);
        result.setPageSize(pageSize);
        result.setTotalRecords(total);
        result.setData(data);
        return result;
    }

    /** The author sees only their own action/reason/time, never operator or target audit IDs. */
    @Transactional(readOnly = true)
    public List<ModerationActionVo> ownActions(Long commentId) {
        long actorId = requireActor();
        migrationGate.requireCutover();
        validateId(commentId, "评论ID有误");
        SolutionComment comment = commentMapper.selectReplyTarget(commentId);
        if (comment == null || !Objects.equals(comment.getUserId(), actorId)) {
            throw new ApiStatusException(404, "评论不存在或无权访问");
        }
        return actionMapper.selectOwnCommentActions(commentId, actorId);
    }

    /** Author or solution-edit manager can close/open the resource; no remote call is needed. */
    @Transactional(rollbackFor = Exception.class)
    public boolean setCommentsOpen(Long solutionId, CommentsStateRequest request) {
        long actorId = requireActor();
        AccountPolicy.requireAllowedActor(AccountPolicy.SOLUTION_DENY, actorId);
        migrationGate.requireCutover();
        validateId(solutionId, "题解ID有误");
        if (request == null || request.getOpen() == null) {
            throw new ApiStatusException(400, "评论区状态有误");
        }

        SolutionRecord solution = solutionMapper.selectCommentTargetForUpdate(solutionId);
        if (solution == null || Boolean.TRUE.equals(solution.getDelFlag())) {
            throw new ApiStatusException(404, "题解不存在或无权操作");
        }
        boolean owner = Objects.equals(solution.getUserId(), actorId);
        boolean manager = AccountPolicy.hasAuthority(SOLUTION_EDIT);
        if (!owner && !manager) throw new ApiStatusException(404, "题解不存在或无权操作");

        if (Objects.equals(solution.getCommentsOpen(), request.getOpen())) return true;
        if (solutionMapper.setCommentsOpen(solutionId, request.getOpen()) != 1) {
            throw new ApiStatusException(409, "评论区状态已变化，请重试");
        }
        return true;
    }

    private boolean delete(Long commentId, Long operatorId, boolean admin, String reason) {
        validateId(commentId, "评论ID有误");
        // This is an unlocked metadata read used only to discover the parent lock key.
        // The target and its relationship are revalidated after acquiring the locks.
        SolutionComment before = commentMapper.selectReplyTarget(commentId);
        if (before == null || before.getSolutionId() == null || before.getSolutionId() <= 0) {
            throw new ApiStatusException(404, "评论不存在");
        }
        Long solutionId = before.getSolutionId();
        SolutionRecord solution = solutionMapper.selectCommentTargetForUpdate(solutionId);
        if (solution == null) throw new ApiStatusException(404, "评论不存在");

        Long rootId = resolveRootId(before, commentId);
        SolutionComment root = commentMapper.selectRootForUpdate(rootId, solutionId);
        if (!isRoot(root, rootId, solutionId)) throw new ApiStatusException(404, "评论不存在");

        SolutionComment target;
        if (Objects.equals(commentId, rootId)) {
            target = root; // Root is both the root lock and deletion target; lock it only once.
        } else {
            target = commentMapper.selectReplyParentForUpdate(commentId, solutionId);
            if (!isReply(target, commentId, rootId, solutionId)) {
                throw new ApiStatusException(404, "评论不存在");
            }
        }
        if (!Objects.equals(target.getUserId(), before.getUserId())) {
            throw new ApiStatusException(409, "评论状态已变化，请重试");
        }
        if (!admin && !Objects.equals(target.getUserId(), operatorId)) {
            throw new ApiStatusException(404, "评论不存在或无权删除");
        }
        if (target.getState() != CommentState.VISIBLE) {
            // Already deleted comments are idempotent success and never alter counts/audit/outbox.
            return true;
        }

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        String deletedState = admin ? CommentState.ADMIN_DELETED.name() : CommentState.AUTHOR_DELETED.name();
        if (commentMapper.markVisibleDeleted(commentId, solutionId, deletedState, operatorId, now) != 1) {
            throw new ApiStatusException(409, "评论状态已变化，请重试");
        }
        if (solutionMapper.adjustCommentCount(solutionId, -1) != 1) {
            throw new ApiStatusException(409, "评论计数状态异常");
        }
        if (!Objects.equals(commentId, rootId) && commentMapper.decrementRootReplyCount(rootId) != 1) {
            throw new ApiStatusException(409, "回复计数状态异常");
        }
        if (admin) recordAdminDelete(target, operatorId, reason, now);
        return true;
    }

    private void recordAdminDelete(SolutionComment comment, Long operatorId, String reason, LocalDateTime now) {
        SolutionModerationAction action = new SolutionModerationAction()
                .setSolutionId(comment.getSolutionId())
                .setTargetType("COMMENT")
                .setTargetId(comment.getCommentId())
                .setAuthorId(comment.getUserId())
                .setOperatorId(operatorId)
                .setAction("DELETE")
                .setReason(reason)
                .setCreatedAt(now);
        if (actionMapper.insert(action) != 1 || action.getActionId() == null) {
            throw new IllegalStateException("Unable to create comment moderation audit");
        }
        CommunityEvent event = new CommunityEvent()
                .setSchemaVersion(1)
                .setEventId(UUID.randomUUID().toString())
                .setEventType(CommunityEventType.COMMENT_MODERATED)
                .setDedupeKey("comment-moderated:" + action.getActionId())
                .setOccurredAt(OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS).toString())
                .setActorId(String.valueOf(operatorId))
                .setSolutionId(String.valueOf(comment.getSolutionId()))
                .setCommentId(String.valueOf(comment.getCommentId()))
                .setRecipientIds(Collections.singletonList(String.valueOf(comment.getUserId())))
                .setAction("DELETE")
                .setReason(reason);
        outbox.recordCommunity(event);
    }

    private Map<Long, SolutionModerationAction> loadLatestActions(List<Long> targetIds) {
        if (targetIds.isEmpty()) return Collections.emptyMap();
        List<SolutionModerationAction> actions = actionMapper.selectCommentActionsByTargets(targetIds);
        Map<Long, SolutionModerationAction> latest = new LinkedHashMap<>();
        if (actions != null) {
            for (SolutionModerationAction action : actions) {
                if (action != null && action.getTargetId() != null) {
                    latest.putIfAbsent(action.getTargetId(), action);
                }
            }
        }
        return latest;
    }

    private AdminCommentVo toAdminVo(SolutionComment comment, SolutionModerationAction action) {
        return new AdminCommentVo()
                .setCommentId(comment.getCommentId())
                .setSolutionId(comment.getSolutionId())
                .setUserId(comment.getUserId())
                .setRootId(comment.getRootId())
                .setParentId(comment.getParentId())
                .setReplyToUserId(comment.getReplyToUserId())
                .setContent(comment.getContent())
                .setState(comment.getState())
                .setDeletedBy(comment.getDeletedBy())
                .setDeletedAt(comment.getDeletedAt())
                .setLikeCount(comment.getLikeCount() == null ? 0L : comment.getLikeCount())
                .setReplyCount(comment.getReplyCount() == null ? 0L : comment.getReplyCount())
                .setCreatedAt(comment.getCreatedAt())
                .setActionId(action == null ? null : action.getActionId())
                .setOperatorId(action == null ? null : action.getOperatorId())
                .setAction(action == null ? null : action.getAction())
                .setReason(action == null ? null : action.getReason())
                .setActionAt(action == null ? null : action.getCreatedAt());
    }

    private Long resolveRootId(SolutionComment comment, Long commentId) {
        if (comment.getRootId() == null && comment.getParentId() == null) return commentId;
        if (comment.getRootId() == null || comment.getRootId() <= 0
                || comment.getParentId() == null || comment.getParentId() <= 0
                || Objects.equals(comment.getRootId(), commentId)) {
            throw new ApiStatusException(404, "评论不存在");
        }
        return comment.getRootId();
    }

    private boolean isRoot(SolutionComment root, Long rootId, Long solutionId) {
        return root != null && Objects.equals(root.getCommentId(), rootId)
                && Objects.equals(root.getSolutionId(), solutionId)
                && root.getRootId() == null && root.getParentId() == null;
    }

    private boolean isReply(SolutionComment target, Long commentId, Long rootId, Long solutionId) {
        return target != null && Objects.equals(target.getCommentId(), commentId)
                && Objects.equals(target.getSolutionId(), solutionId)
                && Objects.equals(target.getRootId(), rootId) && target.getParentId() != null;
    }

    private long requireActor() {
        Long actorId = AuthUtil.getUserId();
        if (actorId == null || actorId <= 0) throw new ApiStatusException(401, "请先登录");
        return actorId;
    }

    private void validateId(Long id, String message) {
        if (id == null || id <= 0) throw new ApiStatusException(400, message);
    }

    private String normalizeReason(String raw) {
        if (raw == null) throw new ApiStatusException(400, "请填写删除原因");
        String value = Normalizer.normalize(raw.replace("\r\n", "\n"), Normalizer.Form.NFC).strip();
        if (value.isEmpty() || value.codePointCount(0, value.length()) > 500) {
            throw new ApiStatusException(400, "删除原因须为1到500个字符");
        }
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isHighSurrogate(current)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) {
                    throw new ApiStatusException(400, "删除原因包含非法字符");
                }
            } else if (Character.isLowSurrogate(current)
                    || (Character.isISOControl(current) && current != '\t' && current != '\n')) {
                throw new ApiStatusException(400, "删除原因包含非法字符");
            }
        }
        return value;
    }
}
