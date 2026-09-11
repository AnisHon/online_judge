package com.anishan.user.controller;


import com.anishan.api.annotation.ControllerLog;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.domain.R;
import com.anishan.user.domain.dto.*;
import com.anishan.user.domain.vo.*;
import com.anishan.user.service.AuthenticationService;
import com.anishan.user.service.SysUserService;
import com.anishan.user.util.UserUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.PermitAll;
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
    private final AuthUtil authUtil;


    @PutMapping("/resetToDefault/{id}")
    @ApiOperation("重制用户密码")
    @PreAuthorize("hasAuthority('user:user:edit')")
    public R<Boolean> reset(@PathVariable @NotNull Long id) {
        boolean b = authenticationService.resetDefault(id);
        return R.success(b);
    }

    @GetMapping("/count")
    @ApiOperation("查看在线人数")
    @PreAuthorize("hasAuthority('user:user:list')")
    public R<Long> countOnline() {
        Long count = authUtil.countUser();
        return R.success(count);
    }

    @GetMapping("/me")
    @ApiOperation("获取用户个人信息")
    public R<LoginUserVo> me() {
        LoginUserVo me = authenticationService.me();
        return R.success(me);
    }

    @GetMapping("/auths")
    @ApiOperation("获取用户所有的权限")

    public R<List<MenuVo>> getAuths(@RequestHeader("user-id") Long userId) {
        List<MenuVo> auths = authenticationService.getAuths(userId);
        return R.success(auths);
    }


    @GetMapping("/menus")
    @ApiOperation("获取所有菜单，以树状的形式返回")
    public R<List<TreedMenuVo>> menus() {
        return R.success(authenticationService.getTreedMenuByRole());
    }



    @GetMapping("/logout")
    @ApiOperation("登出")
    @ControllerLog(api = "auth", operation = "logout", desc = "用户退出登录")
    public R<String> logout(@RequestHeader("token") String token) {
        Long userId = UserUtil.getUserId();
        authenticationService.logout(userId, token);
        return R.success();
    }

    @PostMapping("/login")
    @ApiOperation("登陆接口")
    @ControllerLog(api = "auth", operation = "login", desc = "用户登录")
    public R<LoginVo> login(@RequestBody @Validated LoginForm loginUser) {
        LoginVo login = authenticationService.login(loginUser);
        return R.success(login);
    }

    @PostMapping("/refresh")
    @ApiOperation("使用刷新令牌换取新的访问令牌")
    @PermitAll
    public R<LoginVo> refresh(@RequestBody java.util.Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) return R.unauthorized("刷新令牌不能为空");
        return R.success(authenticationService.refresh(refreshToken));
    }

    @PostMapping("/registration")
    @ApiOperation("注册接口")
    @ControllerLog(api = "auth", operation = "registration", desc = "用户注册")
    public R<LoginVo> registration(@RequestBody @Validated RegistrationForm registrationForm) {
        LoginVo registration = authenticationService.registration(registrationForm);
        return R.success(registration);
    }


    @PutMapping("/reset-pass")
    @ApiOperation("重制密码，重新设置密码")
    @ControllerLog(api = "auth", operation = "reset-pass", desc = "用户重设密码")
    public R<AuthResultVo> resetPass(@RequestBody @Validated PasswordResetRequest passwordResetRequest) {
        AuthResultVo authResultVo = authenticationService.resetPassword(
                passwordResetRequest.getCode(),
                passwordResetRequest.getPassword()
        );
        return authResultVo.toR();
    }

    @PostMapping("/forget-pass")
    @ApiOperation("忘记密码，重设密码")
    @ControllerLog(api = "auth", operation = "forget-pass", desc = "用户重设密码（忘记密码）")
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


    @PutMapping("/reset-email")
    @ApiOperation("重制邮箱")
    @ControllerLog(api = "auth", operation = "reset-email", desc = "用户重设邮箱")
    public R<AuthResultVo> resetEmail(@RequestBody @Validated EmailResetRequest emailResetRequest) {
        AuthResultVo authResultVo = authenticationService.resetEmail(
                emailResetRequest.getCode(),
                emailResetRequest.getNewEmail()
        );
        return authResultVo.toR();
    }
    
    @PostMapping("/send-email-code")
    @ApiOperation("发送邮箱验证码")
    @PermitAll
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
    @ControllerLog(api = "auth", operation = "send-forget-email-code", desc = "用户请求重设密码邮箱验证码")
    @PermitAll
    public R<AuthResultVo> sendForgetEmailCode(@RequestBody @Validated ForgetEmailCodeRequest forgetEmailCodeRequest) {

        SysUser user = sysUserService.getUserByUsernameOrEmail(forgetEmailCodeRequest.getUsername());
        // Do not reveal whether an account exists and avoid a null-pointer 500.
        if (user == null || user.getEmail() == null) {
            return R.success(AuthResultVo.success("如该帐号存在，验证码将发送至绑定邮箱"));
        }
        String email = user.getEmail();
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

    @PutMapping("/ban/{ids}")
    @ApiOperation("封禁用户")
    @PreAuthorize("hasAuthority('user:auth:ban')")
    @ControllerLog(api = "auth", operation = "ban", desc = "封禁用户")
    public R<String> ban(@PathVariable @NotNull @ApiParam(value = "用户id", required = true) List<Long> ids) {
        String ban = authenticationService.ban(ids);
        return R.success(ban);
    }

    @PutMapping("/unban/{id}")
    @ApiOperation("解封用户")
    @PreAuthorize("hasAuthority('user:auth:unban')")
    @ControllerLog(api = "auth", operation = "unban", desc = "解封用户")
    public R<String> unban(@PathVariable @NotNull @ApiParam(value = "用户id", required = true) Long id) {
        String ban = authenticationService.unban(id);
        return R.success(ban);
    }






}
