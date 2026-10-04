package com.anishan.content.service;

import com.anishan.content.domain.dto.DetailSolutionDto;
import com.anishan.content.domain.entity.SolutionExplanation;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.SolutionExplanationMapper;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.content.domain.entity.SolutionModerationAction;
import com.anishan.content.mapper.SolutionModerationActionMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;
import java.util.List;
import java.util.Objects;

/** Content-database-only write transactions. Callers must complete problem-service checks before entering. */
@Service
public class SolutionLocalWriteService {

    private final SolutionExplanationMapper solutionMapper;
    private final SolutionExplanationContentService contentService;
    private final SolutionModerationActionMapper auditMapper;
    private final ContentEventOutboxService outbox;
    private final Clock clock;

    public SolutionLocalWriteService(SolutionExplanationMapper solutionMapper,
                                     SolutionExplanationContentService contentService,
                                     SolutionModerationActionMapper auditMapper, ContentEventOutboxService outbox,
                                     @Qualifier("contentCommunityOutboxClock") Clock clock) {
        this.solutionMapper = solutionMapper;
        this.contentService = contentService;
        this.auditMapper = auditMapper;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public boolean create(Long authorId, DetailSolutionDto dto, boolean admin) {
        requireSocialActor(authorId);
        LocalDateTime now = now();
        SolutionExplanation solution = new SolutionExplanation()
                .setTitle(dto.getTitle())
                .setProblemId(dto.getProblemId())
                .setUserId(authorId)
                .setTopUp(admin && Boolean.TRUE.equals(dto.getTopUp()))
                .setPrivate_(Boolean.TRUE.equals(dto.getPrivate_()))
                .setDelFlag(false)
                .setModerationState(SolutionModerationState.NORMAL)
                .setCommentsOpen(true)
                .setLikeCount(0L)
                .setCommentCount(0L)
                .setVersion(0L)
                .setFirstPublishedAt(Boolean.TRUE.equals(dto.getPrivate_()) ? null : now)
                .setCreateTime(now)
                .setUpdateTime(now);
        if (solutionMapper.insert(solution) != 1 || solution.getSolutionId() == null) {
            throw new ApiStatusException(409, "题解创建失败，请重试");
        }
        contentService.save(solution.getSolutionId(), dto.getContent());
        if (solution.getFirstPublishedAt() != null) published(solution.getSolutionId(), authorId);
        return true;
    }

    @Transactional
    public boolean update(Long callerId, DetailSolutionDto dto, SolutionRecord expected, boolean admin) {
        if (expected == null || dto.getSolutionId() == null) {
            throw new ApiStatusException(404, "题解不存在");
        }
        SolutionRecord current = solutionMapper.selectRecordForUpdate(dto.getSolutionId());
        if (current == null || Boolean.TRUE.equals(current.getDelFlag())) {
            throw new ApiStatusException(404, "题解不存在");
        }
        if (!admin && !Objects.equals(current.getUserId(), callerId)) {
            throw new ApiStatusException(404, "题解不存在");
        }
        if (!Objects.equals(current.getProblemId(), expected.getProblemId())
                || !Objects.equals(current.getVersion(), expected.getVersion())) {
            throw new ApiStatusException(409, "题解已被其他请求修改，请刷新后重试");
        }
        if (!Objects.equals(current.getProblemId(), dto.getProblemId())) {
            throw new ApiStatusException(400, "不能修改题解关联的题目");
        }
        boolean privateValue = dto.getPrivate_() == null
                ? Boolean.TRUE.equals(current.getPrivate_()) : dto.getPrivate_();
        Boolean topUp = admin ? dto.getTopUp() : null;
        boolean firstPublication = !privateValue && current.getModerationState() == SolutionModerationState.NORMAL
                && current.getFirstPublishedAt() == null;
        int changed = solutionMapper.updateFieldsByVersion(dto.getSolutionId(), current.getUserId(),
                current.getVersion() == null ? 0L : current.getVersion(), dto.getTitle(), privateValue, topUp,
                firstPublication ? now() : null);
        if (changed != 1) {
            throw new ApiStatusException(409, "题解已被其他请求修改，请刷新后重试");
        }
        contentService.save(dto.getSolutionId(), dto.getContent());
        if (firstPublication) published(current.getSolutionId(), current.getUserId());
        return true;
    }

    @Transactional
    public boolean deleteOwned(List<Long> solutionIds, Long userId) {
        if (solutionIds == null || solutionIds.isEmpty() || solutionIds.size() > 50
                || solutionIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ApiStatusException(400, "题解参数有误");
        }
        List<Long> ids = solutionIds.stream().distinct().sorted().collect(java.util.stream.Collectors.toList());
        int active = 0;
        // Acquire every solution lock in ID order and validate the entire batch before writing.
        for (Long id : ids) {
            SolutionRecord row = solutionMapper.selectRecordForUpdate(id);
            if (row == null || !Objects.equals(row.getUserId(), userId))
                throw new ApiStatusException(404, "题解不存在或无权操作");
            if (!Boolean.TRUE.equals(row.getDelFlag())) active++;
        }
        if (active > 0 && solutionMapper.softDeleteOwned(ids, userId) != active)
            throw new ApiStatusException(409, "题解已变化，请重试");
        return true;
    }

    /** Fresh problem checks and permission checks are performed by the coordinator before this transaction. */
    @Transactional(rollbackFor = Exception.class)
    public SolutionModerationState moderate(SolutionRecord expected, Long operatorId, String action,
                                             String reason, boolean publicReadable) {
        requireSocialActor(operatorId);
        SolutionRecord current = solutionMapper.selectRecordForUpdate(expected.getSolutionId());
        if (current == null) throw new ApiStatusException(404, "题解不存在");
        boolean delete = "DELETE".equals(action);
        SolutionModerationState state = "AUTHOR_ONLY".equals(action)
                ? SolutionModerationState.AUTHOR_ONLY : SolutionModerationState.NORMAL;
        if (delete && Boolean.TRUE.equals(current.getDelFlag())) return current.getModerationState();
        if (Boolean.TRUE.equals(current.getDelFlag())) throw new ApiStatusException(404, "题解不存在");
        if (!delete && current.getModerationState() == state) return state;
        if (!Objects.equals(current.getVersion(), expected.getVersion())
                || !Objects.equals(current.getProblemId(), expected.getProblemId()))
            throw new ApiStatusException(409, "题解已变化，请刷新后重试");
        if ("RESTORE".equals(action) && !Boolean.TRUE.equals(current.getPrivate_()) && !publicReadable)
            throw new ApiStatusException(400, "该题目当前不能发布公开题解");
        boolean firstPublication = !delete && state == SolutionModerationState.NORMAL && publicReadable
                && !Boolean.TRUE.equals(current.getPrivate_()) && current.getFirstPublishedAt() == null;
        LocalDateTime now = now();
        if (solutionMapper.moderateByVersion(current.getSolutionId(), current.getVersion(),
                delete ? current.getModerationState().name() : state.name(), delete,
                firstPublication ? now : null, now) != 1)
            throw new ApiStatusException(409, "题解已变化，请重试");
        SolutionModerationAction audit = new SolutionModerationAction().setSolutionId(current.getSolutionId())
                .setTargetType("SOLUTION").setTargetId(current.getSolutionId()).setAuthorId(current.getUserId())
                .setOperatorId(operatorId).setAction(action).setReason(reason).setCreatedAt(now);
        if (auditMapper.insert(audit) != 1 || audit.getActionId() == null)
            throw new IllegalStateException("Unable to create solution moderation audit");
        outbox.recordCommunity(event(CommunityEventType.SOLUTION_MODERATED, current.getSolutionId(), operatorId)
                .setDedupeKey("solution-moderated:" + audit.getActionId()).setAction(action).setReason(reason)
                .setRecipientIds(Collections.singletonList(String.valueOf(current.getUserId()))));
        if (firstPublication) published(current.getSolutionId(), current.getUserId());
        return delete ? current.getModerationState() : state;
    }

    private void published(Long solutionId, Long authorId) {
        requireSocialActor(authorId);
        outbox.recordCommunity(event(CommunityEventType.SOLUTION_PUBLISHED, solutionId, authorId)
                .setDedupeKey("solution-published:" + solutionId).setRecipientIds(Collections.emptyList()));
    }

    private CommunityEvent event(CommunityEventType type, Long solutionId, Long actorId) {
        return new CommunityEvent().setSchemaVersion(1).setEventId(UUID.randomUUID().toString())
                .setEventType(type).setActorId(String.valueOf(actorId)).setSolutionId(String.valueOf(solutionId))
                .setOccurredAt(OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS).toString());
    }

    private LocalDateTime now() { return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS); }

    private void requireSocialActor(Long userId) {
        if (userId == null || userId <= 0) throw new ApiStatusException(403, "该账号不能执行社交操作");
    }

    @Transactional
    public boolean setTopUp(Long solutionId, boolean topUp) {
        SolutionRecord current = solutionMapper.selectRecordForUpdate(solutionId);
        if (current == null || Boolean.TRUE.equals(current.getDelFlag())) {
            throw new ApiStatusException(404, "题解不存在");
        }
        if (Objects.equals(current.getTopUp(), topUp)) return true;
        if (solutionMapper.setTopUpByVersion(solutionId, current.getVersion() == null ? 0L : current.getVersion(), topUp) != 1) {
            throw new ApiStatusException(409, "题解已被其他请求修改，请刷新后重试");
        }
        return true;
    }
}
