package com.anishan.api.client.problem.client;

import com.anishan.api.client.problem.domain.vo.SubmitLogVo;
import com.anishan.api.config.FeignDecoderConfig;
import com.anishan.commons.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "problem-service", contextId = "submit-log", path = "log", configuration = FeignDecoderConfig.class)
public interface SubmitLogClient {
    @GetMapping("/get/{id}")
    R<SubmitLogVo> getLog(@PathVariable("id") Long id, @RequestHeader("user-id") Long userId);

    @GetMapping("/submissions/{id}")
    R<SubmitLogVo> poll(@PathVariable("id") Long id, @RequestHeader("user-id") Long userId);

}
