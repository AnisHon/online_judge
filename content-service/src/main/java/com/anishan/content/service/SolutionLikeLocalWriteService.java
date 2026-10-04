package com.anishan.content.service;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.entity.SolutionLike;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.LikeResultVo;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.SolutionExplanationMapper;
import com.anishan.content.mapper.SolutionLikeMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;

/**
 * All local state changes for a solution-like transition share one content DB transaction.
 * The solution row is locked before the relation row, serializing first transitions per solution.
 */
@Service
public class SolutionLikeLocalWriteService {
    private final SolutionExplanationMapper solutionMapper;
    private final SolutionLikeMapper likeMapper;
    private final SolutionAccessService accessService;
    private final ContentEventOutboxService outbox;
    private final Clock clock;

    public SolutionLikeLocalWriteService(SolutionExplanationMapper solutionMapper,
                                         SolutionLikeMapper likeMapper,
                                         SolutionAccessService accessService,
                                         ContentEventOutboxService outbox,
                                         @Qualifier("contentCommunityOutboxClock") Clock clock) {
        this.solutionMapper = solutionMapper;
        this.likeMapper = likeMapper;
        this.accessService = accessService;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public LikeResultVo setState(Long userId, Long solutionId, boolean requestedLike,
                                 ContentProblemReadVo freshProblem) {
        requireActor(userId);
        if (solutionId == null || solutionId <= 0) {
            throw new ApiStatusException(400, "题解ID有误");
        }

        // This is the first lock in the documented order. The mapper selects only the
        // solution row, not its potentially large Markdown body.
        SolutionRecord solution = solutionMapper.selectLikeTargetForUpdate(solutionId);
        if (solution == null) {
            if (requestedLike) throw new ApiStatusException(404, "题解不存在或无权访问");
            return result(false, null);
        }
        boolean publicNow = isPublic(solution, freshProblem);
        if (requestedLike && !publicNow) {
            throw new ApiStatusException(404, "题解不存在或无权访问");
        }

        // The relation is always read/locked after its parent solution row.
        SolutionLike relation = likeMapper.selectBySolutionAndUserForUpdate(solutionId, userId);
        boolean previousLike = relation != null && Boolean.TRUE.equals(relation.getActive());
        if (previousLike == requestedLike) {
            if (relation == null) return result(false, null);
            return result(previousLike, publicNow ? solutionMapper.selectLikeCount(solutionId) : null);
        }

        LocalDateTime now = now();
        if (requestedLike) {
            if (relation == null) {
                SolutionLike inserted = new SolutionLike().setSolutionId(solutionId).setUserId(userId)
                        .setActive(true)
                        // A self-like is intentionally marked as consumed: changing the author later
                        // must not turn that old relationship into a notification.
                        .setFirstNotified(userId.equals(solution.getUserId()))
                        .setCreatedAt(now).setUpdatedAt(now);
                try {
                    if (likeMapper.insertState(inserted) != 1) {
                        throw new ApiStatusException(409, "点赞状态已变化，请重试");
                    }
                    relation = inserted;
                } catch (DuplicateKeyException duplicate) {
                    // The parent lock prevents races among compliant writers. Keep the unique key
                    // as a final guard if another writer bypasses that lock protocol.
                    relation = likeMapper.selectBySolutionAndUserForUpdate(solutionId, userId);
                    if (relation == null) throw new ApiStatusException(409, "点赞状态已变化，请重试");
                    if (Boolean.TRUE.equals(relation.getActive())) {
                        return result(true, solutionMapper.selectLikeCount(solutionId));
                    }
                    if (likeMapper.updateActiveState(solutionId, userId, true, now) != 1) {
                        throw new ApiStatusException(409, "点赞状态已变化，请重试");
                    }
                }
            } else if (likeMapper.updateActiveState(solutionId, userId, true, now) != 1) {
                throw new ApiStatusException(409, "点赞状态已变化，请重试");
            }
            adjustCount(solutionId, 1);
            notifyFirstLike(solution, relation, userId, now);
            return result(true, solutionMapper.selectLikeCount(solutionId));
        }

        // Removing an existing relation is allowed even if the solution/problem is now hidden.
        if (likeMapper.updateActiveState(solutionId, userId, false, now) != 1) {
            throw new ApiStatusException(409, "点赞状态已变化，请重试");
        }
        adjustCount(solutionId, -1);
        return result(false, publicNow ? solutionMapper.selectLikeCount(solutionId) : null);
    }

    private void notifyFirstLike(SolutionRecord solution, SolutionLike relation, Long actorId, LocalDateTime now) {
        if (Boolean.TRUE.equals(relation.getFirstNotified())) return;
        if (!actorId.equals(solution.getUserId())) {
            CommunityEvent event = new CommunityEvent().setSchemaVersion(1)
                    .setEventId(UUID.randomUUID().toString())
                    .setEventType(CommunityEventType.SOLUTION_LIKED)
                    .setDedupeKey("solution-liked:" + solution.getSolutionId() + ":" + actorId)
                    .setOccurredAt(OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS).toString())
                    .setActorId(String.valueOf(actorId))
                    .setSolutionId(String.valueOf(solution.getSolutionId()))
                    .setRecipientIds(Collections.singletonList(String.valueOf(solution.getUserId())));
            outbox.recordCommunity(event);
        }
        if (likeMapper.markFirstNotified(solution.getSolutionId(), actorId, now) != 1) {
            throw new ApiStatusException(409, "点赞状态已变化，请重试");
        }
        relation.setFirstNotified(true);
    }

    private void adjustCount(Long solutionId, int delta) {
        if (solutionMapper.adjustLikeCount(solutionId, delta) != 1) {
            throw new ApiStatusException(409, "点赞计数状态异常");
        }
    }

    private boolean isPublic(SolutionRecord solution, ContentProblemReadVo problem) {
        return !Boolean.TRUE.equals(solution.getDelFlag())
                && !Boolean.TRUE.equals(solution.getPrivate_())
                && solution.getModerationState() == SolutionModerationState.NORMAL
                && accessService.isPubliclyReadable(problem);
    }

    private LikeResultVo result(boolean liked, Long count) {
        return new LikeResultVo().setLiked(liked).setLikeCount(count);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }

    private void requireActor(Long userId) {
        if (userId == null || userId <= 0) throw new ApiStatusException(401, "请先登录");
    }
}
