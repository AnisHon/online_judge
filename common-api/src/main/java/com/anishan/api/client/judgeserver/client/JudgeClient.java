package com.anishan.api.client.judgeserver.client;

import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.config.FeignDecoderConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;


@FeignClient(value = "judge-server", contextId = "record", path = "internal", configuration = FeignDecoderConfig.class)
public interface JudgeClient {

    @PostMapping(value = "judge-save")
    void judgeSave(@RequestBody JudgeScore judgeScore, @RequestHeader("user-id") Long userId);


}
