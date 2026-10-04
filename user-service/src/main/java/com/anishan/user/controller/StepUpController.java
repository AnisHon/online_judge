package com.anishan.user.controller;

import com.anishan.commons.domain.R;
import com.anishan.user.domain.dto.NewEmailCodeRequest;
import com.anishan.user.domain.dto.StepUpEmailCodeRequest;
import com.anishan.user.domain.dto.StepUpVerifyRequest;
import com.anishan.user.domain.vo.StepUpVo;
import com.anishan.user.service.StepUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth/step-up")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class StepUpController {
    private final StepUpService stepUp;

    @PostMapping("/verify")
    public R<StepUpVo> verify(@RequestBody @Validated StepUpVerifyRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return R.success(stepUp.verify(request));
    }

    @PostMapping("/email-code")
    public R<String> sendIdentityCode(@RequestBody @Validated StepUpEmailCodeRequest request) {
        stepUp.sendIdentityCode(request);
        return R.success("验证码已发送至绑定邮箱");
    }

    @PostMapping("/new-email-code")
    public R<String> sendNewEmailCode(@RequestBody @Validated NewEmailCodeRequest request) {
        stepUp.sendNewEmailCode(request);
        return R.success("验证码已发送至新邮箱");
    }
}
