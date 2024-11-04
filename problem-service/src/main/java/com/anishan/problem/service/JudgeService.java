package com.anishan.problem.service;

import com.anishan.api.domain.dto.JudgeRequest;
import com.anishan.api.domain.vo.ProblemJudgeResult;

public interface JudgeService {


    ProblemJudgeResult judge(Long userId, JudgeRequest judgeRequest);

    ProblemJudgeResult codeTest(JudgeRequest judgeRequest);
}
