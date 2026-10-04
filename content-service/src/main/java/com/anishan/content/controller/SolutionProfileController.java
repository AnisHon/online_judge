package com.anishan.content.controller;

import com.anishan.api.domain.LoginUser;
import com.anishan.api.util.AuthUtil;
import com.anishan.content.domain.vo.ProfileSolutionsVo;
import com.anishan.content.service.SolutionProfileQueryService;
import com.anishan.commons.domain.R;
import com.anishan.commons.exception.ApiStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
public class SolutionProfileController {
    private final SolutionProfileQueryService service;

    @GetMapping("/{userId}/solutions")
    public R<ProfileSolutionsVo> solutions(@PathVariable Long userId,
                                          @RequestParam Map<String, String> query) {
        if (query.containsKey("includePrivate"))
            throw new ApiStatusException(400, "不支持此查询参数");
        LoginUser viewer = AuthUtil.getNonThrowUser();
        Long viewerId = viewer == null || viewer.getUser() == null ? null : viewer.getUser().getUserId();
        return R.success(service.getSolutions(userId, viewerId));
    }
}
