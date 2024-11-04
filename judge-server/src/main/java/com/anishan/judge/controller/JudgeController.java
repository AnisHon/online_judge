package com.anishan.judge.controller;

import com.anishan.api.domain.dto.JudgeRequest;
import com.anishan.api.domain.vo.ProblemJudgeResult;
import com.anishan.commons.domain.R;
import com.anishan.judge.service.JudgeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api("原则上判题机接口不对外开放，所以这段Swagger注解前端看不到")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JudgeController {

    private final JudgeService judgeService;

    @ApiOperation("OJ判题")
    @PostMapping("/judge")
    public R<ProblemJudgeResult> judge(@RequestBody JudgeRequest request) {
        ProblemJudgeResult result = judgeService.judge(request);
        return R.success(result);
    }
}
