package com.anishan.user.controller;

import com.anishan.api.client.gojudge.GoJudgeClient;
import com.anishan.api.client.gojudge.domain.GoJudgeVersion;
import com.anishan.commons.domain.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api("测试类")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TestController {

    private final GoJudgeClient goJudgeClient;

    @GetMapping("/test-judge")
    @ApiOperation("测试judge服务")
    public R<GoJudgeVersion> testJudge() {
        return R.success(goJudgeClient.version());
    }

    @GetMapping("/test")
    @ApiOperation("测试方法，返回固定字符串test")
    public String test() {
        return "test";
    }


}
