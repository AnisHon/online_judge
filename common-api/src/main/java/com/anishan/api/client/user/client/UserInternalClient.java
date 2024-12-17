package com.anishan.api.client.user.client;

import com.anishan.api.client.user.domain.SseMessage;
import com.anishan.api.config.FeignDecoderConfig;
import com.anishan.commons.domain.R;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "user-service", contextId = "user-internal", path = "internal", configuration = FeignDecoderConfig.class)
public interface UserInternalClient {
    @GetMapping("/add-point/{userId}/{point}")
    @ApiOperation("添加用户奖励分")
    R<Boolean> addPoint(@PathVariable("point") String point, @PathVariable("userId") Long userId);

    @PostMapping("/send-message")
    @ApiOperation("给用户发送信息，通过SSE")
    R<Boolean>  sendMessage(@RequestBody SseMessage message);
}
