package com.anishan.user.controller;


import com.anishan.api.domain.SysUser;
import com.anishan.commons.domain.R;
import com.anishan.user.domain.dto.*;
import com.anishan.user.domain.vo.*;
import com.anishan.user.service.AuthenticationService;
import com.anishan.user.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

@Api("认证相关接口")
@RestController()
@RequestMapping("/auth")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthenticationController {


    private final AuthenticationService authenticationService;
    private final SysUserService sysUserService;

    @GetMapping("/me")
    @ApiOperation("获取用户个人信息")
    public R<LoginUserVo> me() {
        LoginUserVo me = authenticationService.me();
        return R.success(me);
    }

    @GetMapping("/auths")
    @ApiOperation("获取用户所有的权限")
    public R<List<MenuVo>> getAuths() {
        List<MenuVo> auths = authenticationService.getAuths();
        return R.success(auths);
    }

    @GetMapping("/logout")
    @ApiOperation("登出")
    public R<String> logout() {
        authenticationService.logout();
        return R.success();
    }

    @PostMapping("/login")
    @ApiOperation("登陆接口")
    public R<LoginVo> login(@RequestBody @Validated LoginForm loginUser) {
        LoginVo login = authenticationService.login(loginUser);
        return R.success(login);
    }

    @PostMapping("/registration")
    @ApiOperation("注册接口")
    public R<LoginVo> registration(@RequestBody @Validated RegistrationForm registrationForm) {
        LoginVo registration = authenticationService.registration(registrationForm);
        return R.success(registration);
    }


    @PostMapping("/reset-pass")
    @ApiOperation("重制密码，重新设置密码")
    public R<AuthResultVo> resetPass(@RequestBody @Validated PasswordResetRequest passwordResetRequest) {
        AuthResultVo authResultVo = authenticationService.resetPassword(
                passwordResetRequest.getCode(),
                passwordResetRequest.getPassword()
        );
        return authResultVo.toR();
    }

    @PostMapping("/forget-pass")
    @ApiOperation("忘记密码，重设密码")
    public R<AuthResultVo> forgetPass(
            @RequestBody @Validated PasswordForgetRequest passwordForgetRequest
            ) {
        SysUser user = sysUserService.getUserByUsernameOrEmail(passwordForgetRequest.getUsername());
        if (Objects.isNull(user)) {
            return AuthResultVo.fail("用户不存在").toR();
        }
        AuthResultVo authResultVo = authenticationService.resetPassword(
                user.getUserId(),
                user.getEmail(),
                passwordForgetRequest.getCode(),
                passwordForgetRequest.getPassword()
        );
        return authResultVo.toR();
    }


    @PostMapping("/reset-email")
    @ApiOperation("重制邮箱")
    public R<AuthResultVo> resetEmail(@RequestBody @Validated EmailResetRequest emailResetRequest) {
        AuthResultVo authResultVo = authenticationService.resetEmail(
                emailResetRequest.getCode(),
                emailResetRequest.getNewEmail()
        );
        return authResultVo.toR();
    }

    @PostMapping("/send-email-code")
    @ApiOperation("发送邮箱验证码")

    public R<AuthResultVo> sendEmailCode(@RequestBody @Validated EmailCodeRequest emailCodeRequest) {
        AuthResultVo authResultVo = authenticationService.sendEmailCode(
                emailCodeRequest.getEmail(),
                emailCodeRequest.getCaptchaToken(),
                emailCodeRequest.getCaptchaCode()
        );
        return authResultVo.toR();
    }

    @PostMapping("/send-forget-email-code")
    @ApiOperation("发送验证码，用于忘记密码，只需要提供用户名")
    public R<AuthResultVo> sendForgetEmailCode(@RequestBody @Validated ForgetEmailCodeRequest forgetEmailCodeRequest) {

        String email = sysUserService.getUserByUsernameOrEmail(forgetEmailCodeRequest.getUsername()).getEmail();
        AuthResultVo authResultVo = authenticationService.sendEmailCode(
                email,
                forgetEmailCodeRequest.getCaptchaToken(),
                forgetEmailCodeRequest.getCaptchaCode()
        );
        return R.success(authResultVo);
    }


    @GetMapping("/captcha-code")
    @ApiOperation("获取Captcha验证码")
    public R<CaptchaCodeVo> captchaCode() {
        CaptchaCodeVo captchaCodeVo = authenticationService.sendCaptchaCode();
        return R.success(captchaCodeVo);
    }

    @GetMapping("/ban/{id}")
    @ApiOperation("封禁用户")
    @PreAuthorize("hasAuthority('user:auth:ban')")
    public R<String> ban(@PathVariable @NotNull @ApiParam(value = "用户id", required = true) Long id) {
        String ban = authenticationService.ban(id);
        return R.success(ban);
    }

    @GetMapping("/unban/{id}")
    @ApiOperation("解封用户")
    @PreAuthorize("hasAuthority('user:auth:unban')")
    public R<String> unban(@PathVariable @NotNull @ApiParam(value = "用户id", required = true) Long id) {
        String ban = authenticationService.unban(id);
        return R.success(ban);
    }






}
