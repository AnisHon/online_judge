package com.anishan.user.controller;


import com.anishan.user.domain.entity.SysUser;
import com.anishan.commons.entity.R;
import com.anishan.user.domain.LoginUser;
import com.anishan.user.domain.dto.LoginForm;
import com.anishan.user.domain.dto.RegistrationForm;
import com.anishan.user.domain.vo.AuthResultVo;
import com.anishan.user.domain.vo.CaptchaCodeVo;
import com.anishan.user.domain.vo.LoginVo;
import com.anishan.user.domain.vo.TreedMenuVo;
import com.anishan.user.service.AuthenticationService;
import com.anishan.user.service.SysMenuService;
import com.anishan.user.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

@Api("认证相关接口")
@RestController()
@RequestMapping("/auth")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthenticationController {


    private final SysMenuService sysMenuService;
    private final AuthenticationService authenticationService;
    private final SysUserService sysUserService;

    @GetMapping("/menus")
    @ApiOperation("获取所有菜单，以树状的形式返回")
    public R<List<TreedMenuVo>> menus() {
        return R.success(sysMenuService.getTreedMenuByRole(List.of()));
    }

    @GetMapping("/me")
    @ApiOperation("获取用户个人信息")
    public R<LoginUser> me() {
        LoginUser me = authenticationService.me();
        return R.success(me);
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
    public R<AuthResultVo> resetPass(
            @RequestParam @NotNull @ApiParam(value = "邮箱验证码", required = true) String code,
            @RequestParam @Length(min = 8, max = 16) @ApiParam(value = "新密码", required = true) String password
    ) {
        AuthResultVo authResultVo = authenticationService.resetPassword(code, password);
        return authResultVo.toR();
    }

    @PostMapping("/forget-pass")
    @ApiOperation("忘记密码，重设密码")
    public R<AuthResultVo> forgetPass(
            @RequestParam @NotNull @ApiParam(value = "邮箱或者用户名", required = true) String username,
            @RequestParam @NotNull @ApiParam(value = "邮箱验证码", required = true) String code,
            @RequestParam @Length(min = 8, max = 16) @ApiParam(value = "新密码", required = true) String password
    ) {
        SysUser user = sysUserService.getUserByUsernameOrEmail(username);
        if (Objects.isNull(user)) {
            return AuthResultVo.fail("用户不存在").toR();
        }
        AuthResultVo authResultVo = authenticationService.resetPassword(
                user.getUserId(),
                user.getEmail(),
                code,
                password
        );
        return authResultVo.toR();
    }


    @PostMapping("/reset-email")
    @ApiOperation("重制邮箱")
    public R<AuthResultVo> resetEmail(
            @RequestParam @NotNull @ApiParam(value = "邮箱验证码", required = true) String code,
            @RequestParam @Email @ApiParam(value = "新邮箱", required = true) String newEmail
    ) {
        AuthResultVo authResultVo = authenticationService.resetEmail(code, newEmail);
        return authResultVo.toR();
    }

    @PostMapping("/send-email-code")
    @ApiOperation("发送邮箱验证码")

    public R<AuthResultVo> sendEmailCode(
            @RequestParam @Email @ApiParam(value = "邮箱", required = true) String email,
            @RequestParam @NotNull @ApiParam(value = "验证码token", required = true) String captchaToken,
            @RequestParam @NotNull @ApiParam(value = "验证码code", required = true) String captchaCode) {
        AuthResultVo authResultVo = authenticationService.sendEmailCode(email, captchaToken, captchaCode);
        return authResultVo.toR();
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
