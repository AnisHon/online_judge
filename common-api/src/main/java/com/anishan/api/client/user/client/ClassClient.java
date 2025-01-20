package com.anishan.api.client.user.client;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.api.client.user.domain.vo.UserVo;
import com.anishan.api.config.FeignDecoderConfig;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.commons.domain.R;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.validation.constraints.NotNull;
import java.util.List;

@FeignClient(value = "user-service", contextId = "class", path = "class", configuration = FeignDecoderConfig.class)

public interface ClassClient {
    //todo 待验证
    @GetMapping("/exists/{classId}/{userId}")
    @PreAuthorize("hasAuthority('user:class:list-user')")
    R<Boolean> isUserExists(@PathVariable("classId") @NotNull Long classId, @PathVariable("userId") @NotNull Long userId);

    @GetMapping("/user/{classId}")
    R<List<UserVo>> listUser(@PathVariable("classId") Long classId);

}
