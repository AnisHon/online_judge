package com.anishan.judge.judge.impl;

import cn.hutool.core.collection.CollUtil;
import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.judge.config.LanguageConfigLoader;
import com.anishan.judge.domain.*;
import com.anishan.judge.domain.entity.LanguageConfig;
import com.anishan.judge.exception.CompileError;
import com.anishan.judge.exception.SubmitError;
import com.anishan.judge.exception.SystemError;
import com.anishan.judge.judge.*;
import com.anishan.judge.util.JudgeNotifyUtil;
import com.anishan.judge.judge.Compiler;
import com.anishan.judge.util.JudgeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DefaultJudgeImpl implements JudgeRun {


    private final static ExecutorService executorService;
    private static final int cpuNum = Runtime.getRuntime().availableProcessors();
    static {
        executorService = new ThreadPoolExecutor(
                cpuNum, // 核心线程数
                cpuNum, // 最大线程数。最多几个线程并发。
                3,//当非核心线程无任务时，几秒后结束该线程
                TimeUnit.SECONDS,// 结束线程时间单位
                new LinkedBlockingDeque<>(200 * cpuNum), //阻塞队列，限制等候线程数
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.DiscardOldestPolicy());//队列满了，尝试去和最早的竞争，也不会抛出异常！
    }


    private final Judge judge;

    private final BuildJudgeCaseImpl buildJudgeCase;

    private final GradeSubmission gradeSubmission;

    private final Compiler compiler;

    private final LanguageConfigLoader configLoader;

    private final SandboxRun sandboxRun;

    private final JudgeNotifyUtil judgeNotifyUtil;


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
        try {
            runResult = runJudge(caseContent, judgeInfo, judgeParam);
        } catch (SystemError e) {
            log.error("严重错误，判题机出错", e);
            return null;
        }

        return gradeSubmission.judgeScore(runResult, caseContent);
    }


    /**
     * 最终分数计算
     * @param futureTasks 判题任务列表
     * @param judgeScore 输出参数JudgeScore
     */
    public void judgeFinalScore(List<CompletableFuture<JudgeRunResultScore>> futureTasks, JudgeScore judgeScore) throws ExecutionException, InterruptedException {
        if (CollUtil.isEmpty(futureTasks)) {
            return;
        }

        judgeScore.setResult(JudgeResult.Accept);

        BigDecimal totalScore = BigDecimal.ZERO;
        long runtime = 0;
        long memory = 0;

        for (CompletableFuture<JudgeRunResultScore> futureTask : futureTasks) {
            JudgeRunResultScore judgeRunResultScore = futureTask.get();

            if (judgeRunResultScore == null) {
                return;
            }

            // 算最大运行时间
            if (judgeRunResultScore.getRuntime() > runtime) {
                runtime = judgeRunResultScore.getRuntime();
            }

            // 最大使用内存
            if (judgeRunResultScore.getMemory() > memory) {
                memory = judgeRunResultScore.getMemory();
            }

            // 算总分
            totalScore = totalScore.add(judgeRunResultScore.getScore());

            // 设置结果 报错信息
            if (judgeRunResultScore.getJudgeResult() != JudgeResult.Accept) {
                judgeScore.setResult(judgeRunResultScore.getJudgeResult());
                judgeScore.setErrorMessage(judgeRunResultScore.getErrorMessage());
            }
        }

        judgeScore.setScore(totalScore);   // 设置分数
        judgeScore.setRuntime(runtime);    // 设置最大运行时间
        judgeScore.setMemory(memory);      // 设置最大内存
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
                .setProblemId(judgeInfo.getProblemId())
                .setUserId(judgeInfo.getUserId())
                .setContestId(judgeInfo.getContestId())
                .setCode(judgeInfo.getCode())
                .setLanguageId(judgeInfo.getLanguageId())
                .setScore(BigDecimal.ZERO);

        String language = judgeInfo.getLanguage();

        // 获取语言配置
        LanguageConfig languageConfig = configLoader.getLanguageConfigByName(language);

        String fileId = null;

        try {
            // 通知编译
            judgeNotifyUtil.notifyCompiling(judgeInfo.getUuid());
            fileId = compiler.compile(languageConfig, judgeInfo.getCode(), null, null);

            JudgeCases judgeCases = buildJudgeCase.buildJudgeCases(judgeInfo.getProblemId());

            ArrayList<CompletableFuture<JudgeRunResultScore>> futureTasks = new ArrayList<>();

            JudgeParam judgeParam = JudgeParam
                    .builder()
                    .languageConfig(languageConfig)
                    .fileId(fileId)
                    .content(null)
                    .test(false)
                    .build();

            // 通知正在判题
            judgeNotifyUtil.notifyRunning(judgeInfo.getUuid());
            judgeCases.getCaseContents().forEach(caseContent -> {
                CompletableFuture<JudgeRunResultScore> future =
                        CompletableFuture.supplyAsync(() -> judge(caseContent, judgeInfo, judgeParam), executorService);
                futureTasks.add(future);
            });

            // 统计最后得分
            judgeFinalScore(futureTasks, judgeScore);
        } catch (SystemError e) {
            judgeScore
                    .setResult(JudgeResult.CompileError)
                    .setErrorMessage("SandBox failed to compile, please contact administrator");
            log.error("判题机异常当前参数{}", judgeInfo, e);
        } catch (CompileError e) {
            judgeScore
                    .setResult(JudgeResult.CompileError)
                    .setErrorMessage(e.getStderr());
        } catch (SubmitError e) {
            judgeScore
                    .setResult(JudgeResult.CompileError)
                    .setErrorMessage(e.getStderr());
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
            judgeScore.setResult(JudgeResult.RuntimeError);
        } finally {
            if (fileId != null) {
                sandboxRun.delFile(fileId);
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
                .setMemoryLimit(languageConfig.getMaxMemory())
                .setTimeLimit(languageConfig.getMaxRealTime() * 1000)
                .setStackLimit(128);

        String fileId = null;

        try {
            // 通知编译
            judgeNotifyUtil.notifyCompiling(runTestInfo.getUuid());
            fileId = compiler.compile(languageConfig, runTestInfo.getCode(), null, null);


            JudgeParam judgeParam = JudgeParam
                    .builder()
                    .languageConfig(languageConfig)
                    .fileId(fileId)
                    .content(runTestInfo.getStdin())
                    .test(true)
                    .build();

            // 通知正在运行
            judgeNotifyUtil.notifyRunning(runTestInfo.getUuid());
            CompletableFuture<RunResult> future =
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            return runJudge(null, judgeInfo, judgeParam);
                        } catch (SystemError e) {
                            throw new RuntimeException(e);
                        }
                    }, executorService);

            RunResult runResult = future.get();

            testResult.setJudgeResult(JudgeUtils.judgeToStatus(runResult.getStatus()));

            if (runResult.getExitStatus() > 0 && runResult.getExitStatus() <= 31) {
                testResult.setStderr(SandboxRunImpl.signals.get(runResult.getExitStatus()));
            }
            testResult.setStdout(runResult.getFiles().getStdout());
        } catch (SystemError e) {
            log.error("判题机异常当前参数{}", judgeInfo, e);
        } catch (CompileError e) {
            testResult.setJudgeResult(JudgeResult.CompileError);
            testResult.setStderr(e.getStderr());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            testResult.setJudgeResult(JudgeResult.RuntimeError);
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
