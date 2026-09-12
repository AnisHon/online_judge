package com.anishan.judge.judge.impl;

import cn.hutool.core.collection.CollUtil;
import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeCaseResult;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;
import com.anishan.api.client.problem.client.ProblemInternalClient;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.judge.config.LanguageConfigLoader;
import com.anishan.judge.config.JudgeConfig;
import com.anishan.judge.domain.*;
import com.anishan.judge.domain.entity.LanguageConfig;
import com.anishan.judge.exception.CompileError;
import com.anishan.judge.exception.SubmitError;
import com.anishan.judge.exception.SystemError;
import com.anishan.judge.judge.*;
import com.anishan.judge.judge.Compiler;
import com.anishan.judge.util.JudgeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DefaultJudgeImpl implements JudgeRun {

    private final Judge judge;

    private final BuildJudgeCaseImpl buildJudgeCase;

    private final GradeSubmission gradeSubmission;

    private final Compiler compiler;

    private final LanguageConfigLoader configLoader;

    private final SandboxRun sandboxRun;

    private final ProblemInternalClient problemInternalClient;
    private final JudgeConfig judgeConfig;

    private ExecutorService executorService;
    private Semaphore caseSemaphore;
    private Semaphore submissionSemaphore;

    @PostConstruct
    public void initExecutor() {
        int maxConcurrency = Math.max(1, judgeConfig.getMaxConcurrency());
        int queueCapacity = Math.max(maxConcurrency, judgeConfig.getQueueCapacity());
        executorService = new ThreadPoolExecutor(
                maxConcurrency,
                maxConcurrency,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>(queueCapacity),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        caseSemaphore = new Semaphore(maxConcurrency);
        submissionSemaphore = new Semaphore(Math.max(1, judgeConfig.getMaxSubmissions()));
        log.info("判题并发限制已启用: submissions={}, cases={}, queue={}",
                judgeConfig.getMaxSubmissions(), maxConcurrency, queueCapacity);
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }


    private RunResult runJudge(CaseContent caseContent, JudgeInfo judgeInfo, JudgeParam judgeParam) throws SystemError {
        JudgeContent.JudgeContentBuilder builder = JudgeContent
                .builder()
                .languageConfig(judgeParam.getLanguageConfig())
                .testCaseContent(judgeParam.getContent())
                .maxTime(judgeInfo.getTimeLimit() + 200L)      // 题目有200ms的冗余
                .maxMemory(judgeInfo.getMemoryLimit())
                .maxStack(judgeInfo.getStackLimit())
                .fileId(judgeParam.getFileId())
                .fileIO(false);

        if (!judgeParam.isTest()) {
            builder
                    .testCasePath(caseContent.getInPath())
                    .maxOutputSize(caseContent.getOutSize());
        } else {
            builder.maxOutputSize(1024 * 32L);
        }
        return judge.doJudge(builder.build());
    }


    /**
     * 单个测试用例 SandBox判题 + 判分
     * @param caseContent 当前case
     * @param judgeInfo 判题信息
     * @param judgeParam 通用判题参数
     * @return 当前测试用例的分数 错误信息 状态信息
     */
    private JudgeRunResultScore judge(CaseContent caseContent, JudgeInfo judgeInfo, JudgeParam judgeParam) {

        RunResult runResult;
        boolean acquired = false;
        try {
            caseSemaphore.acquire();
            acquired = true;
            runResult = runJudge(caseContent, judgeInfo, judgeParam);
        } catch (SystemError e) {
            log.error("严重错误，判题机出错 stderr:{}", e.getStderr());
            log.error("严重错误，判题机出错 stdout:{}", e.getStdout());
            log.error("严重错误，判题机出错 message:{}", e.getMessage());
            return JudgeRunResultScore.builder()
                    .caseId(caseContent.getCaseId())
                    .score(BigDecimal.ZERO)
                    .judgeResult(JudgeResult.JUDGE_ERROR)
                    .passed(false)
                    .runtime(0L)
                    .memory(0L)
                    .internalError("sandbox error: " + e.getMessage())
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return JudgeRunResultScore.builder()
                    .caseId(caseContent.getCaseId())
                    .score(BigDecimal.ZERO)
                    .judgeResult(JudgeResult.JUDGE_ERROR)
                    .passed(false)
                    .runtime(0L)
                    .memory(0L)
                    .internalError("case execution interrupted")
                    .build();
        } finally {
            if (acquired) {
                caseSemaphore.release();
            }
        }

        return gradeSubmission.judgeScore(runResult, caseContent);
    }


    /**
     * 最终分数计算
     *
     * @param futureTasks 判题任务列表
     * @param judgeScore  输出参数JudgeScore
     */
    public void judgeFinalScore(List<CompletableFuture<JudgeRunResultScore>> futureTasks,
                                List<CaseContent> caseContents,
                                JudgeScore judgeScore,
                                BigDecimal totalScore,
                                JudgeInfo judgeInfo) throws InterruptedException {
        if (CollUtil.isEmpty(futureTasks)) {
            judgeScore
                    .setResult(JudgeResult.JUDGE_ERROR)
                    .setErrorCode("NO_TEST_CASE")
                    .setInternalError("no active test case was found for problem " + judgeInfo.getProblemId())
                    .setTotalCount(0)
                    .setPassCount(0)
                    .setCaseResults(new ArrayList<>());
            return;
        }

        judgeScore.setResult(JudgeResult.ACCEPT);

        BigDecimal score = BigDecimal.ZERO;
        long runtime = 0;
        long memory = 0;
        int passCount = 0; // 通过数量
        List<JudgeCaseResult> caseResults = new ArrayList<>();

        for (int index = 0; index < futureTasks.size(); index++) {
            CompletableFuture<JudgeRunResultScore> futureTask = futureTasks.get(index);
            JudgeRunResultScore judgeRunResultScore;
            try {
                judgeRunResultScore = futureTask.get(2, TimeUnit.MINUTES);
            } catch (ExecutionException | TimeoutException e) {
                judgeRunResultScore = JudgeRunResultScore.builder()
                        .caseId(caseContents.get(index).getCaseId())
                        .judgeResult(JudgeResult.JUDGE_ERROR)
                        .score(BigDecimal.ZERO)
                        .passed(false)
                        .runtime(0L)
                        .memory(0L)
                        .internalError("case future failed: " + e.getMessage())
                        .build();
            }

            if (judgeRunResultScore == null) {
                judgeRunResultScore = JudgeRunResultScore.builder()
                        .caseId(caseContents.get(index).getCaseId())
                        .judgeResult(JudgeResult.JUDGE_ERROR)
                        .score(BigDecimal.ZERO)
                        .passed(false)
                        .runtime(0L)
                        .memory(0L)
                        .internalError("case returned no result")
                        .build();
            }

            // 算最大运行时间
            long caseRuntime = Optional.ofNullable(judgeRunResultScore.getRuntime()).orElse(0L);
            long caseMemory = Optional.ofNullable(judgeRunResultScore.getMemory()).orElse(0L);
            if (caseRuntime > runtime) {
                runtime = caseRuntime;
            }

            // 最大使用内存
            if (caseMemory > memory) {
                memory = caseMemory;
            }

            // 算总分
            score = score.add(Optional.ofNullable(judgeRunResultScore.getScore()).orElse(BigDecimal.ZERO));

            caseResults.add(new JudgeCaseResult()
                    .setCaseId(judgeRunResultScore.getCaseId())
                    .setCaseIndex(index)
                    .setStatus(Optional.ofNullable(judgeRunResultScore.getJudgeResult()).orElse(JudgeResult.JUDGE_ERROR))
                    .setScore(Optional.ofNullable(judgeRunResultScore.getScore()).orElse(BigDecimal.ZERO))
                    .setTime(caseRuntime)
                    .setMemory(caseMemory)
                    .setErrorMessage(Optional.ofNullable(judgeRunResultScore.getInternalError())
                            .orElse(judgeRunResultScore.getErrorMessage())));

            // 设置结果 报错信息
            JudgeResult caseResult = Optional.ofNullable(judgeRunResultScore.getJudgeResult())
                    .orElse(JudgeResult.JUDGE_ERROR);
            if (caseResult != JudgeResult.ACCEPT) {
                if (judgeScore.getResult() == JudgeResult.ACCEPT
                        || caseResult == JudgeResult.JUDGE_ERROR) {
                    judgeScore.setResult(caseResult);
                }
                if (caseResult == JudgeResult.JUDGE_ERROR) {
                    judgeScore
                            .setErrorCode("CASE_EXECUTION_FAILED")
                            .setInternalError(judgeRunResultScore.getInternalError());
                } else if (judgeScore.getErrorMessage() == null) {
                    judgeScore.setErrorMessage(judgeRunResultScore.getErrorMessage());
                }
            } else  {
                passCount++;
            }
        }

        if (judgeInfo.getContestId() != null && judgeInfo.getListScore() != null) {
            if (BigDecimal.ZERO.equals(totalScore)) {
                score = BigDecimal.ZERO;
            } else {
                BigDecimal temp = score.divide(totalScore, 8, RoundingMode.DOWN);
                BigDecimal ttt = judgeInfo.getListScore();
                score =  temp.multiply(ttt);
            }
        }


        judgeScore.setScore(score);   // 设置分数
        judgeScore.setRuntime(runtime);    // 设置最大运行时间
        judgeScore.setMemory(memory);      // 设置最大内存
        judgeScore.setPassCount(passCount); // 通过数量
        judgeScore.setTotalCount(futureTasks.size());
        judgeScore.setCaseResults(caseResults);
    }

    /**
     * 用线程池控制并发量的判所有测试用例，直接被MQ调用
     *
     * @param judgeInfo MQ判题参数
     * @return 判题结果，分数，状态，错误信息
     */
    @Override
    public JudgeScore judgeAll(JudgeInfo judgeInfo) {

        // 初始参数
        JudgeScore judgeScore = new JudgeScore()
                .setSubmitId(judgeInfo.getSubmitId())
                .setProblemId(judgeInfo.getProblemId())
                .setUserId(judgeInfo.getUserId())
                .setContestId(judgeInfo.getContestId())
                .setCode(judgeInfo.getCode())
                .setLanguageId(judgeInfo.getLanguageId())
                .setScore(BigDecimal.ZERO);

        String language = judgeInfo.getLanguage();

        boolean submissionAcquired = false;
        try {
            submissionSemaphore.acquire();
            submissionAcquired = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return judgeScore
                    .setResult(JudgeResult.JUDGE_ERROR)
                    .setErrorCode("SUBMISSION_INTERRUPTED")
                    .setInternalError("submission permit interrupted");
        }

        // 获取语言配置
        LanguageConfig languageConfig = configLoader.getLanguageConfigByName(language);
        if (languageConfig == null) {
            submissionSemaphore.release();
            return judgeScore
                    .setResult(JudgeResult.JUDGE_ERROR)
                    .setErrorCode("UNSUPPORTED_LANGUAGE")
                    .setInternalError("language config not found: " + language);
        }

        String fileId = null;

        try {
            // 先读取测试用例并锁定总数，编译失败时公开日志也能知道本次判题的规模。
            JudgeCases judgeCases = buildJudgeCase.buildJudgeCases(judgeInfo.getProblemId());
            List<CaseContent> caseContents = judgeCases == null
                    ? new ArrayList<>()
                    : Optional.ofNullable(judgeCases.getCaseContents()).orElse(new ArrayList<>());
            judgeScore.setTotalCount(caseContents.size());

            if (caseContents.isEmpty()) {
                judgeScore
                        .setResult(JudgeResult.JUDGE_ERROR)
                        .setErrorCode("NO_TEST_CASE")
                        .setInternalError("no active test case was found for problem " + judgeInfo.getProblemId())
                        .setPassCount(0)
                        .setCaseResults(new ArrayList<>());
                return judgeScore;
            }

            fileId = compiler.compile(languageConfig, judgeInfo.getCode(), null, null);

            try {
                problemInternalClient.judgeStatus(new JudgeScore()
                        .setSubmitId(judgeInfo.getSubmitId())
                        .setUserId(judgeInfo.getUserId())
                        .setResult(JudgeResult.RUNNING));
            } catch (Exception e) {
                log.warn("更新提交运行状态失败，继续执行判题: submitId={}", judgeInfo.getSubmitId(), e);
            }

            ArrayList<CompletableFuture<JudgeRunResultScore>> futureTasks = new ArrayList<>();

            JudgeParam judgeParam = JudgeParam
                    .builder()
                    .languageConfig(languageConfig)
                    .fileId(fileId)
                    .content(null)
                    .test(false)
                    .build();

            caseContents.forEach(caseContent -> {
                CompletableFuture<JudgeRunResultScore> future =
                        CompletableFuture.supplyAsync(() -> judge(caseContent, judgeInfo, judgeParam), executorService);
                futureTasks.add(future);
            });

            // 统计最后得分
            BigDecimal totalScore = judgeCases == null || judgeCases.getTotalScore() == null
                    ? BigDecimal.ZERO : judgeCases.getTotalScore();
            judgeFinalScore(futureTasks, caseContents, judgeScore, totalScore, judgeInfo);
        } catch (SystemError e) {
            judgeScore
                    .setResult(JudgeResult.JUDGE_ERROR)
                    .setErrorCode("SANDBOX_ERROR")
                    .setInternalError(e.getMessage());
            log.error("判题机异常当前参数{}", judgeInfo, e);
        } catch (CompileError e) {
            judgeScore
                    .setResult(JudgeResult.COMPILE_ERROR)
                    .setErrorMessage(e.getStderr());
        } catch (SubmitError e) {
            judgeScore
                    .setResult(JudgeResult.JUDGE_ERROR)
                    .setErrorCode("SANDBOX_SUBMIT_ERROR")
                    .setInternalError(e.getMessage() + " " + e.getStderr());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(e.getMessage(), e);
            judgeScore
                    .setResult(JudgeResult.JUDGE_ERROR)
                    .setErrorCode("JUDGE_INTERRUPTED")
                    .setInternalError(e.getMessage());
        } finally {
            if (fileId != null) {
                sandboxRun.delFile(fileId);
            }
            if (submissionAcquired) {
                submissionSemaphore.release();
            }
        }


        return judgeScore;
    }


    private TestResult test(RunTestInfo runTestInfo) {

        TestResult testResult = new TestResult()
                .setUserId(runTestInfo.getUserId());
        String language = runTestInfo.getLanguage();



        // 获取语言配置
        LanguageConfig languageConfig = configLoader.getLanguageConfigByName(language);

        JudgeInfo judgeInfo = new JudgeInfo()
                .setMemoryLimit(languageConfig.getMaxMemory() / 1024)
                .setTimeLimit(languageConfig.getMaxRealTime())
                .setStackLimit(128);

        String fileId = null;

        try {
            fileId = compiler.compile(languageConfig, runTestInfo.getCode(), null, null);


            JudgeParam judgeParam = JudgeParam
                    .builder()
                    .languageConfig(languageConfig)
                    .fileId(fileId)
                    .content(runTestInfo.getStdin())
                    .test(true)
                    .build();

            CompletableFuture<RunResult> future =
                    CompletableFuture.supplyAsync(() -> {
                        boolean acquired = false;
                        try {
                            caseSemaphore.acquire();
                            acquired = true;
                            return runJudge(null, judgeInfo, judgeParam);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("test execution interrupted", e);
                        } catch (SystemError e) {
                            throw new RuntimeException(e);
                        } finally {
                            if (acquired) {
                                caseSemaphore.release();
                            }
                        }
                    }, executorService);

            RunResult runResult = future.get();

            testResult.setJudgeResult(JudgeUtils.judgeToStatus(runResult.getStatus()));
            if (runResult.getExitStatus() > 0 && runResult.getExitStatus() <= 31) {
                testResult.setStderr(SandboxRunImpl.signals.get(runResult.getExitStatus()));
            }
            RunResult.StdIoFile files = runResult.getFiles();
            if (testResult.getJudgeResult() != JudgeResult.ACCEPT) {
                files.setStdout(null);
                files.setStderr(files.getStderr());
            }
            testResult.setStdout(files.getStdout());
        } catch (SystemError e) {
            log.error("判题机异常当前参数{}", judgeInfo, e);
        } catch (CompileError e) {
            testResult.setJudgeResult(JudgeResult.COMPILE_ERROR);
            testResult.setStderr(e.getStderr());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            testResult.setJudgeResult(JudgeResult.RUNTIME_ERROR);
        }finally {
            if (fileId != null) {
                sandboxRun.delFile(fileId);
            }
        }


        return testResult;
    }
    /**
     * 测试题目
     * @param runTestInfo 测试题目参数
     * @return 标准输出
     */
    @Override
    public TestResult judgeTest(RunTestInfo runTestInfo) {
        return test(runTestInfo);
    }

}
