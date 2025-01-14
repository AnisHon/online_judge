package com.anishan.api.client.user.client;


import com.anishan.api.config.FeignDecoderConfig;
import com.anishan.commons.domain.R;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(value = "user-service", contextId = "user-internal", path = "internal", configuration = FeignDecoderConfig.class)
public interface UserInternalClient {
    @GetMapping("/add-point/{userId}/{point}")
    @ApiOperation("添加用户奖励分")
    R<Boolean> addPoint(@PathVariable("point") String point, @PathVariable("userId") Long userId);

    @GetMapping("/nikeName/{ids}")
    @ApiOperation("通过ID获取用户名")
    R<Map<Long, String>> nikeName(@PathVariable("ids") List<Long> ids);

}
