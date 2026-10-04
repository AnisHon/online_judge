package com.anishan.user.controller;

import com.anishan.api.client.user.domain.vo.UserSummaryVo;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.user.domain.dto.FollowPageQuery;
import com.anishan.user.domain.vo.FollowSummaryVo;
import com.anishan.user.service.FollowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Api("用户关注")
@RestController
@RequestMapping("/follow")
@Validated
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@PreAuthorize("isAuthenticated()")
public class FollowController {

    private final FollowService followService;

    @PutMapping("/{userId}")
    @ApiOperation("关注用户")
    public R<FollowSummaryVo> follow(@PathVariable("userId") Long userId) {
        return R.success(followService.follow(userId));
    }

    @DeleteMapping("/{userId}")
    @ApiOperation("取消关注")
    public R<FollowSummaryVo> unfollow(@PathVariable("userId") Long userId) {
        return R.success(followService.unfollow(userId));
    }

    @GetMapping("/{userId}/summary")
    @ApiOperation("获取关注关系摘要")
    public R<FollowSummaryVo> summary(@PathVariable("userId") Long userId) {
        return R.success(followService.getSummary(userId));
    }

    @GetMapping("/{userId}/following")
    @ApiOperation("分页获取用户关注列表")
    public R<PagedResult<UserSummaryVo>> following(@PathVariable("userId") Long userId,
                                                   @Validated FollowPageQuery query) {
        return followService.getFollowing(userId, query).toR();
    }

    @GetMapping("/{userId}/followers")
    @ApiOperation("分页获取用户粉丝列表")
    public R<PagedResult<UserSummaryVo>> followers(@PathVariable("userId") Long userId,
                                                   @Validated FollowPageQuery query) {
        return followService.getFollowers(userId, query).toR();
    }
}
