package com.anishan.judge.judge.impl;

import com.anishan.api.client.content.domain.OssFileInputStream;
import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.api.file.FileOperation;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.judge.domain.CaseContent;
import com.anishan.judge.domain.JudgeRunResultScore;
import com.anishan.judge.judge.GradeSubmission;
import com.anishan.judge.util.JudgeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;

@Slf4j
@Service
public class GradeSubmissionImpl implements GradeSubmission {

    private final FileOperation fileOperation;

    public GradeSubmissionImpl(FileOperation fileOperation) {
        this.fileOperation = fileOperation;
    }


    /**
     * 通过结果判分
     * @param runResult 判题机判题结果
     * @param caseContent 当前case对象
     * @return 分数状态，错误信息
     */
    @Override
    public JudgeRunResultScore judgeScore(RunResult runResult, CaseContent caseContent) {
        Integer status = runResult.getStatus();
        Integer exitStatus = runResult.getExitStatus();
        JudgeResult judgeResult = JudgeUtils.judgeToStatus(status);


        JudgeRunResultScore.JudgeRunResultScoreBuilder builder = JudgeRunResultScore
                .builder()
                .caseId(caseContent.getCaseId())
                .judgeResult(judgeResult)
                .score(BigDecimal.ZERO)
                .passed(false)
                .runtime(runResult.getRunTime() / 1_000_000) // ns -> ms
                .memory(runResult.getMemory() / (1024 * 1024));  // kib -> mib

        StringBuilder stringBuilder = new StringBuilder();

        // 拼错误信息
        if (exitStatus > 0 && exitStatus <= 31) {
            stringBuilder
                    .append("Exit Code:")
                    .append(exitStatus)
                    .append("\n")
                    .append(SandboxRunImpl.signals.get(exitStatus))
                    .append("\n")
                    .append(runResult.getFiles().getStderr());

            return builder
                    .errorMessage(stringBuilder.toString())
                    .internalError(stringBuilder.toString())
                    .build();
        }


        String outPath = caseContent.getOutPath();

        // 获取is
        try (OssFileInputStream is = fileOperation.getFile(outPath)) {
            if (JudgeUtils.equals(is, runResult.getFiles().getStdout())) {
                builder
                        .score(caseContent.getScore())
                        .judgeResult(JudgeResult.ACCEPT)
                        .passed(true);
            } else {
                builder.judgeResult(JudgeResult.WRONG_ANSWER);
            }
        } catch (IOException e) {
            log.error("答案对比出错，当前key:{}", outPath, e);
            builder.judgeResult(JudgeResult.WRONG_ANSWER);
        }

        return builder.build();
    }
}
