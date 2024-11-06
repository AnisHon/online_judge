package com.anishan.api.client.problem.client;

import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.config.FeignDecoderConfig;
import com.anishan.commons.domain.R;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "problem-service", contextId = "record", path = "record", configuration = FeignDecoderConfig.class)
public interface RecordClient {

    @PostMapping(value = "judge-save")
    void judgeSave(@RequestBody JudgeScore judgeScore, @RequestHeader("user-id") Long userId);
}
