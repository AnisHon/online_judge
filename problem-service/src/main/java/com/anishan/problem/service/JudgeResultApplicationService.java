package com.anishan.problem.service;

import com.anishan.api.client.judgeserver.domain.JudgeCaseResult;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.problem.domain.vo.JudgeSubmissionStatusVo;
import com.anishan.api.util.RedisJudgeSubmissionLock;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.ContestAttempt;
import com.anishan.problem.domain.entity.ContestRecords;
import com.anishan.problem.domain.entity.JudgeCaseLog;
import com.anishan.problem.domain.entity.Records;
import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.problem.domain.enumeration.ContestAttemptState;
import com.anishan.problem.domain.vo.UserAnswer;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestRecordsMapper;
import com.anishan.problem.mapper.JudgeCaseLogMapper;
import com.anishan.problem.mapper.SubmitLogMapper;
import com.anishan.problem.service.RecordsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Applies judge callbacks exactly once in the same DB transaction as cases, scores and first-AC events. */
@Service
@Slf4j
public class JudgeResultApplicationService {

    private final ContestMapper contestMapper;
    private final SubmitLogMapper submitLogMapper;
    private final ContestAttemptMapper attemptMapper;
    private final ContestRecordsMapper contestRecordsMapper;
    private final JudgeCaseLogMapper caseLogMapper;
    private final RecordsService recordsService;
    private final ProblemCompletionAwardService completionAwardService;
    private final RedisJudgeSubmissionLock submissionLock;
    private final Clock clock;

    public JudgeResultApplicationService(ContestMapper contestMapper,
                                         SubmitLogMapper submitLogMapper,
                                         ContestAttemptMapper attemptMapper,
                                         ContestRecordsMapper contestRecordsMapper,
                                         JudgeCaseLogMapper caseLogMapper,
                                         RecordsService recordsService,
                                         ProblemCompletionAwardService completionAwardService,
                                         RedisJudgeSubmissionLock submissionLock,
                                         @Qualifier("contestSubmissionClock") Clock clock) {
        this.contestMapper = contestMapper;
        this.submitLogMapper = submitLogMapper;
        this.attemptMapper = attemptMapper;
        this.contestRecordsMapper = contestRecordsMapper;
        this.caseLogMapper = caseLogMapper;
        this.recordsService = recordsService;
        this.completionAwardService = completionAwardService;
        this.submissionLock = submissionLock;
        this.clock = clock;
    }

    /** Intermediate state callbacks are serialized with final application and can never downgrade a terminal row. */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateIntermediateStatus(JudgeScore callback) {
        if (callback == null || callback.getSubmitId() == null || callback.getSubmitId() <= 0
                || (callback.getResult() != JudgeResult.COMPILING && callback.getResult() != JudgeResult.RUNNING)) {
            throw new ApiStatusException(400, "判题状态参数无效");
        }

        Long contestId = submitLogMapper.selectContestIdBySubmitId(callback.getSubmitId());
        if (contestId == null && submitLogMapper.selectById(callback.getSubmitId()) == null) {
            throw new ApiStatusException(404, "提交记录不存在");
        }
        lockContestIfNeeded(contestId);
        SubmitLog logRow = lockSubmitLog(callback.getSubmitId(), contestId);
        verifyOptionalIdentity(callback, logRow);

        if (Boolean.TRUE.equals(logRow.getResultApplied()) || isTerminal(logRow.getStatus())) {
            return false;
        }
        if (!canTransition(logRow.getStatus(), callback.getResult())) {
            return false;
        }

        ContestAttempt attempt = null;
        if (contestId != null) {
            attempt = lockAttempt(callback.getSubmitId(), logRow);
            if (attempt.getState() != ContestAttemptState.PENDING) {
                return false;
            }
        }

        if (submitLogMapper.updateIntermediateStatus(callback.getSubmitId(), callback.getResult()) != 1) {
            throw new IllegalStateException("Intermediate judge status was not persisted");
        }
        if (attempt != null && attemptMapper.updatePendingAttemptStatus(callback.getSubmitId(), callback.getResult()) != 1) {
            throw new IllegalStateException("Contest attempt status was not persisted");
        }
        return true;
    }

    /** Final callback application. Lock order is contest -> submit log -> attempt -> contest projection. */
    @Transactional(rollbackFor = Exception.class)
    public void applyFinalResult(JudgeScore callback) {
        if (callback == null || callback.getSubmitId() == null || callback.getSubmitId() <= 0) {
            throw new ApiStatusException(400, "判题结果参数无效");
        }
        normalizeAndValidateResult(callback);

        Long contestId = submitLogMapper.selectContestIdBySubmitId(callback.getSubmitId());
        if (contestId == null && submitLogMapper.selectById(callback.getSubmitId()) == null) {
            throw new ApiStatusException(404, "提交记录不存在");
        }
        lockContestIfNeeded(contestId);
        SubmitLog logRow = lockSubmitLog(callback.getSubmitId(), contestId);
        verifyFinalIdentity(callback, logRow);

        if (Boolean.TRUE.equals(logRow.getResultApplied())) {
            if (sameAppliedResult(callback, logRow)) {
                releaseAfterCommit(callback.getUserId(), callback.getSubmissionLockToken());
            } else {
                log.warn("判题重复回调内容不一致，保留已应用结果 submitId={} stored={} received={}",
                        callback.getSubmitId(), logRow.getStatus(), callback.getResult());
            }
            return;
        }

        // T17 intentionally left old terminal history unapplied: do not reinterpret it as a live callback.
        if (isTerminal(logRow.getStatus())) {
            log.warn("忽略命中历史终态的判题回调 submitId={} status={}", callback.getSubmitId(), logRow.getStatus());
            return;
        }
        if (!isIntermediateOrQueued(logRow.getStatus())) {
            throw new ApiStatusException(409, "提交状态已变化，无法应用判题结果");
        }

        ContestAttempt attempt = null;
        ContestRecords projection = null;
        if (contestId != null) {
            attempt = lockAttempt(callback.getSubmitId(), logRow);
            if (attempt.getState() != ContestAttemptState.PENDING) {
                throw new ApiStatusException(409, "比赛提交状态已变化");
            }
            projection = contestRecordsMapper.selectForUpdate(contestId, logRow.getUserId(), logRow.getProblemId());
            if (projection == null || projection.getRecordId() == null) {
                throw new IllegalStateException("Accepted contest submission has no score projection");
            }
        }

        LocalDateTime completedAt = LocalDateTime.now(clock);
        String userStderr = callback.getResult() == JudgeResult.JUDGE_ERROR ? null : callback.getErrorMessage();
        int updated = submitLogMapper.applyFinalResult(callback.getSubmitId(), callback.getResult(),
                callback.getRuntime(), callback.getMemory(), userStderr, callback.getInternalError(),
                callback.getErrorCode(), callback.getTotalCount(), callback.getPassCount(), callback.getScore(), completedAt);
        if (updated != 1) {
            throw new IllegalStateException("Final judge result was not atomically claimed");
        }

        insertCaseLogs(callback, logRow);

        if (attempt != null) {
            boolean infrastructureFailure = callback.getResult() == JudgeResult.JUDGE_ERROR;
            int changed = attemptMapper.applyPendingAttemptResult(callback.getSubmitId(),
                    infrastructureFailure ? ContestAttemptState.INFRA_ERROR : ContestAttemptState.COMPLETED,
                    callback.getResult(), infrastructureFailure ? null : callback.getScore(),
                    callback.getResult() == JudgeResult.ACCEPT, completedAt);
            if (changed != 1) {
                throw new IllegalStateException("Contest attempt result was not applied");
            }

            // Infrastructure failures terminate the receipt but never replace a valid score projection.
            if (!infrastructureFailure) {
                contestRecordsMapper.upsertGradedProjection(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(),
                        contestId, logRow.getUserId(), logRow.getProblemId(),
                        callback.getResult() == JudgeResult.ACCEPT, callback.getScore(),
                        attempt.getAttemptSeq(), attempt.getAttemptId());
            }
        } else if (callback.getResult() != JudgeResult.JUDGE_ERROR) {
            Records record = new Records()
                    .setContestId(null)
                    .setUserId(logRow.getUserId())
                    .setProblemId(logRow.getProblemId())
                    .setScore(callback.getScore())
                    .setStatus(callback.getResult() == JudgeResult.ACCEPT)
                    .setAnswer(new UserAnswer(null, logRow.getCode(), callback.getLanguageId()));
            recordsService.addRecord(record);
        }

        if (callback.getResult() == JudgeResult.ACCEPT && attempt != null) {
            completionAwardService.completeOnAc(logRow.getUserId(), logRow.getProblemId(), callback.getScore());
        }
        releaseAfterCommit(logRow.getUserId(), callback.getSubmissionLockToken());
    }

    /** Minimal, read-only internal contract; never returns source code or internal diagnostic fields. */
    @Transactional(readOnly = true)
    public JudgeSubmissionStatusVo getSubmissionStatus(Long submitId) {
        if (submitId == null || submitId <= 0) {
            throw new ApiStatusException(400, "提交编号无效");
        }
        SubmitLog statusRow = submitLogMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SubmitLog>()
                .select(SubmitLog::getResultApplied, SubmitLog::getStatus)
                .eq(SubmitLog::getSubmitId, submitId));
        if (statusRow == null) {
            throw new ApiStatusException(404, "提交记录不存在");
        }
        JudgeSubmissionStatusVo response = new JudgeSubmissionStatusVo();
        response.setResultApplied(Boolean.TRUE.equals(statusRow.getResultApplied()));
        response.setStatus(statusRow.getStatus());
        return response;
    }

    private void lockContestIfNeeded(Long contestId) {
        if (contestId == null) {
            return;
        }
        Contest contest = contestMapper.selectContestForUpdate(contestId);
        if (contest == null) {
            throw new ApiStatusException(404, "比赛不存在或提交记录无效");
        }
    }

    private SubmitLog lockSubmitLog(Long submitId, Long expectedContestId) {
        SubmitLog logRow = submitLogMapper.selectByIdForUpdate(submitId);
        if (logRow == null) {
            throw new ApiStatusException(404, "提交记录不存在");
        }
        if (!Objects.equals(expectedContestId, logRow.getContestId())) {
            throw new ApiStatusException(409, "提交所属比赛已变化");
        }
        return logRow;
    }

    private ContestAttempt lockAttempt(Long submitId, SubmitLog logRow) {
        ContestAttempt attempt = attemptMapper.selectBySubmitIdForUpdate(submitId);
        if (attempt == null
                || !Objects.equals(attempt.getUserId(), logRow.getUserId())
                || !Objects.equals(attempt.getProblemId(), logRow.getProblemId())
                || !Objects.equals(attempt.getContestId(), logRow.getContestId())) {
            throw new ApiStatusException(409, "比赛提交关联信息不一致");
        }
        return attempt;
    }

    private void verifyFinalIdentity(JudgeScore callback, SubmitLog logRow) {
        if (!Objects.equals(callback.getUserId(), logRow.getUserId())
                || !Objects.equals(callback.getProblemId(), logRow.getProblemId())
                || !Objects.equals(callback.getContestId(), logRow.getContestId())) {
            throw new ApiStatusException(403, "判题回调身份与提交记录不匹配");
        }
    }

    private void verifyOptionalIdentity(JudgeScore callback, SubmitLog logRow) {
        if ((callback.getUserId() != null && !Objects.equals(callback.getUserId(), logRow.getUserId()))
                || (callback.getProblemId() != null && !Objects.equals(callback.getProblemId(), logRow.getProblemId()))
                || (callback.getContestId() != null && !Objects.equals(callback.getContestId(), logRow.getContestId()))
                || callback.getUserId() == null) {
            throw new ApiStatusException(403, "判题状态身份与提交记录不匹配");
        }
    }

    private void normalizeAndValidateResult(JudgeScore callback) {
        if (callback.getResult() == null) {
            callback.setResult(JudgeResult.JUDGE_ERROR)
                    .setErrorCode(defaultText(callback.getErrorCode(), "MISSING_FINAL_STATUS"))
                    .setInternalError(defaultText(callback.getInternalError(), "Final callback contained no judge status"));
        }
        if (callback.getResult() == JudgeResult.QUEUE || callback.getResult() == JudgeResult.COMPILING
                || callback.getResult() == JudgeResult.RUNNING) {
            throw new ApiStatusException(400, "最终判题结果不能是中间状态");
        }
        BigDecimal score = callback.getScore() == null ? BigDecimal.ZERO : callback.getScore();
        if (score.signum() < 0 || nonNegative(callback.getRuntime()) == false
                || nonNegative(callback.getMemory()) == false) {
            throw new ApiStatusException(400, "判题结果数值无效");
        }
        callback.setScore(score.setScale(2, RoundingMode.HALF_DOWN));

        int total = callback.getTotalCount() == null ? 0 : callback.getTotalCount();
        int passed = callback.getPassCount() == null ? 0 : callback.getPassCount();
        List<JudgeCaseResult> cases = callback.getCaseResults() == null
                ? Collections.emptyList() : callback.getCaseResults();
        if (total < 0 || passed < 0 || passed > total || cases.size() > total) {
            throw new ApiStatusException(400, "判题测试用例数量无效");
        }
        Set<Integer> indices = new HashSet<>();
        int acceptedCases = 0;
        for (JudgeCaseResult item : cases) {
            if (item == null || item.getCaseIndex() == null || item.getCaseIndex() < 0
                    || item.getCaseIndex() >= total || item.getCaseId() == null || item.getCaseId() <= 0
                    || item.getStatus() == null || !indices.add(item.getCaseIndex())
                    || item.getScore() != null && item.getScore().signum() < 0
                    || !nonNegative(item.getTime()) || !nonNegative(item.getMemory())) {
                throw new ApiStatusException(400, "判题测试用例结果无效");
            }
            if (item.getStatus() == JudgeResult.ACCEPT) {
                acceptedCases++;
            }
        }
        if (acceptedCases != passed) {
            throw new ApiStatusException(400, "判题通过用例数与用例结果不一致");
        }
        if (callback.getResult() == JudgeResult.ACCEPT) {
            if (total <= 0 || cases.size() != total || passed != total
                    || cases.stream().anyMatch(item -> item.getStatus() != JudgeResult.ACCEPT)) {
                throw new ApiStatusException(400, "AC 结果缺少完整通过的测试用例记录");
            }
        } else if (callback.getResult() != JudgeResult.COMPILE_ERROR
                && callback.getResult() != JudgeResult.JUDGE_ERROR
                && (total <= 0 || cases.size() != total)) {
            throw new ApiStatusException(400, "判题结果缺少完整测试用例记录");
        }
        if (callback.getResult() == JudgeResult.JUDGE_ERROR) {
            callback.setScore(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY))
                    .setErrorCode(defaultText(callback.getErrorCode(), "JUDGE_ERROR"))
                    .setInternalError(defaultText(callback.getInternalError(), "Judge infrastructure failed"));
        }
        callback.setTotalCount(total).setPassCount(passed).setCaseResults(new ArrayList<>(cases));
    }

    private boolean sameAppliedResult(JudgeScore callback, SubmitLog existing) {
        if (callback.getResult() != existing.getStatus()
                || !sameDecimal(callback.getScore(), existing.getScore())
                || !Objects.equals(callback.getRuntime(), existing.getTime())
                || !Objects.equals(callback.getMemory(), existing.getMemory())
                || !Objects.equals(callback.getTotalCount(), existing.getTotalCount())
                || !Objects.equals(callback.getPassCount(), existing.getPassCount())
                || !Objects.equals(callback.getErrorCode(), existing.getErrorCode())
                || !Objects.equals(callback.getInternalError(), existing.getInternalError())
                || !Objects.equals(callback.getResult() == JudgeResult.JUDGE_ERROR ? null : callback.getErrorMessage(), existing.getStderr())) {
            return false;
        }
        // A regular SELECT would reuse the snapshot opened by the initial submit/contest read and could miss
        // case rows committed by the callback that set result_applied=true.
        List<JudgeCaseLog> stored = caseLogMapper.selectBySubmitIdForUpdate(callback.getSubmitId());
        List<JudgeCaseResult> received = callback.getCaseResults();
        if (stored == null || stored.size() != received.size()) {
            return false;
        }
        for (int i = 0; i < stored.size(); i++) {
            JudgeCaseLog oldCase = stored.get(i);
            JudgeCaseResult newCase = received.get(i);
            if (!Objects.equals(oldCase.getCaseId(), newCase.getCaseId())
                    || !Objects.equals(oldCase.getCaseIndex(), newCase.getCaseIndex())
                    || oldCase.getStatus() != newCase.getStatus()
                    || !sameDecimal(oldCase.getScore(), newCase.getScore())
                    || !Objects.equals(oldCase.getTime(), newCase.getTime())
                    || !Objects.equals(oldCase.getMemory(), newCase.getMemory())
                    || !Objects.equals(oldCase.getInternalError(), newCase.getErrorMessage())) {
                return false;
            }
        }
        return true;
    }

    private void insertCaseLogs(JudgeScore callback, SubmitLog logRow) {
        for (JudgeCaseResult result : callback.getCaseResults()) {
            JudgeCaseLog item = new JudgeCaseLog()
                    .setSubmitId(callback.getSubmitId())
                    .setProblemId(logRow.getProblemId())
                    .setCaseId(result.getCaseId())
                    .setCaseIndex(result.getCaseIndex())
                    .setStatus(result.getStatus())
                    .setScore(result.getScore())
                    .setTime(result.getTime())
                    .setMemory(result.getMemory())
                    .setInternalError(result.getErrorMessage());
            if (caseLogMapper.insert(item) != 1) {
                throw new IllegalStateException("Judge case result was not persisted");
            }
        }
    }

    private void releaseAfterCommit(Long userId, String token) {
        if (userId == null || token == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            submissionLock.release(userId, token);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                submissionLock.release(userId, token);
            }
        });
    }

    private boolean canTransition(JudgeResult current, JudgeResult next) {
        if (current == null) {
            return false;
        }
        if (current == next) {
            return true;
        }
        return current == JudgeResult.QUEUE && next == JudgeResult.COMPILING
                || current == JudgeResult.COMPILING && next == JudgeResult.RUNNING;
    }

    private boolean isIntermediateOrQueued(JudgeResult status) {
        return status == JudgeResult.QUEUE || status == JudgeResult.COMPILING || status == JudgeResult.RUNNING;
    }

    private boolean isTerminal(JudgeResult status) {
        return status != null && !isIntermediateOrQueued(status);
    }

    private boolean nonNegative(Long value) {
        return value == null || value >= 0;
    }

    private boolean sameDecimal(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
