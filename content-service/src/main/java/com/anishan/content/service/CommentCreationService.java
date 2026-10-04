package com.anishan.content.service;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.entity.SolutionComment;
import com.anishan.content.domain.enumeration.CommentState;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.SolutionCommentMapper;
import com.anishan.content.mapper.SolutionExplanationMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Objects;
import java.util.UUID;

/** Database-only comment transaction. Qualification is fetched before entering this bean. */
@Service
public class CommentCreationService {
    private final SolutionExplanationMapper solutionMapper;
    private final SolutionCommentMapper commentMapper;
    private final SolutionAccessService accessService;
    private final ContentEventOutboxService outbox;
    private final Clock clock;

    public CommentCreationService(SolutionExplanationMapper solutionMapper,
                                  SolutionCommentMapper commentMapper,
                                  SolutionAccessService accessService,
                                  ContentEventOutboxService outbox,
                                  @Qualifier("contentCommunityOutboxClock") Clock clock) {
        this.solutionMapper = solutionMapper;
        this.commentMapper = commentMapper;
        this.accessService = accessService;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public SolutionComment createRoot(Long actorId, Long solutionId, Long expectedVersion,
                                      Long expectedProblemId, ContentProblemReadVo freshProblem,
                                      String normalizedContent, String clientRequestId) {
        if (actorId == null || actorId <= 0) throw new ApiStatusException(401, "请先登录");
        SolutionRecord solution = solutionMapper.selectCommentTargetForUpdate(solutionId);
        if (solution == null || Boolean.TRUE.equals(solution.getDelFlag())) {
            throw new ApiStatusException(404, "题解不存在或无权访问");
        }
        if (!Objects.equals(expectedProblemId, solution.getProblemId())
                || !Objects.equals(expectedVersion, solution.getVersion())) {
            throw new ApiStatusException(409, "题解已变化，请刷新后重试");
        }
        if (!isPublic(solution, freshProblem)) {
            throw new ApiStatusException(404, "题解不存在或无权访问");
        }
        if (!Boolean.TRUE.equals(solution.getCommentsOpen())) {
            throw new ApiStatusException(409, "该题解已关闭评论");
        }

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        SolutionComment comment = new SolutionComment()
                .setSolutionId(solutionId)
                .setUserId(actorId)
                .setRootId(null)
                .setParentId(null)
                .setReplyToUserId(null)
                .setContent(normalizedContent)
                .setState(CommentState.VISIBLE)
                .setLikeCount(0L)
                .setReplyCount(0L)
                .setClientRequestId(clientRequestId)
                .setCreatedAt(now)
                .setUpdatedAt(now);
        if (commentMapper.insert(comment) != 1 || comment.getCommentId() == null) {
            throw new ApiStatusException(409, "评论创建失败，请重试");
        }
        if (solutionMapper.adjustCommentCount(solutionId, 1) != 1) {
            throw new ApiStatusException(409, "评论计数状态异常");
        }

        CommunityEvent event = new CommunityEvent()
                .setSchemaVersion(1)
                .setEventId(UUID.randomUUID().toString())
                .setEventType(CommunityEventType.SOLUTION_COMMENTED)
                .setDedupeKey("solution-commented:" + comment.getCommentId())
                .setOccurredAt(OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS).toString())
                .setActorId(String.valueOf(actorId))
                .setSolutionId(String.valueOf(solutionId))
                .setCommentId(String.valueOf(comment.getCommentId()))
                .setRecipientIds(actorId.equals(solution.getUserId())
                        ? Collections.emptyList()
                        : Collections.singletonList(String.valueOf(solution.getUserId())));
        outbox.recordCommunity(event);
        return comment;
    }

    @Transactional(rollbackFor = Exception.class)
    public SolutionComment createReply(Long actorId, Long parentId, Long expectedRootId,
                                       Long expectedSolutionId, Long expectedVersion,
                                       Long expectedProblemId, ContentProblemReadVo freshProblem,
                                       String normalizedContent, String clientRequestId) {
        if (actorId == null || actorId <= 0) throw new ApiStatusException(401, "请先登录");
        SolutionRecord solution = solutionMapper.selectCommentTargetForUpdate(expectedSolutionId);
        if (solution == null || Boolean.TRUE.equals(solution.getDelFlag())) {
            throw new ApiStatusException(404, "题解不存在或无权访问");
        }
        if (!Objects.equals(expectedProblemId, solution.getProblemId())
                || !Objects.equals(expectedVersion, solution.getVersion())) {
            throw new ApiStatusException(409, "题解已变化，请刷新后重试");
        }
        if (!isPublic(solution, freshProblem)) {
            throw new ApiStatusException(404, "题解不存在或无权访问");
        }
        if (!Boolean.TRUE.equals(solution.getCommentsOpen())) {
            throw new ApiStatusException(409, "该题解已关闭评论");
        }

        // Global lock order: solution -> root -> parent. A direct root reply uses
        // the root row as its parent and therefore locks that row only once.
        SolutionComment root = commentMapper.selectRootForUpdate(expectedRootId, expectedSolutionId);
        if (root == null || !Objects.equals(root.getCommentId(), expectedRootId)
                || root.getRootId() != null || root.getParentId() != null) {
            throw new ApiStatusException(404, "评论不存在");
        }
        SolutionComment parent;
        if (Objects.equals(parentId, expectedRootId)) {
            parent = root;
        } else {
            parent = commentMapper.selectReplyParentForUpdate(parentId, expectedSolutionId);
            if (parent == null || !Objects.equals(parent.getRootId(), expectedRootId)
                    || parent.getParentId() == null || parent.getParentId() <= 0) {
                throw new ApiStatusException(404, "评论不存在或不可回复");
            }
        }
        if (parent.getState() != CommentState.VISIBLE
                || parent.getUserId() == null || parent.getUserId() <= 0) {
            throw new ApiStatusException(404, "评论不存在或不可回复");
        }

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        SolutionComment reply = new SolutionComment()
                .setSolutionId(expectedSolutionId)
                .setUserId(actorId)
                .setRootId(expectedRootId)
                .setParentId(parentId)
                .setReplyToUserId(parent.getUserId())
                .setContent(normalizedContent)
                .setState(CommentState.VISIBLE)
                .setLikeCount(0L)
                .setReplyCount(0L)
                .setClientRequestId(clientRequestId)
                .setCreatedAt(now)
                .setUpdatedAt(now);
        if (commentMapper.insert(reply) != 1 || reply.getCommentId() == null) {
            throw new ApiStatusException(409, "回复创建失败，请重试");
        }
        if (commentMapper.incrementRootReplyCount(expectedRootId) != 1) {
            throw new ApiStatusException(409, "回复计数状态异常");
        }
        if (solutionMapper.adjustCommentCount(expectedSolutionId, 1) != 1) {
            throw new ApiStatusException(409, "评论计数状态异常");
        }

        Set<Long> recipientIds = new LinkedHashSet<>();
        addRecipient(recipientIds, parent.getUserId(), actorId);
        addRecipient(recipientIds, solution.getUserId(), actorId);
        CommunityEvent event = new CommunityEvent()
                .setSchemaVersion(1)
                .setEventId(UUID.randomUUID().toString())
                .setEventType(CommunityEventType.COMMENT_REPLIED)
                .setDedupeKey("comment-replied:" + reply.getCommentId())
                .setOccurredAt(OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS).toString())
                .setActorId(String.valueOf(actorId))
                .setSolutionId(String.valueOf(expectedSolutionId))
                .setCommentId(String.valueOf(reply.getCommentId()))
                .setRecipientIds(recipientIds.stream().map(String::valueOf)
                        .collect(java.util.stream.Collectors.toList()));
        outbox.recordCommunity(event);
        return reply;
    }

    private void addRecipient(Set<Long> recipientIds, Long candidate, Long actorId) {
        if (candidate != null && candidate > 0 && !Objects.equals(candidate, actorId)) {
            recipientIds.add(candidate);
        }
    }

    private boolean isPublic(SolutionRecord solution, ContentProblemReadVo freshProblem) {
        return !Boolean.TRUE.equals(solution.getDelFlag())
                && !Boolean.TRUE.equals(solution.getPrivate_())
                && solution.getModerationState() == SolutionModerationState.NORMAL
                && accessService.isPubliclyReadable(freshProblem);
    }
}
