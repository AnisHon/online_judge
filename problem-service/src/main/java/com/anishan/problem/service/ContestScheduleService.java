package com.anishan.problem.service;

import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.dto.ContestDto;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.ContestRankSnapshot;
import com.anishan.problem.domain.enumeration.ContestRankState;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestRankSnapshotMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/** Owns schedule/list changes and serializes them with submission acceptance. */
@Service
public class ContestScheduleService {

    private final ContestMapper contestMapper;
    private final ContestAttemptMapper attemptMapper;
    private final ContestRankSnapshotMapper rankSnapshotMapper;
    private final ContestProblemSnapshotService snapshotService;
    private final Clock clock;

    @Autowired
    public ContestScheduleService(ContestMapper contestMapper,
                                  ContestAttemptMapper attemptMapper,
                                  ContestRankSnapshotMapper rankSnapshotMapper,
                                  ContestProblemSnapshotService snapshotService,
                                  @Qualifier("contestSubmissionClock") Clock clock) {
        this.contestMapper = contestMapper;
        this.attemptMapper = attemptMapper;
        this.rankSnapshotMapper = rankSnapshotMapper;
        this.snapshotService = snapshotService;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean update(ContestDto request) {
        if (request == null || request.getContestId() == null || request.getContestId() <= 0) {
            throw new ApiStatusException(400, "活动ID无效");
        }
        Contest current = contestMapper.selectContestForUpdate(request.getContestId());
        if (current == null) {
            throw new ApiStatusException(404, "活动不存在");
        }

        LocalDateTime start = request.getStartTime() == null ? current.getStartTime() : request.getStartTime();
        LocalDateTime end = request.getEndTime() == null ? current.getEndTime() : request.getEndTime();
        Long newListId = request.getListId() == null ? current.getListId() : request.getListId();
        if (start == null || end == null || !start.isBefore(end)) {
            throw new ApiStatusException(400, "结束时间必须晚于开始时间");
        }
        if (newListId == null || newListId <= 0) {
            throw new ApiStatusException(400, "题单ID无效");
        }
        if (request.getAuth() != null && request.getAuth().name().equalsIgnoreCase("PRIVATE")
                && !StringUtils.hasText(request.getPwd()) && !StringUtils.hasText(current.getPwd())) {
            throw new ApiStatusException(400, "私有活动必须设置访问密码");
        }

        boolean listChanged = !Objects.equals(current.getListId(), newListId);
        boolean scheduleChanged = listChanged || !Objects.equals(current.getStartTime(), start)
                || !Objects.equals(current.getEndTime(), end);
        if (scheduleChanged) {
            ContestRankSnapshot rank = rankSnapshotMapper.selectByContestId(current.getContestId());
            if (rank != null && (rank.getState() == ContestRankState.BUILDING
                    || rank.getState() == ContestRankState.READY
                    || (rank.getVersion() != null && rank.getVersion() > 0)
                    || (listChanged && rank.getState() != ContestRankState.WAITING))) {
                throw new ApiStatusException(409, "已生成最终榜单的活动不能修改赛程或题单");
            }
            if (attemptMapper.countByContestId(current.getContestId()) > 0) {
                throw new ApiStatusException(409, "活动已有正式提交，不能修改赛程或题单");
            }
            if (listChanged) {
                // Reuses the T17 lock order and changes listId + snapshot atomically in this TX.
                snapshotService.resetIfNoAttempts(current.getContestId(), newListId);
            }
        }

        LambdaUpdateWrapper<Contest> update = Wrappers.lambdaUpdate(Contest.class)
                .eq(Contest::getContestId, current.getContestId())
                .set(Contest::getStartTime, start)
                .set(Contest::getEndTime, end)
                .set(Contest::getUpdateTime, LocalDateTime.now(clock));
        // resetIfNoAttempts has already changed list_id without overwriting snapshot metadata.
        if (request.getTitle() != null) update.set(Contest::getTitle, request.getTitle());
        if (request.getDescription() != null) update.set(Contest::getDescription, request.getDescription());
        if (request.getAuth() != null) update.set(Contest::getAuth, request.getAuth());
        if (request.getPwd() != null) update.set(Contest::getPwd, request.getPwd());
        return contestMapper.update(null, update) == 1;
    }
}
