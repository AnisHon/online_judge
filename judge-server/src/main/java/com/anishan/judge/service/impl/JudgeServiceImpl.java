package com.anishan.judge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
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
import com.anishan.judge.util.JudgeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JudgeServiceImpl implements JudgeService {

    private final Judge judge;
    private final Compiler compiler;
    private final LanguageConfigLoader languageConfigLoader;
    private final SandboxRun sandboxRun;

    private LanguageConfig getLanguageConfig(JudgeMessage message) {
        return languageConfigLoader.getLanguageConfigByName(message.getLanguage());
    }

    @Override
    public String compile(LanguageConfig config, JudgeMessage message) throws CompileError, SystemError, SubmitError {
        return compiler.compile(config, message.getCode(), config.getLanguage(), null);
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
            return judgeScore.setResult(JudgeResult.RuntimeError);
        }

        BigDecimal score = BigDecimal.ZERO;
        judgeScore.setResult(JudgeResult.Accept);


        // 对比所有的Answer然后判分
        for (int i = 0; i < cases.size(); i++) {

            OjProblemCaseVo answer = cases.get(i);
            RunResult userAnswer = results.get(i);

            boolean isAccepted = !Objects.equals(userAnswer.getStatus(), Constants.Judge.STATUS_ACCEPTED.getStatus()) && judgeScore.getResult() != JudgeResult.WrongAnswer;
            if (isAccepted) {
                judgeScore.setResult(JudgeUtils.judgeToStatus(userAnswer.getStatus()));
                judgeScore.setErrorMessage(userAnswer.getFiles().getStderr());
                continue;
            }

            String stdout = StrUtil.strip(userAnswer.getFiles().getStdout(), "\n");
            String answerOutput = StrUtil.strip(answer.getOutput(), "\n");
            if (!StrUtil.equals(answerOutput, stdout)) {
                judgeScore.setResult(JudgeResult.WrongAnswer);
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
                judgeScore.setResult(JudgeResult.CompileError);
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
        String fileId = null;
        try {
            fileId = compile(languageConfig, message);
        } catch (CompileError e) {
            return TestResult.compileError(e.getStderr());
        }
        RunResult runResult = runCases(languageConfig, fileId, message, List.of(message.getTestInput())).get(0);


        return TestResult.fromTestResul(runResult, JudgeUtils.judgeToStatus(runResult.getStatus()));
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

}
