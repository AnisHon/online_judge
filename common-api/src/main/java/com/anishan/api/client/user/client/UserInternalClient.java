package com.anishan.api.client.user.client;


import com.anishan.api.config.FeignDecoderConfig;
import com.anishan.api.client.user.domain.dto.UserSummaryRequest;
import com.anishan.api.client.user.domain.vo.UserSummaryVo;
import com.anishan.commons.domain.R;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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

    @PostMapping("/user-summaries")
    @ApiOperation("批量获取安全用户摘要")
    R<List<UserSummaryVo>> userSummaries(@RequestBody UserSummaryRequest request);

}
