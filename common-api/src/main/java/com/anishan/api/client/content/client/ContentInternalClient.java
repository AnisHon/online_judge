package com.anishan.api.client.content.client;

import com.anishan.api.client.user.domain.SseMessage;
import com.anishan.api.config.FeignDecoderConfig;
import com.anishan.commons.domain.R;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(value = "content-service", contextId = "content-internal", path = "internal", configuration = FeignDecoderConfig.class)
public interface ContentInternalClient {


    @PostMapping("/send-message")
    @ApiOperation("给用户发送信息，通过SSE")
    R<Boolean> sendMessage(@RequestBody SseMessage message);


}
