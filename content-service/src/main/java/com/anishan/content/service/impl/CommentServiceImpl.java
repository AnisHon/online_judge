package com.anishan.content.service.impl;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.api.client.user.domain.dto.UserSummaryRequest;
import com.anishan.api.client.user.domain.vo.UserSummaryVo;
import com.anishan.api.util.AccountPolicy;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.dto.CommentCreateRequest;
import com.anishan.content.domain.dto.CommentPageQuery;
import com.anishan.content.domain.dto.CommentReplyRequest;
import com.anishan.content.domain.entity.SolutionComment;
import com.anishan.content.domain.enumeration.CommentState;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.CommentVo;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.CommentLikeMapper;
import com.anishan.content.mapper.SolutionCommentMapper;
import com.anishan.content.service.CommentContentValidator;
import com.anishan.content.service.CommentCreationService;
import com.anishan.content.service.CommentRateLimiter;
import com.anishan.content.service.CommentService;
import com.anishan.content.service.SolutionAccessService;
import com.anishan.content.service.SolutionDomainMigrationGate;
import com.anishan.content.service.SolutionProblemReferenceService;
import com.anishan.content.service.SolutionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private static final long DEFAULT_PAGE_SIZE = 20L;
    private static final long MAX_PAGE_SIZE = 50L;
    private static final int MAX_QUALIFICATION_RETRIES = 2;

    private final SolutionCommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final SolutionQueryService solutionQueryService;
    private final SolutionProblemReferenceService problemReferenceService;
    private final SolutionAccessService solutionAccessService;
    private final UserInternalClient userInternalClient;
    private final CommentCreationService creationService;
    private final CommentContentValidator contentValidator;
    private final CommentRateLimiter rateLimiter;
    private final SolutionDomainMigrationGate migrationGate;

    @Override
    public PagedResult<CommentVo> pageRoots(Long solutionId, CommentPageQuery query) {
        long viewerId = requireActor();
        migrationGate.requireCutover();
        validateSolutionId(solutionId);
        long currentPage = query == null || query.getCurrentPage() == null
                ? 1L : query.getCurrentPage();
        long pageSize = query == null || query.getPageSize() == null
                ? DEFAULT_PAGE_SIZE : query.getPageSize();
        long offset = pageOffset(currentPage, pageSize);

        requirePublicSolution(solutionId);
        long total = commentMapper.countRootComments(solutionId);
        List<SolutionComment> comments = commentMapper.selectRootComments(solutionId, offset, pageSize);
        PagedResult<CommentVo> result = new PagedResult<>();
        result.setCurrentPage(currentPage);
        result.setPageSize(pageSize);
        result.setTotalRecords(total);
        result.setData(toVos(comments, viewerId));
        return result;
    }

    @Override
    public PagedResult<CommentVo> pageReplies(Long rootId, CommentPageQuery query) {
        long viewerId = requireActor();
        migrationGate.requireCutover();
        validateCommentId(rootId);
        long currentPage = query == null || query.getCurrentPage() == null
                ? 1L : query.getCurrentPage();
        long pageSize = query == null || query.getPageSize() == null
                ? DEFAULT_PAGE_SIZE : query.getPageSize();
        long offset = pageOffset(currentPage, pageSize);

        SolutionComment root = commentMapper.selectReplyTarget(rootId);
        requireRoot(root, rootId, null);
        Long solutionId = root.getSolutionId();
        requirePublicSolution(solutionId);
        // Recheck local ownership/shape after the cross-service qualification call.
        root = commentMapper.selectReplyTarget(rootId);
        requireRoot(root, rootId, solutionId);

        long total = commentMapper.countRootReplies(rootId);
        List<SolutionComment> replies = commentMapper.selectRootReplies(rootId, offset, pageSize);
        PagedResult<CommentVo> result = new PagedResult<>();
        result.setCurrentPage(currentPage);
        result.setPageSize(pageSize);
        result.setTotalRecords(total);
        result.setData(toVos(replies, viewerId));
        return result;
    }

    @Override
    public CommentVo createRoot(Long solutionId, CommentCreateRequest request) {
        long actorId = requireActor();
        AccountPolicy.requireAllowedActor(AccountPolicy.COMMENT_DENY, actorId);
        migrationGate.requireCutover();
        validateSolutionId(solutionId);
        CommentCreateRequest normalized = contentValidator.normalize(request);

        SolutionComment existing = commentMapper.selectByUserAndClientRequestId(
                actorId, normalized.getClientRequestId());
        QualifiedSolution qualified = requirePublicSolution(solutionId);
        if (existing != null) {
            if (!sameRequest(existing, actorId, solutionId, normalized.getContent())) {
                throw new ApiStatusException(409, "该请求标识已用于其他评论");
            }
            // Idempotent success is checked before comments_open, but never bypasses current visibility.
            return toVos(Collections.singletonList(existing), actorId).get(0);
        }
        if (!Boolean.TRUE.equals(qualified.solution.getCommentsOpen())) {
            throw new ApiStatusException(409, "该题解已关闭评论");
        }

        rateLimiter.acquire(actorId);
        try {
            SolutionComment created = creationService.createRoot(actorId, solutionId,
                    qualified.solution.getVersion(), qualified.solution.getProblemId(), qualified.problem,
                    normalized.getContent(), normalized.getClientRequestId());
            return toVos(Collections.singletonList(created), actorId).get(0);
        } catch (DuplicateKeyException duplicate) {
            // The @Transactional writer has rolled back before control returns here.
            SolutionComment concurrent = commentMapper.selectByUserAndClientRequestId(
                    actorId, normalized.getClientRequestId());
            if (concurrent == null) throw new ApiStatusException(409, "评论状态已变化，请重试");
            // Recheck the current target before returning any record to a retrying caller.
            requirePublicSolution(solutionId);
            if (!sameRequest(concurrent, actorId, solutionId, normalized.getContent())) {
                throw new ApiStatusException(409, "该请求标识已用于其他评论");
            }
            return toVos(Collections.singletonList(concurrent), actorId).get(0);
        }
    }

    @Override
    public CommentVo createReply(Long parentId, CommentReplyRequest request) {
        long actorId = requireActor();
        AccountPolicy.requireAllowedActor(AccountPolicy.COMMENT_DENY, actorId);
        migrationGate.requireCutover();
        validateCommentId(parentId);
        CommentCreateRequest normalized = contentValidator.normalize(request == null ? null
                : new CommentCreateRequest().setContent(request.getContent())
                        .setClientRequestId(request.getClientRequestId()));

        SolutionComment existing = commentMapper.selectByUserAndClientRequestId(
                actorId, normalized.getClientRequestId());
        if (existing != null) {
            if (!sameReplyRequest(existing, actorId, parentId, normalized.getContent())) {
                throw new ApiStatusException(409, "该请求标识已用于其他评论");
            }
            requirePublicSolution(existing.getSolutionId());
            return toVos(Collections.singletonList(existing), actorId).get(0);
        }

        SolutionComment parent = commentMapper.selectReplyTarget(parentId);
        if (parent == null || parent.getSolutionId() == null || parent.getSolutionId() <= 0
                || parent.getState() != CommentState.VISIBLE) {
            throw new ApiStatusException(404, "评论不存在或不可回复");
        }
        Long rootId = resolveRootId(parent, parentId);
        SolutionComment root = Objects.equals(rootId, parentId)
                ? parent : commentMapper.selectReplyTarget(rootId);
        requireRoot(root, rootId, parent.getSolutionId());

        QualifiedSolution qualified = requirePublicSolution(parent.getSolutionId());
        if (!Boolean.TRUE.equals(qualified.solution.getCommentsOpen())) {
            throw new ApiStatusException(409, "该题解已关闭评论");
        }
        rateLimiter.acquire(actorId);
        try {
            SolutionComment created = creationService.createReply(actorId, parentId, rootId,
                    parent.getSolutionId(), qualified.solution.getVersion(),
                    qualified.solution.getProblemId(), qualified.problem,
                    normalized.getContent(), normalized.getClientRequestId());
            return toVos(Collections.singletonList(created), actorId).get(0);
        } catch (DuplicateKeyException duplicate) {
            // The writer transaction has rolled back before the idempotent lookup.
            SolutionComment concurrent = commentMapper.selectByUserAndClientRequestId(
                    actorId, normalized.getClientRequestId());
            if (concurrent == null) throw new ApiStatusException(409, "评论状态已变化，请重试");
            if (!sameReplyRequest(concurrent, actorId, parentId, normalized.getContent())) {
                throw new ApiStatusException(409, "该请求标识已用于其他评论");
            }
            requirePublicSolution(concurrent.getSolutionId());
            return toVos(Collections.singletonList(concurrent), actorId).get(0);
        }
    }

    private QualifiedSolution requirePublicSolution(Long solutionId) {
        for (int attempt = 0; attempt < MAX_QUALIFICATION_RETRIES; attempt++) {
            SolutionRecord before = solutionQueryService.commentSnapshot(solutionId);
            if (before == null || Boolean.TRUE.equals(before.getDelFlag())) {
                throw new ApiStatusException(404, "题解不存在或无权访问");
            }
            if (before.getProblemId() == null || before.getProblemId() <= 0) {
                throw new ApiStatusException(503, "题目状态暂时无法确认，请稍后重试");
            }
            Map<Long, ContentProblemReadVo> fresh = problemReferenceService.refresh(
                    Collections.singletonList(before.getProblemId()));
            SolutionRecord current = solutionQueryService.commentSnapshot(solutionId);
            if (!sameSnapshot(before, current)) continue;
            ContentProblemReadVo problem = fresh.get(current.getProblemId());
            boolean publicSolution = !Boolean.TRUE.equals(current.getPrivate_())
                    && current.getModerationState() == SolutionModerationState.NORMAL
                    && solutionAccessService.isPubliclyReadable(problem);
            if (!publicSolution) throw new ApiStatusException(404, "题解不存在或无权访问");
            return new QualifiedSolution(current, problem);
        }
        throw new ApiStatusException(503, "题解状态正在变化，请稍后重试");
    }

    private List<CommentVo> toVos(List<SolutionComment> comments, Long viewerId) {
        if (comments == null || comments.isEmpty()) return Collections.emptyList();
        Set<Long> userIds = new LinkedHashSet<>();
        List<Long> commentIds = new ArrayList<>();
        for (SolutionComment comment : comments) {
            if (comment.getUserId() != null && comment.getUserId() > 0) userIds.add(comment.getUserId());
            if (comment.getReplyToUserId() != null && comment.getReplyToUserId() > 0) userIds.add(comment.getReplyToUserId());
            if (comment.getCommentId() != null) commentIds.add(comment.getCommentId());
        }
        Map<Long, UserSummaryVo> summaries = loadSummaries(userIds);
        List<Long> activeCommentIds = commentIds.isEmpty() ? Collections.emptyList()
                : commentLikeMapper.selectActiveCommentIdsForUser(viewerId, commentIds);
        Set<Long> likedIds = activeCommentIds == null
                ? Collections.emptySet() : new LinkedHashSet<>(activeCommentIds);
        List<CommentVo> result = new ArrayList<>(comments.size());
        for (SolutionComment comment : comments) {
            boolean visible = comment.getState() == CommentState.VISIBLE;
            result.add(new CommentVo()
                    .setCommentId(comment.getCommentId())
                    .setSolutionId(comment.getSolutionId())
                    .setRootId(comment.getRootId())
                    .setParentId(comment.getParentId())
                    .setAuthor(summaries.get(comment.getUserId()))
                    .setReplyTo(comment.getReplyToUserId() == null ? null : summaries.get(comment.getReplyToUserId()))
                    .setContent(visible ? comment.getContent() : null)
                    .setState(comment.getState())
                    .setLikeCount(defaultCount(comment.getLikeCount()))
                    .setLikedByMe(likedIds.contains(comment.getCommentId()))
                    .setReplyCount(defaultCount(comment.getReplyCount()))
                    .setCreatedAt(comment.getCreatedAt())
                    .setCanDelete(visible && Objects.equals(viewerId, comment.getUserId())));
        }
        return result;
    }

    private Map<Long, UserSummaryVo> loadSummaries(Collection<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();
        UserSummaryRequest request = new UserSummaryRequest();
        request.setUserIds(ids.stream().map(String::valueOf).collect(Collectors.toList()));
        final R<List<UserSummaryVo>> response;
        try {
            response = userInternalClient.userSummaries(request);
        } catch (RuntimeException exception) {
            throw new ApiStatusException(503, "用户信息暂时无法加载，请稍后重试");
        }
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            throw new ApiStatusException(503, "用户信息暂时无法加载，请稍后重试");
        }
        Map<Long, UserSummaryVo> result = new LinkedHashMap<>();
        for (UserSummaryVo summary : response.getData()) {
            if (summary != null && summary.getUserId() != null && ids.contains(summary.getUserId())) {
                result.put(summary.getUserId(), summary);
            }
        }
        return result;
    }

    private boolean sameRequest(SolutionComment existing, Long actorId, Long solutionId, String content) {
        return Objects.equals(existing.getUserId(), actorId)
                && Objects.equals(existing.getSolutionId(), solutionId)
                && existing.getRootId() == null
                && existing.getParentId() == null
                && Objects.equals(existing.getContent(), content);
    }

    private boolean sameReplyRequest(SolutionComment existing, Long actorId, Long parentId, String content) {
        return Objects.equals(existing.getUserId(), actorId)
                && existing.getRootId() != null
                && Objects.equals(existing.getParentId(), parentId)
                && Objects.equals(existing.getContent(), content);
    }

    private Long resolveRootId(SolutionComment parent, Long parentId) {
        if (parent.getRootId() == null && parent.getParentId() == null) return parentId;
        if (parent.getRootId() == null || parent.getRootId() <= 0
                || Objects.equals(parent.getRootId(), parentId)
                || parent.getParentId() == null || parent.getParentId() <= 0) {
            throw new ApiStatusException(404, "评论不存在或不可回复");
        }
        return parent.getRootId();
    }

    private void requireRoot(SolutionComment root, Long expectedRootId, Long expectedSolutionId) {
        if (root == null || !Objects.equals(root.getCommentId(), expectedRootId)
                || root.getRootId() != null || root.getParentId() != null
                || (expectedSolutionId != null && !Objects.equals(root.getSolutionId(), expectedSolutionId))) {
            throw new ApiStatusException(404, "评论不存在");
        }
    }

    private boolean sameSnapshot(SolutionRecord before, SolutionRecord current) {
        return current != null && !Boolean.TRUE.equals(current.getDelFlag())
                && Objects.equals(before.getProblemId(), current.getProblemId())
                && Objects.equals(before.getVersion(), current.getVersion());
    }

    private long requireActor() {
        Long actorId = AuthUtil.getUserId();
        if (actorId == null || actorId <= 0) throw new ApiStatusException(401, "请先登录");
        return actorId;
    }

    private long pageOffset(long currentPage, long pageSize) {
        if (currentPage < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ApiStatusException(400, "分页参数有误");
        }
        try {
            return Math.multiplyExact(currentPage - 1, pageSize);
        } catch (ArithmeticException exception) {
            throw new ApiStatusException(400, "分页参数有误");
        }
    }

    private void validateSolutionId(Long solutionId) {
        if (solutionId == null || solutionId <= 0) throw new ApiStatusException(400, "题解ID有误");
    }

    private void validateCommentId(Long commentId) {
        if (commentId == null || commentId <= 0) throw new ApiStatusException(400, "评论ID有误");
    }

    private long defaultCount(Long count) {
        return count == null ? 0L : count;
    }

    private static final class QualifiedSolution {
        private final SolutionRecord solution;
        private final ContentProblemReadVo problem;

        private QualifiedSolution(SolutionRecord solution, ContentProblemReadVo problem) {
            this.solution = solution;
            this.problem = problem;
        }
    }
}
