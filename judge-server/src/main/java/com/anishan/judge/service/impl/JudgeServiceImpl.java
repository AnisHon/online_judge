package com.anishan.judge.service.impl;

import com.anishan.api.domain.dto.JudgeRequest;
import com.anishan.api.domain.vo.ProblemJudgeResult;
import com.anishan.judge.service.JudgeService;
import org.springframework.stereotype.Component;

@Component
public class JudgeServiceImpl implements JudgeService {
    @Override
    public ProblemJudgeResult judge(JudgeRequest request) {
        return null;
    }
}
