package com.anishan.judge.judge;

import com.anishan.api.domain.dto.JudgeRequest;
import com.anishan.api.domain.entity.OjProblemCase;
import com.anishan.commons.e.JudgeResult;

import java.util.List;

public interface Judge {
    JudgeResult judgeAll(JudgeRequest request, List<OjProblemCase> cases);
}
