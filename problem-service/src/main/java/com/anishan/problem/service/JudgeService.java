package com.anishan.problem.service;

import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.problem.domain.dto.JudgeRequest;
import com.anishan.problem.domain.dto.TestRequest;
import com.anishan.problem.domain.vo.ProblemJudgeResult;

public interface JudgeService {


    ProblemJudgeResult judge(Long userId, JudgeRequest judgeRequest);

    void codeTest(Long userId, TestRequest judgeRequest);

    TestResult testStatus(Long userId);
}
