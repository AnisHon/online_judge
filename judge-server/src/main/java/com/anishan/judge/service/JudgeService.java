package com.anishan.judge.service;

import com.anishan.api.domain.dto.JudgeRequest;
import com.anishan.api.domain.vo.ProblemJudgeResult;

public interface JudgeService {
    ProblemJudgeResult judge(JudgeRequest request);
}
