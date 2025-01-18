package com.anishan.judge.judge;

import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.judge.domain.CaseContent;
import com.anishan.judge.domain.JudgeRunResultScore;

/**
 * 用判题结果JudgeResult得到分数
 */
public interface GradeSubmission {


    /**
     * 通过结果判分
     * @param runResult 判题机判题结果
     * @param caseContent 当前case对象
     * @return 分数状态，错误信息
     */
    JudgeRunResultScore judgeScore(RunResult runResult, CaseContent caseContent);
}
