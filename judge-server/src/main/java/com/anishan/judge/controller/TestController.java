package com.anishan.judge.controller;

import com.anishan.api.client.gojudge.GoJudgeClient;
import com.anishan.api.client.gojudge.domain.GoJudgeVersion;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.judge.domain.entity.SubmitLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TestController {


    private final GoJudgeClient judgeClient;

    @GetMapping("/version")
    public GoJudgeVersion version() {


        return judgeClient.version();
    }


}
