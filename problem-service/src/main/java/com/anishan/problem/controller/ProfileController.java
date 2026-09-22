package com.anishan.problem.controller;

import com.anishan.commons.domain.R;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.util.AuthUtil;
import com.anishan.problem.domain.vo.ProfileActivityVo;
import com.anishan.problem.service.ProfileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

@Api("个人主页活动摘要接口")
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{userId}")
    @ApiOperation("获取个人主页活动摘要")
    public R<ProfileActivityVo> getActivity(
            @PathVariable @NotNull Long userId) {
        LoginUser viewer = AuthUtil.getNonThrowUser();
        Long viewerId = viewer == null || viewer.getUser() == null ? null : viewer.getUser().getUserId();
        boolean owner = viewerId != null && viewerId.equals(userId);
        return R.success(profileService.getActivity(userId, owner));
    }
}
