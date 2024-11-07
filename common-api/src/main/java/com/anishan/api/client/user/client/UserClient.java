package com.anishan.api.client.user.client;

import com.anishan.api.client.user.domain.vo.UserVo;
import com.anishan.api.config.FeignDecoderConfig;
import com.anishan.commons.domain.R;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(value = "user-service", contextId = "user", path = "user", configuration = FeignDecoderConfig.class)
public interface UserClient {
    @GetMapping("/list/{ids}")
    @PreAuthorize("hasAuthority('user:user:list')")
    @ApiOperation("通过多个id获取用户，id之间用','隔开")
    R<List<UserVo>> listUser(@PathVariable("ids") List<Long> ids);
}
