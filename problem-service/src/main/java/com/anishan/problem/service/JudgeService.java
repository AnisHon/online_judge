package com.anishan.problem.service;

import com.anishan.problem.domain.dto.JudgeRequest;
import com.anishan.problem.domain.vo.ProblemJudgeResult;

public interface JudgeService {


    ProblemJudgeResult judge(Long userId, JudgeRequest judgeRequest);

    ProblemJudgeResult codeTest(JudgeRequest judgeRequest);
}
