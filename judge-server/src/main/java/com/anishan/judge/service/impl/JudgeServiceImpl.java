package com.anishan.judge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;
import com.anishan.api.client.problem.domain.vo.OjProblemCaseVo;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.judge.config.LanguageConfigLoader;
import com.anishan.judge.domain.entity.LanguageConfig;
import com.anishan.judge.exception.CompileError;
import com.anishan.judge.exception.SubmitError;
import com.anishan.judge.exception.SystemError;
import com.anishan.judge.judge.Compiler;
import com.anishan.judge.judge.Judge;
import com.anishan.judge.judge.SandboxRun;
import com.anishan.judge.service.JudgeService;
import com.anishan.judge.util.Constants;
import com.anishan.judge.util.JudgeDelayUtil;
import com.anishan.judge.util.JudgeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JudgeServiceImpl implements JudgeService {

    private final Judge judge;
    private final Compiler compiler;
    private final LanguageConfigLoader languageConfigLoader;
    private final SandboxRun sandboxRun;
    private final RabbitTemplate rabbitTemplate;

    private LanguageConfig getLanguageConfig(JudgeMessage message) {
        return languageConfigLoader.getLanguageConfigByName(message.getLanguage());
    }

    private JudgeMessage makeJudgeMessage(JudgeInfo judgeInfo) {
        List<OjProblemCaseVo> caseVos = null;

        BigDecimal score = BigDecimal.ZERO;
        for (OjProblemCaseVo caseVo : caseVos) {
            score = score.add(caseVo.getScore());
        }


        JudgeMessage judgeMessage = new JudgeMessage();
        judgeMessage.setUserId(judgeInfo.getUserId());
        judgeMessage.setContestId(judgeInfo.getContestId());
        judgeMessage.setLanguageId(judgeInfo.getLanguageId());
        judgeMessage.setCode(judgeInfo.getCode());
        judgeMessage.setLanguage(judgeInfo.getLanguage());
        judgeMessage.setTimeLimit(judgeInfo.getTimeLimit());
        judgeMessage.setMemoryLimit(judgeInfo.getMemoryLimit());
        judgeMessage.setStackLimit(judgeInfo.getStackLimit());
        judgeMessage.setCases(caseVos);
        judgeMessage.setProblemId(judgeInfo.getProblemId());
        judgeMessage.setScore(score);
        return judgeMessage;
    }

    @Override
    public JudgeScore judge(JudgeInfo info){
        JudgeMessage judgeMessage = makeJudgeMessage(info);

        JudgeScore result = null;

        try {
            result = judge(judgeMessage);
        } catch (SystemError | SubmitError e) {
            log.error(e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("非法语言：{}", e.getMessage());
        }

        return result;
    }


    @Override
    public String compile(LanguageConfig config, JudgeMessage message) throws CompileError, SystemError, SubmitError {
//        return compiler.compile(config, message.getCode(), config.getLanguage(), null);
        return compile(config, message.getCode());
    }

    @Override
    public String compile(LanguageConfig config, String code) throws CompileError, SystemError, SubmitError {
        return compiler.compile(config, code, config.getLanguage(), null);
    }

    @Override
    public List<RunResult> runCases(LanguageConfig config, String fileId, JudgeMessage message, List<String> cases) throws SystemError {

        return judge.judgeAll(fileId, config, message.getMemoryLimit(), message.getTimeLimit(), message.getStackLimit(), cases);
    }


    @Override
    public void deleteFile(String fileId) {
        sandboxRun.delFile(fileId);
    }

    private JudgeScore judgeCases(JudgeScore judgeScore, List<OjProblemCaseVo> cases, List<RunResult> results) {
        Optional<RunResult> maxTime = results.stream().max((a, b) -> Math.toIntExact(a.getTime() - b.getTime()));
        Optional<RunResult> maxMemory = results.stream().max((a, b) -> Math.toIntExact(a.getMemory() - b.getMemory()));
        // 默认时间取最大值
        judgeScore
                .runtimeSetter(maxTime.get())
                .memorySetter(maxMemory.get());
        if (cases.size() != results.size()) {
            return judgeScore.setResult(JudgeResult.RUNTIME_ERROR);
        }

        BigDecimal score = BigDecimal.ZERO;
        judgeScore.setResult(JudgeResult.ACCEPT);


        // 对比所有的Answer然后判分
        for (int i = 0; i < cases.size(); i++) {

            OjProblemCaseVo answer = cases.get(i);
            RunResult userAnswer = results.get(i);

            boolean isAccepted = !Objects.equals(userAnswer.getStatus(), Constants.Judge.STATUS_ACCEPTED.getStatus()) && judgeScore.getResult() != JudgeResult.WRONG_ANSWER;
            if (isAccepted) {
                judgeScore.setResult(JudgeUtils.judgeToStatus(userAnswer.getStatus()));
                judgeScore.setErrorMessage(userAnswer.getFiles().getStderr());
                continue;
            }

            String stdout = StrUtil.strip(userAnswer.getFiles().getStdout(), "\n");
            String answerOutput = StrUtil.strip(answer.getOutput(), "\n");
            if (!StrUtil.equals(answerOutput, stdout)) {
                judgeScore.setResult(JudgeResult.WRONG_ANSWER);
            } else  {
                score = score.add(Objects.requireNonNullElse(answer.getScore(), BigDecimal.ZERO));
            }
        }


        return judgeScore.setScore(score);
    }

    @Override
    public JudgeScore judge(JudgeMessage message) throws SystemError, SubmitError {

        List<OjProblemCaseVo> cases = message.getCases();
        List<String> inputCases = cases.stream().map(OjProblemCaseVo::getInput).collect(Collectors.toList());
        LanguageConfig languageConfig = getLanguageConfig(message);
        if (languageConfig == null) {
            throw new RuntimeException(message.getLanguage());
        }
        JudgeScore judgeScore = new JudgeScore()
                .setUserId(message.getUserId())
                .setProblemId(message.getProblemId())
                .setContestId(message.getContestId());
        List<RunResult> runResults;
        // 编译
        String fileId = null;
        try {

            try {
                fileId = compile(languageConfig, message);
            } catch (CompileError e) {
                judgeScore.setScore(BigDecimal.ZERO);
                judgeScore.setResult(JudgeResult.COMPILE_ERROR);
                return judgeScore.setErrorMessage(e.getStderr());
            }

            // 运行
            runResults = runCases(languageConfig, fileId, message, inputCases);
        } finally {
            // 删除
            if (fileId != null) {
                deleteFile(fileId);
            }
        }


        judgeScore = judgeCases(judgeScore, cases, runResults);

        // 重新计算分数
        BigDecimal score = calcScore(message, judgeScore);

        return judgeScore.setScore(score);
    }

    @Override
    public TestResult test(JudgeMessage message) throws SystemError, SubmitError {
        String language = message.getLanguage();
        LanguageConfig languageConfig = languageConfigLoader.getLanguageConfigByName(language);
        String fileId;
        try {
            fileId = compile(languageConfig, message);
        } catch (CompileError e) {
            return TestResult.compileError(e.getStderr());
        }
        RunResult runResult = runCases(languageConfig, fileId, message, List.of(message.getTestInput())).get(0);


        return TestResult.fromTestResul(runResult, JudgeUtils.judgeToStatus(runResult.getStatus()));
    }

    @Override
    public TestResult test(RunTestInfo message) {
        LanguageConfig languageConfig = languageConfigLoader.getLanguageConfigByName(message.getLanguage());
        String fileId = null;
        RunResult runResult = null;
        try {
            fileId = compile(languageConfig, message.getCode());
            runResult = judge.doJudge(
                    fileId,
                    languageConfig,
                    message.getStdin(),
                    languageConfig.getMaxCpuTime(),
                    languageConfig.getMaxMemory(),
                    128
            );
        } catch (CompileError e) {
            return TestResult.compileError(e.getStderr());

        } catch (SystemError | SubmitError e) {
          log.error(e.getMessage());
        } finally {
            if (fileId != null) {
                deleteFile(fileId);
            }
        }


        if (runResult != null) {
            return TestResult.fromTestResul(runResult, JudgeUtils.judgeToStatus(runResult.getStatus()));
        } else {
            // 不能让用户接触服务错误，用RuntimeError应付过去
            runResult = new RunResult();
            runResult.setFiles(new RunResult.StdIoFile());
            return TestResult.fromTestResul(runResult, JudgeResult.RUNTIME_ERROR);
        }
    }



    private static BigDecimal calcScore(JudgeMessage message, JudgeScore judgeScore) {
        if (message.getContestId() == null) {
            return judgeScore.getScore();
        }
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal score = judgeScore.getScore();
        for (OjProblemCaseVo case_ : message.getCases()) {
            totalScore = totalScore.add(Objects.requireNonNullElse(case_.getScore(), BigDecimal.ZERO));
        }
        if (totalScore.equals(BigDecimal.ZERO)) {
            score = BigDecimal.ZERO;
        } else if (message.getScore() != null) {
            score = score.divide(totalScore, 2, RoundingMode.FLOOR).multiply(message.getScore());
        }
        return score;
    }

    @Override
    public boolean sendJudgeMessage(JudgeInfo judgeInfo) {
        // 并发安全
        synchronized (this) {
            boolean available = JudgeDelayUtil.isAvailable(judgeInfo.getUserId());
            if (available) {
                JudgeDelayUtil.setDelay(judgeInfo.getUserId());
            } else {
                return false;
            }
        }

        rabbitTemplate.convertAndSend("judge-exchange", "judge-info", judgeInfo);
        return true;
    }

    @Override
    public boolean sendTestMessage(RunTestInfo testInfo) {
        // 并发安全
        synchronized (this) {
            boolean available = JudgeDelayUtil.isAvailable(testInfo.getUserId());
            if (available) {
                JudgeDelayUtil.setDelay(testInfo.getUserId());
            } else {
                return false;
            }
        }

        rabbitTemplate.convertAndSend("judge-exchange", "test-info", testInfo);
        return true;
    }

}
