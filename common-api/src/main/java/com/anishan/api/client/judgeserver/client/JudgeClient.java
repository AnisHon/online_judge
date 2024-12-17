package com.anishan.api.client.judgeserver.client;

import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;
import com.anishan.api.config.FeignDecoderConfig;
import com.anishan.commons.domain.R;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;


@FeignClient(value = "judge-server", contextId = "judge", path = "internal", configuration = FeignDecoderConfig.class)
public interface JudgeClient {

    @PostMapping(value = "judge-save")
    void judgeSave(@RequestBody JudgeScore judgeScore, @RequestHeader("user-id") Long userId);

    @ApiOperation("判题接口")
    @PostMapping("/judge")
    R<Boolean> judge(@RequestBody JudgeInfo judgeInfo);

    @ApiOperation("测试代码接口")
    @PostMapping("/test")
    R<Boolean> test(@RequestBody RunTestInfo runTestInfo);



}
