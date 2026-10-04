package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.ContestRankSnapshot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ContestRankSnapshotMapper extends BaseMapper<ContestRankSnapshot> {
    ContestRankSnapshot selectByContestId(@Param("contestId") Long contestId);

    ContestRankSnapshot selectByContestIdForUpdate(@Param("contestId") Long contestId);

    int insertInitial(@Param("snapshot") ContestRankSnapshot snapshot);

    List<Long> selectDueContestIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    int scheduleRebuild(@Param("contestId") Long contestId, @Param("now") LocalDateTime now);

    int markWaitingForPending(@Param("contestId") Long contestId,
                              @Param("pendingCount") long pendingCount,
                              @Param("nextBuildAt") LocalDateTime nextBuildAt,
                              @Param("lastError") String lastError,
                              @Param("now") LocalDateTime now);

    int markUnclaimedError(@Param("contestId") Long contestId,
                           @Param("buildAttempts") int buildAttempts,
                           @Param("nextBuildAt") LocalDateTime nextBuildAt,
                           @Param("pendingCount") long pendingCount,
                           @Param("lastError") String lastError,
                           @Param("now") LocalDateTime now);

    int claimBuild(@Param("contestId") Long contestId,
                   @Param("leaseOwner") String leaseOwner,
                   @Param("leaseUntil") LocalDateTime leaseUntil,
                   @Param("candidateVersion") long candidateVersion,
                   @Param("sourceSeq") long sourceSeq,
                   @Param("pendingCount") long pendingCount,
                   @Param("now") LocalDateTime now);

    int renewLease(@Param("contestId") Long contestId,
                   @Param("candidateVersion") long candidateVersion,
                   @Param("leaseOwner") String leaseOwner,
                   @Param("leaseUntil") LocalDateTime leaseUntil,
                   @Param("now") LocalDateTime now);

    int publishCandidate(@Param("contestId") Long contestId,
                         @Param("candidateVersion") long candidateVersion,
                         @Param("sourceSeq") long sourceSeq,
                         @Param("sourceMode") String sourceMode,
                         @Param("ruleVersion") String ruleVersion,
                         @Param("totalUsers") long totalUsers,
                         @Param("generatedAt") LocalDateTime generatedAt,
                         @Param("leaseOwner") String leaseOwner);

    int markBuildFailed(@Param("contestId") Long contestId,
                        @Param("candidateVersion") long candidateVersion,
                        @Param("leaseOwner") String leaseOwner,
                        @Param("buildAttempts") int buildAttempts,
                        @Param("nextBuildAt") LocalDateTime nextBuildAt,
                        @Param("pendingCount") long pendingCount,
                        @Param("lastError") String lastError,
                        @Param("now") LocalDateTime now);
}
