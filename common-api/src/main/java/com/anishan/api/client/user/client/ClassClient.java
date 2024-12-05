package com.anishan.api.client.user.client;

import com.anishan.api.config.FeignDecoderConfig;
import com.anishan.commons.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.validation.constraints.NotNull;

@FeignClient(value = "user-service", contextId = "class", path = "class", configuration = FeignDecoderConfig.class)

public interface ClassClient {
    //todo 待验证
    @GetMapping("/exists/{classId}/{userId}")
    @PreAuthorize("hasAuthority('user:class:list-user')")
    R<Boolean> isUserExists(@PathVariable("classId") @NotNull Long classId, @PathVariable("userId") @NotNull Long userId);

}
