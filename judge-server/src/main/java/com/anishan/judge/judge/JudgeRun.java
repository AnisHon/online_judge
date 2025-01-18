package com.anishan.judge.judge;

import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;

public interface JudgeRun {
    JudgeScore judgeAll(JudgeInfo judgeInfo);

    TestResult judgeTest(RunTestInfo runTestInfo);
}
