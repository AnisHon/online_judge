package com.anishan.judge.judge.impl;

import com.anishan.api.domain.dto.JudgeRequest;
import com.anishan.api.domain.entity.OjProblemCase;
import com.anishan.commons.e.JudgeResult;
import com.anishan.judge.judge.Judge;

import java.util.List;

public class JudgeImpl implements Judge {

    public JudgeResult doJudge(JudgeRequest request, List<OjProblemCase> cases) {
        for (int index = 0; index < cases.size(); index++) {

        }
        return null;
    }


    @Override
    public JudgeResult judgeAll(JudgeRequest request, List<OjProblemCase> cases) {



        return null;
    }
}
