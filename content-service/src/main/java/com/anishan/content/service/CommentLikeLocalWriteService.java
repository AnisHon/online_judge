package com.anishan.content.service;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.entity.CommentLike;
import com.anishan.content.domain.entity.SolutionComment;
import com.anishan.content.domain.enumeration.CommentState;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.LikeResultVo;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.CommentLikeMapper;
import com.anishan.content.mapper.SolutionCommentMapper;
import com.anishan.content.mapper.SolutionExplanationMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/** Owns the atomic comment-like, counter, first-notice, and outbox transition. */
@Service
public class CommentLikeLocalWriteService {
    private final SolutionExplanationMapper solutionMapper;
    private final SolutionCommentMapper commentMapper;
    private final CommentLikeMapper likeMapper;
    private final SolutionAccessService accessService;
    private final ContentEventOutboxService outbox;
    private final Clock clock;

    public CommentLikeLocalWriteService(SolutionExplanationMapper solutionMapper,
                                        SolutionCommentMapper commentMapper,
                                        CommentLikeMapper likeMapper,
                                        SolutionAccessService accessService,
                                        ContentEventOutboxService outbox,
                                        @Qualifier("contentCommunityOutboxClock") Clock clock) {
        this.solutionMapper = solutionMapper;
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.accessService = accessService;
        this.outbox = outbox;
        this.clock = clock;
    }

    /**
     * Call only after all remote qualification has completed. Locks follow the fixed order:
     * solution, root, target comment, then the viewer's like relation.
     */
    @Transactional(rollbackFor = Exception.class)
    public LikeResultVo setState(Long userId, Long solutionId, Long rootId, Long commentId,
                                 boolean requestedLike, ContentProblemReadVo freshProblem) {
        if (userId == null || userId <= 0) throw new ApiStatusException(401, "请先登录");
        requireResourceId(solutionId, "评论不存在");
        requireResourceId(rootId, "评论不存在");
        requireResourceId(commentId, "评论ID有误");

        SolutionRecord solution = solutionMapper.selectCommentTargetForUpdate(solutionId);
        if (solution == null) {
            if (requestedLike) throw notFound();
            return result(false, null);
        }

        SolutionComment root = commentMapper.selectRootForUpdate(rootId, solutionId);
        if (!isRoot(root, rootId, solutionId)) {
            if (requestedLike) throw notFound();
            return result(false, null);
        }

        SolutionComment target = Objects.equals(rootId, commentId)
                ? root : commentMapper.selectTargetForUpdate(commentId, solutionId);
        if (!isTarget(target, commentId, rootId, solutionId)) {
            if (requestedLike) throw notFound();
            return result(false, null);
        }

        boolean publicSolution = isPublic(solution, freshProblem);
        boolean visibleTarget = target.getState() == CommentState.VISIBLE;
        if (requestedLike && (!publicSolution || !visibleTarget)) throw notFound();

        CommentLike relation = likeMapper.selectByCommentAndUserForUpdate(commentId, userId);
        boolean previouslyLiked = relation != null && Boolean.TRUE.equals(relation.getActive());
        if (previouslyLiked == requestedLike) {
            if (relation == null) return result(false, null);
            return result(previouslyLiked, publicSolution && visibleTarget
                    ? commentMapper.selectLikeCount(commentId) : null);
        }

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        if (requestedLike) {
            boolean transitioned = false;
            if (relation == null) {
                CommentLike inserted = new CommentLike().setCommentId(commentId).setUserId(userId)
                        .setActive(true)
                        // Self-likes are consumed without an author notification, even if ownership changes later.
                        .setFirstNotified(userId.equals(target.getUserId()))
                        .setCreatedAt(now).setUpdatedAt(now);
                try {
                    if (likeMapper.insertState(inserted) != 1) {
                        throw new ApiStatusException(409, "点赞状态已变化，请重试");
                    }
                    relation = inserted;
                    transitioned = true;
                } catch (DuplicateKeyException duplicate) {
                    // Parent locks serialize normal writers; the unique key remains the final race guard.
                    relation = likeMapper.selectByCommentAndUserForUpdate(commentId, userId);
                    if (relation == null) throw new ApiStatusException(409, "点赞状态已变化，请重试");
                    if (!Boolean.TRUE.equals(relation.getActive())) {
                        if (likeMapper.updateActiveState(commentId, userId, true, now) != 1) {
                            throw new ApiStatusException(409, "点赞状态已变化，请重试");
                        }
                        transitioned = true;
                    }
                }
            } else {
                if (likeMapper.updateActiveState(commentId, userId, true, now) != 1) {
                    throw new ApiStatusException(409, "点赞状态已变化，请重试");
                }
                transitioned = true;
            }

            if (transitioned) adjustCount(commentId, 1, now);
            notifyFirstLike(solutionId, target, relation, userId, now);
            return result(true, commentMapper.selectLikeCount(commentId));
        }

        // Removing one's own old relation remains permitted after moderation/deletion/private changes.
        if (likeMapper.updateActiveState(commentId, userId, false, now) != 1) {
            throw new ApiStatusException(409, "点赞状态已变化，请重试");
        }
        adjustCount(commentId, -1, now);
        return result(false, publicSolution && visibleTarget ? commentMapper.selectLikeCount(commentId) : null);
    }

    private void notifyFirstLike(Long solutionId, SolutionComment target, CommentLike relation,
                                 Long actorId, LocalDateTime now) {
        if (Boolean.TRUE.equals(relation.getFirstNotified())) return;
        if (!actorId.equals(target.getUserId())) {
            CommunityEvent event = new CommunityEvent().setSchemaVersion(1)
                    .setEventId(UUID.randomUUID().toString())
                    .setEventType(CommunityEventType.COMMENT_LIKED)
                    .setDedupeKey("comment-liked:" + target.getCommentId() + ":" + actorId)
                    .setOccurredAt(OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS).toString())
                    .setActorId(String.valueOf(actorId))
                    .setSolutionId(String.valueOf(solutionId))
                    .setCommentId(String.valueOf(target.getCommentId()))
                    .setRecipientIds(Collections.singletonList(String.valueOf(target.getUserId())));
            outbox.recordCommunity(event);
        }
        if (likeMapper.markFirstNotified(target.getCommentId(), actorId, now) != 1) {
            throw new ApiStatusException(409, "点赞状态已变化，请重试");
        }
        relation.setFirstNotified(true);
    }

    private void adjustCount(Long commentId, int delta, LocalDateTime now) {
        if (commentMapper.adjustLikeCount(commentId, delta, now) != 1) {
            throw new ApiStatusException(409, "点赞计数状态异常");
        }
    }

    private boolean isPublic(SolutionRecord solution, ContentProblemReadVo problem) {
        return !Boolean.TRUE.equals(solution.getDelFlag())
                && !Boolean.TRUE.equals(solution.getPrivate_())
                && solution.getModerationState() == SolutionModerationState.NORMAL
                && accessService.isPubliclyReadable(problem);
    }

    private boolean isRoot(SolutionComment root, Long rootId, Long solutionId) {
        return root != null && Objects.equals(root.getCommentId(), rootId)
                && Objects.equals(root.getSolutionId(), solutionId)
                && root.getRootId() == null && root.getParentId() == null;
    }

    private boolean isTarget(SolutionComment target, Long commentId, Long rootId, Long solutionId) {
        if (target == null || !Objects.equals(target.getCommentId(), commentId)
                || !Objects.equals(target.getSolutionId(), solutionId)) return false;
        if (Objects.equals(commentId, rootId)) return target.getRootId() == null && target.getParentId() == null;
        return Objects.equals(target.getRootId(), rootId) && target.getParentId() != null
                && target.getParentId() > 0;
    }

    private void requireResourceId(Long id, String message) {
        if (id == null || id <= 0) throw new ApiStatusException(400, message);
    }

    private LikeResultVo result(boolean liked, Long count) {
        return new LikeResultVo().setLiked(liked).setLikeCount(count);
    }

    private ApiStatusException notFound() {
        return new ApiStatusException(404, "评论不存在或无权访问");
    }
}
