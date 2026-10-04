package com.anishan.problem.service;

import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.util.WeightedScoreCalculator;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.commons.enumeration.ProblemType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.ContestSubmissionContext;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.ContestAttempt;
import com.anishan.problem.domain.entity.ContestAnswerRecords;
import com.anishan.problem.domain.entity.ContestProblemSnapshot;
import com.anishan.problem.domain.entity.ContestRecords;
import com.anishan.problem.domain.entity.ProblemEventOutbox;
import com.anishan.problem.domain.entity.SupplementContest;
import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.problem.domain.entity.UserSubmit;
import com.anishan.problem.domain.entity.UserContestRelation;
import com.anishan.problem.domain.enumeration.ContestAttemptState;
import com.anishan.problem.domain.vo.UserAnswer;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestAnswerRecordsMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestRecordsMapper;
import com.anishan.problem.mapper.ProblemEventOutboxMapper;
import com.anishan.problem.mapper.SupplementContestMapper;
import com.anishan.problem.mapper.SubmitLogMapper;
import com.anishan.problem.mapper.UserContestMapper;
import com.anishan.problem.mapper.UserSubmitMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.time.Clock;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

/**
 * The single transactional acceptance boundary for formal OJ submissions.
 * It deliberately depends on mappers and the snapshot/outbox boundaries, not on ContestService.
 */
@Service
public class ContestSubmissionCoordinator {

    private static final String OJ_ATTEMPT_KIND = "OJ";

    private final ContestMapper contestMapper;
    private final UserContestMapper userContestMapper;
    private final UserSubmitMapper userSubmitMapper;
    private final SupplementContestMapper supplementContestMapper;
    private final ContestProblemSnapshotService snapshotService;
    private final ContestAttemptMapper attemptMapper;
    private final ContestRecordsMapper contestRecordsMapper;
    private final ContestAnswerRecordsMapper contestAnswerRecordsMapper;
    private final SubmitLogMapper submitLogMapper;
    private final ProblemEventOutboxMapper outboxMapper;
    private final SubmitLogService submitLogService;
    private final ProblemEventOutboxService outboxService;
    private final Clock clock;

    @Autowired
    public ContestSubmissionCoordinator(ContestMapper contestMapper,
                                        UserContestMapper userContestMapper,
                                        UserSubmitMapper userSubmitMapper,
                                        SupplementContestMapper supplementContestMapper,
                                        ContestProblemSnapshotService snapshotService,
                                        ContestAttemptMapper attemptMapper,
                                        ContestRecordsMapper contestRecordsMapper,
                                        ContestAnswerRecordsMapper contestAnswerRecordsMapper,
                                        SubmitLogMapper submitLogMapper,
                                        ProblemEventOutboxMapper outboxMapper,
                                        SubmitLogService submitLogService,
                                        ProblemEventOutboxService outboxService,
                                        @Qualifier("contestSubmissionClock") Clock clock) {
        this.contestMapper = contestMapper;
        this.userContestMapper = userContestMapper;
        this.userSubmitMapper = userSubmitMapper;
        this.supplementContestMapper = supplementContestMapper;
        this.snapshotService = snapshotService;
        this.attemptMapper = attemptMapper;
        this.contestRecordsMapper = contestRecordsMapper;
        this.contestAnswerRecordsMapper = contestAnswerRecordsMapper;
        this.submitLogMapper = submitLogMapper;
        this.outboxMapper = outboxMapper;
        this.submitLogService = submitLogService;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public ContestSubmissionContext acceptOjSubmission(JudgeInfo judgeInfo) {
        validateSubmission(judgeInfo);
        LocalDateTime acceptedAt = LocalDateTime.now(clock);
        Long contestId = judgeInfo.getContestId();
        Long attemptSeq = null;
        BigDecimal maxScore = null;

        if (contestId != null) {
            Contest contest = contestMapper.selectContestForUpdate(contestId);
            if (contest == null) {
                throw new ApiStatusException(404, "活动不存在或不可提交");
            }
            assertSubmissionWindow(contest, judgeInfo.getUserId(), acceptedAt);

            List<ContestProblemSnapshot> snapshot = snapshotService.createIfAbsent(contestId);
            ContestProblemSnapshot frozenProblem = snapshot.stream()
                    .filter(item -> judgeInfo.getProblemId().equals(item.getProblemId()))
                    .findFirst()
                    .orElseThrow(() -> new ApiStatusException(409, "题目不属于当前活动的冻结题单"));
            if (frozenProblem.getProblemType() == null
                    || frozenProblem.getProblemType() != com.anishan.commons.enumeration.ProblemType.OJ.getValue()) {
                throw new ApiStatusException(409, "当前活动题目类型已变化，请重新打开题目");
            }
            maxScore = frozenProblem.getMaxScore();

            long currentSeq = contest.getNextAttemptSeq() == null ? 0L : contest.getNextAttemptSeq();
            if (currentSeq < 0 || currentSeq == Long.MAX_VALUE) {
                throw new ApiStatusException(409, "活动提交序号暂不可分配");
            }
            attemptSeq = currentSeq + 1;
            if (contestMapper.updateNextAttemptSeq(contestId, currentSeq, attemptSeq) != 1) {
                throw new ApiStatusException(409, "活动提交状态已变化，请重试");
            }
            judgeInfo.setListScore(maxScore);
        }

        // The submit log is inserted before its attempt receipt; the outbox joins this TX.
        Long submitId = submitLogService.createQueued(judgeInfo);
        if (submitId == null || submitId <= 0) {
            throw new IllegalStateException("Accepted submission did not receive an ID");
        }
        judgeInfo.setSubmitId(submitId);

        if (contestId != null) {
            ContestAttempt attempt = new ContestAttempt();
            attempt.setContestId(contestId);
            attempt.setUserId(judgeInfo.getUserId());
            attempt.setProblemId(judgeInfo.getProblemId());
            attempt.setAttemptSeq(attemptSeq);
            attempt.setSubmitId(submitId);
            attempt.setKind(OJ_ATTEMPT_KIND);
            attempt.setAcceptedAt(acceptedAt);
            attempt.setState(ContestAttemptState.PENDING);
            attempt.setJudgeStatus(JudgeResult.QUEUE.getValue());
            if (attemptMapper.insert(attempt) != 1) {
                throw new IllegalStateException("Accepted contest attempt was not persisted");
            }
            saveAcceptedOjAnswer(contestId, judgeInfo);
        }

        outboxService.recordJudgeDispatch(judgeInfo);
        return new ContestSubmissionContext()
                .setSubmitId(submitId)
                .setContestId(contestId)
                .setUserId(judgeInfo.getUserId())
                .setProblemId(judgeInfo.getProblemId())
                .setAttemptSeq(attemptSeq)
                .setMaxScore(maxScore)
                .setAcceptedAt(acceptedAt);
    }

    /** Accepts and completes a non-OJ attempt inside the caller's DB transaction. */
    @Transactional(rollbackFor = Exception.class)
    public ContestSubmissionContext acceptNonOjAnswer(Long contestId, Long userId, Long problemId,
                                                       BigDecimal earned, BigDecimal fullMark,
                                                       boolean correct) {
        requirePositive(contestId, "活动ID无效");
        requirePositive(userId, "用户身份无效");
        requirePositive(problemId, "题目ID无效");
        ContestSubmissionContext context = lockAndValidateActivity(contestId, userId, problemId);
        ContestProblemSnapshot snapshotProblem = snapshotService.getSnapshot(contestId).stream()
                .filter(item -> problemId.equals(item.getProblemId()))
                .findFirst().orElseThrow(() -> new ApiStatusException(409, "题目不属于当前活动的冻结题单"));
        if (snapshotProblem.getProblemType() == null
                || snapshotProblem.getProblemType() == ProblemType.OJ.getValue()) {
            throw new ApiStatusException(409, "当前题目类型不支持同步作答");
        }

        Contest contest = contestMapper.selectContestForUpdate(contestId);
        long currentSeq = contest.getNextAttemptSeq() == null ? 0L : contest.getNextAttemptSeq();
        if (currentSeq < 0 || currentSeq == Long.MAX_VALUE) {
            throw new ApiStatusException(409, "活动提交序号暂不可分配");
        }
        long attemptSeq = currentSeq + 1;
        if (contestMapper.updateNextAttemptSeq(contestId, currentSeq, attemptSeq) != 1) {
            throw new ApiStatusException(409, "活动提交状态已变化，请重试");
        }

        BigDecimal normalized = WeightedScoreCalculator.normalize(
                earned, fullMark, snapshotProblem.getMaxScore());
        LocalDateTime acceptedAt = context.getAcceptedAt();
        ContestAttempt attempt = new ContestAttempt();
        attempt.setAttemptId(IdWorker.getId());
        attempt.setContestId(contestId);
        attempt.setUserId(userId);
        attempt.setProblemId(problemId);
        attempt.setAttemptSeq(attemptSeq);
        attempt.setKind("NON_OJ");
        attempt.setAcceptedAt(acceptedAt);
        attempt.setState(ContestAttemptState.COMPLETED);
        attempt.setJudgeStatus(correct ? JudgeResult.ACCEPT.getValue() : JudgeResult.WRONG_ANSWER.getValue());
        attempt.setScore(normalized);
        attempt.setCorrect(correct);
        attempt.setCompletedAt(acceptedAt);
        if (attemptMapper.insert(attempt) != 1) {
            throw new IllegalStateException("Accepted non-OJ attempt was not persisted");
        }
        return context.setAttemptId(attempt.getAttemptId())
                .setAttemptSeq(attemptSeq)
                .setScore(normalized)
                .setMaxScore(snapshotProblem.getMaxScore())
                .setAcceptedAt(acceptedAt);
    }

    /** Validates an answer draft while holding the same contest lock as formal submissions. */
    @Transactional(rollbackFor = Exception.class)
    public ContestSubmissionContext prepareDraft(Long contestId, Long userId, Long problemId) {
        requirePositive(contestId, "活动ID无效");
        requirePositive(userId, "用户身份无效");
        requirePositive(problemId, "题目ID无效");
        return lockAndValidateActivity(contestId, userId, problemId);
    }

    private ContestSubmissionContext lockAndValidateActivity(Long contestId, Long userId, Long problemId) {
        Contest contest = contestMapper.selectContestForUpdate(contestId);
        if (contest == null) {
            throw new ApiStatusException(404, "活动不存在");
        }
        LocalDateTime acceptedAt = LocalDateTime.now(clock);
        assertSubmissionWindow(contest, userId, acceptedAt);
        ContestProblemSnapshot frozenProblem = snapshotService.createIfAbsent(contestId).stream()
                .filter(item -> problemId.equals(item.getProblemId()))
                .findFirst().orElseThrow(() -> new ApiStatusException(409, "题目不属于当前活动的冻结题单"));
        if (frozenProblem.getProblemType() == null) {
            throw new ApiStatusException(409, "冻结题目类型无效");
        }
        return new ContestSubmissionContext()
                .setContestId(contestId)
                .setUserId(userId)
                .setProblemId(problemId)
                .setMaxScore(frozenProblem.getMaxScore())
                .setAcceptedAt(acceptedAt);
    }

    private void saveAcceptedOjAnswer(Long contestId, JudgeInfo judgeInfo) {
        contestRecordsMapper.insertDraftIfAbsent(IdWorker.getId(), contestId,
                judgeInfo.getUserId(), judgeInfo.getProblemId());
        ContestRecords projection = contestRecordsMapper.selectForUpdate(contestId,
                judgeInfo.getUserId(), judgeInfo.getProblemId());
        if (projection == null || projection.getRecordId() == null) {
            throw new IllegalStateException("Accepted OJ answer projection was not initialized");
        }
        UserAnswer answer = new UserAnswer(null, judgeInfo.getCode(), judgeInfo.getLanguageId());
        contestAnswerRecordsMapper.upsertAnswer(projection.getRecordId(), answer);
    }

    private void requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new ApiStatusException(400, message);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean retryFailedDispatch(Long submitId) {
        if (submitId == null || submitId <= 0) {
            throw new ApiStatusException(400, "提交编号无效");
        }
        SubmitLog initial = submitLogMapper.selectById(submitId);
        if (initial == null) {
            throw new ApiStatusException(404, "提交记录不存在");
        }

        Long contestId = initial.getContestId();
        if (contestId != null && contestMapper.selectContestForUpdate(contestId) == null) {
            throw new ApiStatusException(409, "活动状态已变化，不能重试该派发");
        }

        // Match result application lock order: contest -> log -> attempt -> outbox.
        SubmitLog submitLog = submitLogMapper.selectByIdForUpdate(submitId);
        if (submitLog == null || !sameSubmissionIdentity(initial, submitLog)) {
            throw new ApiStatusException(409, "提交状态已变化，不能重试该派发");
        }
        if (Boolean.TRUE.equals(submitLog.getResultApplied()) || !isNonTerminal(submitLog.getStatus())) {
            throw new ApiStatusException(409, "判题已结束，不能重试派发");
        }
        if (contestId != null) {
            ContestAttempt attempt = attemptMapper.selectBySubmitIdForUpdate(submitId);
            if (attempt == null || !OJ_ATTEMPT_KIND.equals(attempt.getKind())
                    || attempt.getState() != ContestAttemptState.PENDING) {
                throw new ApiStatusException(409, "活动提交状态已结束，不能重试派发");
            }
        }

        String dedupeKey = "judge-dispatch:" + submitId;
        ProblemEventOutbox event = outboxMapper.selectByDedupeKeyForUpdate(dedupeKey);
        if (event == null || !ProblemEventOutboxService.JUDGE_DISPATCH.equals(event.getEventType())
                || !"FAILED".equals(event.getStatus())) {
            throw new ApiStatusException(409, "当前派发记录不可重试");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (outboxMapper.retryFailedJudgeDispatch(event.getEventId(), dedupeKey, now) != 1) {
            throw new ApiStatusException(409, "派发状态已变化，请刷新后重试");
        }
        return true;
    }

    private void assertSubmissionWindow(Contest contest, Long userId, LocalDateTime now) {
        boolean joined = userContestMapper.selectCount(new LambdaQueryWrapper<UserContestRelation>()
                .eq(UserContestRelation::getContestId, contest.getContestId())
                .eq(UserContestRelation::getUserId, userId)) > 0;
        boolean handedIn = userSubmitMapper.selectCount(new LambdaQueryWrapper<UserSubmit>()
                .eq(UserSubmit::getContestId, contest.getContestId())
                .eq(UserSubmit::getUserId, userId)) > 0;
        SupplementContest supplement = contest.getType() == com.anishan.commons.enumeration.ContestType.HOMEWORK
                ? supplementContestMapper.selectOne(new LambdaQueryWrapper<SupplementContest>()
                    .eq(SupplementContest::getContestId, contest.getContestId())
                    .eq(SupplementContest::getUserId, userId))
                : null;
        ContestSubmissionPolicy.requireCanSubmit(contest, joined, handedIn, supplement, now);
    }

    private void validateSubmission(JudgeInfo info) {
        if (info == null || !positive(info.getUserId()) || !positive(info.getProblemId())
                || !positive(info.getLanguageId()) || info.getCode() == null
                || info.getLanguage() == null || info.getLanguage().trim().isEmpty()
                || (info.getContestId() != null && !positive(info.getContestId()))) {
            throw new ApiStatusException(400, "判题参数无效");
        }
    }

    private boolean isNonTerminal(JudgeResult status) {
        return status == JudgeResult.QUEUE || status == JudgeResult.COMPILING || status == JudgeResult.RUNNING;
    }

    private boolean sameSubmissionIdentity(SubmitLog expected, SubmitLog actual) {
        return expected.getSubmitId().equals(actual.getSubmitId())
                && java.util.Objects.equals(expected.getUserId(), actual.getUserId())
                && java.util.Objects.equals(expected.getProblemId(), actual.getProblemId())
                && java.util.Objects.equals(expected.getContestId(), actual.getContestId());
    }

    private boolean positive(Long value) {
        return value != null && value > 0;
    }
}
