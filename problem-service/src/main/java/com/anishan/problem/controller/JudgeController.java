package com.anishan.problem.controller;

import com.anishan.api.entity.LoginUser;
import com.anishan.commons.entity.R;
import com.anishan.problem.domain.dto.JudgeRequest;
import com.anishan.problem.domain.vo.ProblemJudgeResult;
import com.anishan.problem.service.JudgeService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/judge")
@ApiModel("判题模块")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JudgeController {


    private final JudgeService judgeService;

    @PostMapping
    @ApiOperation("判题")
    public R<ProblemJudgeResult> judge(@RequestBody JudgeRequest judgeRequest) {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginUser user = (LoginUser) authentication.getPrincipal();

        ProblemJudgeResult judge = judgeService.judge(user.getUser().getUserId(), judgeRequest);
        return R.success(judge);
    }




}
