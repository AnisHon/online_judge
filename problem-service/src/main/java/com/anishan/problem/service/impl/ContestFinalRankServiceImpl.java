package com.anishan.problem.service.impl;

import com.anishan.commons.enumeration.ContestType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.config.ContestRankProperties;
import com.anishan.problem.domain.ContestRankBuildLease;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.ContestRankSnapshot;
import com.anishan.problem.domain.enumeration.ContestRankState;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestRankEntryMapper;
import com.anishan.problem.mapper.ContestRankSnapshotMapper;
import com.anishan.problem.service.ContestFinalRankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ContestFinalRankServiceImpl implements ContestFinalRankService {

    private static final String CURRENT_SOURCE = "CURRENT";
    private static final String CURRENT_RULE = "WEIGHTED_LATEST_V1";
    private static final String LEGACY_SOURCE = "LEGACY";
    private static final String LEGACY_RULE = "LEGACY_RECORD_V1";
    private static final long PENDING_GRACE_MINUTES = 15L;
    private static final long WAITING_RECHECK_SECONDS = 15L;

    private final ContestMapper contestMapper;
    private final ContestAttemptMapper attemptMapper;
    private final ContestRankSnapshotMapper snapshotMapper;
    private final ContestRankEntryMapper entryMapper;
    private final ContestRankProperties properties;
    private final Clock clock;

    public ContestFinalRankServiceImpl(ContestMapper contestMapper,
                                       ContestAttemptMapper attemptMapper,
                                       ContestRankSnapshotMapper snapshotMapper,
                                       ContestRankEntryMapper entryMapper,
                                       ContestRankProperties properties,
                                       @Qualifier("contestSubmissionClock") Clock clock) {
        this.contestMapper = contestMapper;
        this.attemptMapper = attemptMapper;
        this.snapshotMapper = snapshotMapper;
        this.entryMapper = entryMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initializeForContest(Long contestId) {
        Contest contest = lockContest(contestId);
        if (contest.getType() != ContestType.CONTEST) {
            return;
        }
        initializeHeaderIfAbsent(contest, LocalDateTime.now(clock));
    }

    /** Create a missing header or make the existing due state visible to the scheduler. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureScheduled(Long contestId) {
        Contest contest = lockContest(contestId);
        if (contest.getType() != ContestType.CONTEST) {
            throw new ApiStatusException(400, "作业没有最终比赛榜单");
        }
        initializeHeaderIfAbsent(contest, LocalDateTime.now(clock));
    }

    /** Register a rebuild only. Aggregation always runs on the bounded background worker. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean requestRebuild(Long contestId) {
        Contest contest = lockContest(contestId);
        LocalDateTime now = LocalDateTime.now(clock);
        requireEndedContest(contest, now);
        initializeHeaderIfAbsent(contest, now);
        ContestRankSnapshot header = snapshotMapper.selectByContestIdForUpdate(contestId);
        if (header == null) {
            throw new IllegalStateException("Final-rank header was not initialized");
        }
        long pendingCount = attemptMapper.countPendingByContestId(contestId);
        List<Long> inconsistent = collectInconsistentReferences(contest);
        if (pendingCount > 0) {
            throw new ApiStatusException(409, "仍有判题结果未完成，暂不能重建最终榜单");
        }
        if (!inconsistent.isEmpty()) {
            log.error("Final-rank rebuild blocked by inconsistent attempt/log state contestId={} refs={}",
                    contestId, compactIds(inconsistent));
            throw new ApiStatusException(409, "判题记录状态不一致，暂不能重建最终榜单");
        }
        if (header.getState() == ContestRankState.BUILDING
                && header.getLeaseUntil() != null && header.getLeaseUntil().isAfter(now)) {
            return false;
        }
        return snapshotMapper.scheduleRebuild(contestId, now) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findDueContests(int limit) {
        int boundedLimit = Math.max(1, Math.min(properties.getBatchSize(), limit));
        return snapshotMapper.selectDueContestIds(LocalDateTime.now(clock), boundedLimit);
    }

    /** Claim under the global contest -> rank-header lock order; no claim is held while queued. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContestRankBuildLease claim(Long contestId, String leaseOwner) {
        if (leaseOwner == null || leaseOwner.trim().isEmpty() || leaseOwner.length() > 64) {
            throw new IllegalArgumentException("A unique rank lease owner is required");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Contest contest = lockContest(contestId);
        if (contest.getType() != ContestType.CONTEST || !Integer.valueOf(0).equals(contest.getDelFlag())
                || contest.getEndTime() == null || contest.getEndTime().isAfter(now)) {
            return null;
        }
        initializeHeaderIfAbsent(contest, now);
        ContestRankSnapshot header = snapshotMapper.selectByContestIdForUpdate(contestId);
        if (header == null) {
            throw new IllegalStateException("Final-rank header was not initialized");
        }
        if (header.getState() == ContestRankState.READY) {
            return null;
        }
        if (header.getState() == ContestRankState.BUILDING
                && header.getLeaseUntil() != null && header.getLeaseUntil().isAfter(now)) {
            return null;
        }
        if (header.getState() != ContestRankState.BUILDING
                && header.getNextBuildAt() != null && header.getNextBuildAt().isAfter(now)) {
            return null;
        }

        RankMode mode;
        try {
            mode = modeFor(contest.getScoringVersion());
        } catch (ApiStatusException invalidMode) {
            markUnclaimedError(header, 0L, "UNSUPPORTED_SCORING_VERSION value=" + contest.getScoringVersion(), now);
            return null;
        }
        if (!mode.sourceMode.equals(header.getSourceMode()) || !mode.ruleVersion.equals(header.getRuleVersion())) {
            markUnclaimedError(header, 0L, "RANK_MODE_MISMATCH scoringVersion=" + contest.getScoringVersion(), now);
            return null;
        }

        long pendingCount = attemptMapper.countPendingByContestId(contestId);
        List<Long> inconsistent = collectInconsistentReferences(contest);
        if (!inconsistent.isEmpty()) {
            markUnclaimedError(header, pendingCount,
                    "ATTEMPT_LOG_INCONSISTENCY refs=" + compactIds(inconsistent), now);
            log.error("Final rank blocked by inconsistent attempt/log state contestId={} refs={}",
                    contestId, compactIds(inconsistent));
            return null;
        }
        if (pendingCount > 0) {
            String references = compactIds(attemptMapper.selectPendingAttemptReferences(contestId, 8));
            if (!now.isBefore(contest.getEndTime().plusMinutes(PENDING_GRACE_MINUTES))) {
                markUnclaimedError(header, pendingCount,
                        "PENDING_TIMEOUT count=" + pendingCount + " refs=" + references, now);
                log.error("Final rank waiting for late judge callbacks contestId={} pending={} refs={}",
                        contestId, pendingCount, references);
            } else {
                snapshotMapper.markWaitingForPending(contestId, pendingCount,
                        now.plusSeconds(WAITING_RECHECK_SECONDS), "PENDING_JUDGE_RESULT", now);
            }
            return null;
        }

        long sourceSeq = contest.getNextAttemptSeq() == null ? 0L : contest.getNextAttemptSeq();
        if (sourceSeq < 0) {
            markUnclaimedError(header, 0L, "NEGATIVE_CONTEST_SOURCE_SEQUENCE", now);
            return null;
        }
        long lastAllocated = Math.max(valueOrZero(header.getNextVersion()), valueOrZero(header.getVersion()));
        if (lastAllocated == Long.MAX_VALUE) {
            markUnclaimedError(header, 0L, "RANK_VERSION_EXHAUSTED", now);
            return null;
        }
        long candidateVersion = lastAllocated + 1;
        LocalDateTime leaseUntil = now.plusSeconds(properties.getLeaseSeconds());
        int claimed = snapshotMapper.claimBuild(contestId, leaseOwner, leaseUntil, candidateVersion,
                sourceSeq, pendingCount, now);
        if (claimed != 1) {
            return null;
        }
        return new ContestRankBuildLease(contestId, candidateVersion, sourceSeq, leaseOwner,
                mode.sourceMode, mode.ruleVersion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean renewLease(ContestRankBuildLease lease) {
        if (lease == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return snapshotMapper.renewLease(lease.getContestId(), lease.getCandidateVersion(),
                lease.getLeaseOwner(), now.plusSeconds(properties.getLeaseSeconds()), now) == 1;
    }

    /** One DB-side INSERT ... SELECT creates the whole immutable candidate; no rows are loaded into Java. */
    @Override
    @Transactional(timeout = 60, rollbackFor = Exception.class)
    public long buildCandidate(ContestRankBuildLease lease) {
        LocalDateTime before = LocalDateTime.now(clock);
        ContestRankSnapshot beforeHeader = snapshotMapper.selectByContestId(lease.getContestId());
        requireActiveLease(beforeHeader, lease, before);

        LocalDateTime createdAt = LocalDateTime.now(clock);
        entryMapper.insertCandidate(lease.getContestId(), lease.getCandidateVersion(), createdAt);
        long entryCount = entryMapper.countByContestVersion(lease.getContestId(), lease.getCandidateVersion());
        long rosterCount = entryMapper.countRosterUsers(lease.getContestId());
        if (entryCount != rosterCount) {
            throw new IllegalStateException("Candidate rank row count does not match contest roster");
        }

        // Do not hold either long-lived row lock while the aggregation query runs.
        Contest contest = lockContest(lease.getContestId());
        ContestRankSnapshot afterHeader = snapshotMapper.selectByContestIdForUpdate(lease.getContestId());
        requireActiveLease(afterHeader, lease, LocalDateTime.now(clock));
        if (!sameSourceSequence(contest, lease)) {
            throw new LeaseLostException("Contest source sequence changed while building rank");
        }
        if (!sameMode(contest, lease)) {
            throw new LeaseLostException("Contest scoring mode changed while building rank");
        }
        return entryCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishCandidate(ContestRankBuildLease lease, long totalUsers) {
        LocalDateTime now = LocalDateTime.now(clock);
        Contest contest = lockContest(lease.getContestId());
        requireEndedContest(contest, now);
        ContestRankSnapshot header = snapshotMapper.selectByContestIdForUpdate(lease.getContestId());
        if (!isActiveLease(header, lease, now) || !sameSourceSequence(contest, lease)) {
            return false;
        }

        long pending = attemptMapper.countPendingByContestId(lease.getContestId());
        List<Long> inconsistent = collectInconsistentReferences(contest);
        if (pending > 0 || !inconsistent.isEmpty()) {
            if (!inconsistent.isEmpty()) {
                markUnclaimedError(header, pending,
                        "ATTEMPT_LOG_INCONSISTENCY refs=" + compactIds(inconsistent), now);
            } else if (!now.isBefore(contest.getEndTime().plusMinutes(PENDING_GRACE_MINUTES))) {
                markUnclaimedError(header, pending, "PENDING_TIMEOUT count=" + pending
                        + " refs=" + compactIds(attemptMapper.selectPendingAttemptReferences(lease.getContestId(), 8)), now);
            } else {
                snapshotMapper.markWaitingForPending(lease.getContestId(), pending,
                        now.plusSeconds(WAITING_RECHECK_SECONDS), "PENDING_JUDGE_RESULT", now);
            }
            return false;
        }

        long actualEntries = entryMapper.countByContestVersion(lease.getContestId(), lease.getCandidateVersion());
        long actualRoster = entryMapper.countRosterUsers(lease.getContestId());
        if (actualEntries != totalUsers || actualEntries != actualRoster) {
            throw new IllegalStateException("Candidate rank version is incomplete");
        }
        return snapshotMapper.publishCandidate(lease.getContestId(), lease.getCandidateVersion(),
                lease.getSourceSeq(), lease.getSourceMode(), lease.getRuleVersion(), totalUsers,
                now, lease.getLeaseOwner()) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markBuildFailed(ContestRankBuildLease lease, String safeError) {
        if (lease == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Contest contest = contestMapper.selectContestForUpdate(lease.getContestId());
        if (contest == null) {
            return;
        }
        ContestRankSnapshot header = snapshotMapper.selectByContestIdForUpdate(lease.getContestId());
        if (!isActiveLease(header, lease, now)) {
            return;
        }
        int attempts = Math.max(0, valueOrZero(header.getBuildAttempts())) + 1;
        long pending = attemptMapper.countPendingByContestId(lease.getContestId());
        snapshotMapper.markBuildFailed(lease.getContestId(), lease.getCandidateVersion(),
                lease.getLeaseOwner(), attempts, now.plusSeconds(retryDelaySeconds(attempts)),
                pending, truncate(safeError, 1000), now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupVersionBatch(Long contestId, int batchSize) {
        lockContest(contestId);
        ContestRankSnapshot header = snapshotMapper.selectByContestIdForUpdate(contestId);
        if (header == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Long protectedCandidate = header.getState() == ContestRankState.BUILDING
                && header.getLeaseUntil() != null && header.getLeaseUntil().isAfter(now)
                ? header.getNextVersion() : null;
        return entryMapper.deleteUnretainedVersions(contestId, valueOrZero(header.getVersion()),
                protectedCandidate, Math.min(500, Math.max(1, batchSize)));
    }

    static long retryDelaySeconds(int attempt) {
        int exponent = Math.max(0, Math.min(30, attempt - 1));
        return Math.min(300L, 15L * (1L << exponent));
    }

    private List<Long> collectInconsistentReferences(Contest contest) {
        List<Long> references = new ArrayList<>(attemptMapper.selectInconsistentAttemptReferences(contest.getContestId(), 8));
        if (Integer.valueOf(2).equals(contest.getScoringVersion())) {
            references.addAll(attemptMapper.selectUnlinkedUnappliedSubmitIds(contest.getContestId(), 8));
        }
        return references;
    }

    private void markUnclaimedError(ContestRankSnapshot header, long pendingCount, String reason, LocalDateTime now) {
        int attempts = Math.max(0, valueOrZero(header.getBuildAttempts())) + 1;
        snapshotMapper.markUnclaimedError(header.getContestId(), attempts,
                now.plusSeconds(retryDelaySeconds(attempts)), pendingCount, truncate(reason, 1000), now);
    }

    private void initializeHeaderIfAbsent(Contest contest, LocalDateTime now) {
        RankMode mode = initialMode(contest.getScoringVersion());
        ContestRankSnapshot initial = new ContestRankSnapshot();
        initial.setContestId(contest.getContestId());
        initial.setState(ContestRankState.WAITING);
        initial.setVersion(0L);
        initial.setNextVersion(0L);
        initial.setBuildAttempts(0);
        initial.setNextBuildAt(now);
        initial.setSourceSeq(0L);
        initial.setSourceMode(mode.sourceMode);
        initial.setRuleVersion(mode.ruleVersion);
        initial.setTotalUsers(0L);
        initial.setPendingCount(0L);
        initial.setUpdatedAt(now);
        snapshotMapper.insertInitial(initial);
    }

    private Contest lockContest(Long contestId) {
        if (contestId == null || contestId <= 0) {
            throw new ApiStatusException(400, "比赛编号无效");
        }
        Contest contest = contestMapper.selectContestForUpdate(contestId);
        if (contest == null) {
            throw new ApiStatusException(404, "比赛不存在");
        }
        return contest;
    }

    private void requireEndedContest(Contest contest, LocalDateTime now) {
        if (contest.getType() != ContestType.CONTEST) {
            throw new ApiStatusException(400, "作业没有最终比赛榜单");
        }
        if (contest.getEndTime() == null || contest.getEndTime().isAfter(now)) {
            throw new ApiStatusException(409, "比赛尚未结束");
        }
    }

    private RankMode modeFor(Integer scoringVersion) {
        if (Integer.valueOf(1).equals(scoringVersion)) {
            return new RankMode(LEGACY_SOURCE, LEGACY_RULE);
        }
        if (scoringVersion == null || Integer.valueOf(2).equals(scoringVersion)) {
            return new RankMode(CURRENT_SOURCE, CURRENT_RULE);
        }
        throw new ApiStatusException(409, "比赛评分版本不受支持");
    }

    private RankMode initialMode(Integer scoringVersion) {
        try {
            return modeFor(scoringVersion);
        } catch (ApiStatusException unsupported) {
            return new RankMode("UNSUPPORTED", "UNSUPPORTED");
        }
    }

    private boolean sameSourceSequence(Contest contest, ContestRankBuildLease lease) {
        return valueOrZero(contest.getNextAttemptSeq()) == valueOrZero(lease.getSourceSeq());
    }

    private boolean sameMode(Contest contest, ContestRankBuildLease lease) {
        try {
            RankMode current = modeFor(contest.getScoringVersion());
            return current.sourceMode.equals(lease.getSourceMode())
                    && current.ruleVersion.equals(lease.getRuleVersion());
        } catch (ApiStatusException unsupported) {
            return false;
        }
    }

    private void requireActiveLease(ContestRankSnapshot header, ContestRankBuildLease lease, LocalDateTime now) {
        if (!isActiveLease(header, lease, now)) {
            throw new LeaseLostException("Final-rank build lease expired or was replaced");
        }
    }

    private boolean isActiveLease(ContestRankSnapshot header, ContestRankBuildLease lease, LocalDateTime now) {
        return header != null && lease != null && header.getState() == ContestRankState.BUILDING
                && lease.getCandidateVersion().equals(header.getNextVersion())
                && lease.getLeaseOwner().equals(header.getLeaseOwner())
                && header.getLeaseUntil() != null && header.getLeaseUntil().isAfter(now);
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String compactIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "none";
        }
        List<Long> limited = ids.size() > 8 ? ids.subList(0, 8) : ids;
        return limited.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "RANK_BUILD_FAILED";
        }
        String safe = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    private static final class RankMode {
        private final String sourceMode;
        private final String ruleVersion;

        private RankMode(String sourceMode, String ruleVersion) {
            this.sourceMode = sourceMode;
            this.ruleVersion = ruleVersion;
        }
    }

    private static final class LeaseLostException extends IllegalStateException {
        private LeaseLostException(String message) {
            super(message);
        }
    }
}
