package com.anishan.problem.controller;

import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.commons.domain.R;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.problem.domain.entity.Records;
import com.anishan.problem.domain.vo.UserAnswer;
import com.anishan.problem.service.RecordsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@Api("内部接口")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InternalController {

    private final RecordsService recordsService;

    @ApiOperation("存储判题结果用的")
    @PostMapping("/judgeResult")
    public R<Void> judgeResult(@RequestBody JudgeScore judgeScore) {

        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setCode( judgeScore.getCode());
        userAnswer.setLanguageId(judgeScore.getLanguageId());

        Records records = new Records();
        records
                .setContestId(judgeScore.getContestId())
                .setProblemId(judgeScore.getProblemId())
                .setUserId(judgeScore.getUserId())
                .setScore(judgeScore.getScore())
                .setScore(judgeScore.getScore())
                .setStatus(judgeScore.getResult() == JudgeResult.ACCEPT)
                .setAnswer(userAnswer);

        recordsService.addRecord(records);

        return R.success(null);
    }


}
