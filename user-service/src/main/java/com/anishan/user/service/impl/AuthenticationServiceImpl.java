package com.anishan.user.service.impl;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.core.util.IdUtil;
import com.anishan.api.domain.SysRole;
import com.anishan.api.domain.SysUser;
import com.anishan.commons.e.UserState;
import com.anishan.api.domain.LoginUser;
import com.anishan.user.domain.dto.LoginForm;
import com.anishan.user.domain.dto.RegistrationForm;
import com.anishan.user.domain.vo.CaptchaCodeVo;
import com.anishan.user.domain.vo.LoginVo;
import com.anishan.user.domain.vo.AuthResultVo;
import com.anishan.user.service.*;
import com.anishan.api.util.AuthUtil;
import com.anishan.user.util.EmailSender;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// todo
@Slf4j
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final SysUserService sysUserService;
    private final SysUserRoleService sysUserRoleService;
    private final SysMenuService sysMenuService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SysRoleService sysRoleService;
    // 默认就是student
    private static final Long DEFAULT_ROLE_ID = 1L;

    @Autowired
    public AuthenticationServiceImpl(
            SysUserService sysUserService,
            SysUserRoleService sysUserRoleService,
            SysMenuService sysMenuService,
            RedisTemplate<String, Object> redisTemplate,
            StringRedisTemplate stringRedisTemplate,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            SysRoleService sysRoleService
    ) {
        this.sysUserService = sysUserService;
        this.sysUserRoleService = sysUserRoleService;
        this.sysMenuService = sysMenuService;
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.sysRoleService = sysRoleService;
    }

    @Override
    public LoginUser me() {
        return (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Override
    public Long myId() {
        return me().getUser().getUserId();
    }

    @Override
    public LoginUser loadUserCache(Long userId) {
        return AuthUtil.getLoginUser(redisTemplate, userId);
    }

    @Override
    public void cacheUser(LoginUser loginUser) {
        AuthUtil.cacheLoginUser(redisTemplate, loginUser);
    }

    private String createLoginTokenAndCache(LoginUser loginUser) {
        AuthUtil.cacheLoginUser(redisTemplate, loginUser);
        return AuthUtil.createToken(loginUser.getUser().getUserId());
    }


    public Authentication doCheckLogin(LoginForm loginForm) {

        if (AuthUtil.checkAndRemoveCaptchaCode(stringRedisTemplate, loginForm.getToken(), loginForm.getCaptchaCode())) {
            throw new RuntimeException("验证码错误");
        }

        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(loginForm.getUsername(), loginForm.getPassword());

        Authentication authenticate = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        if (authenticate == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        return authenticate;


    }

    @Override
    public LoginVo login(LoginForm loginForm) {
        LoginVo loginVo = new LoginVo();


        Authentication authenticate;
        try {
            authenticate = doCheckLogin(loginForm);
        } catch (RuntimeException e) {
            loginVo.setSuccess(false);
            loginVo.setMessage(e.getMessage());
            return loginVo;
        }

        LoginUser loginUser = (LoginUser) authenticate.getPrincipal();

        String token = createLoginTokenAndCache(loginUser);

        loginVo.setSuccess(true);
        loginVo.setMessage("登陆成功");
        loginVo.setToken(token);
        return loginVo;
    }



    private LoginVo doCheckRegistration(RegistrationForm registrationForm) {
        LoginVo loginVo = new LoginVo();
        loginVo.setSuccess(false);

        if (!doCheckEmailCode(registrationForm.getCode())) {
            loginVo.setMessage("邮箱验证码错误");
        } else if (sysUserService.existsUsername(registrationForm.getUserName())) {
            loginVo.setMessage("用户已存在");
        } else if (sysUserService.existsEmail(registrationForm.getEmail())) {
            loginVo.setMessage("邮箱已被使用");
        } else {
            loginVo.setSuccess(true);
            loginVo.setMessage("注册成功");
        }

        // 校验 删除
        AuthUtil.removeEmailCode(stringRedisTemplate, registrationForm.getEmail());
        return loginVo;
    }

    private Long bindDefaultRole(SysUser sysUser) {
        sysUserRoleService.addRoleForUser(sysUser.getUserId(), DEFAULT_ROLE_ID);
        return DEFAULT_ROLE_ID;
    }

    private LoginUser doBuildLoginUser(SysUser sysUser) {
        LoginUser loginUser = new LoginUser();

        sysUserService.save(sysUser);

        Long roleId = bindDefaultRole(sysUser);


        List<Long> roleIds = List.of(roleId);
        List<String> authorities = sysMenuService.getAuthorities(roleIds);
        List<SysRole> sysRoles = sysRoleService.listByIds(roleIds);

        loginUser.setUser(sysUser);
        loginUser.setAuths(authorities);
        loginUser.setRoles(sysRoles);
        return loginUser;
    }

    @Transactional
    @Override
    public LoginVo registration(RegistrationForm registrationForm) {

        LoginVo loginVo = doCheckRegistration(registrationForm);
        if (!loginVo.isSuccess()) {
            return loginVo;
        }

        SysUser sysUser = new SysUser();

        sysUser.setUserName(registrationForm.getUserName());
        sysUser.setNikeName(registrationForm.getNikeName());
        sysUser.setEmail(registrationForm.getEmail());
        sysUser.setPassword(passwordEncoder.encode(registrationForm.getPassword()));

        LoginUser loginUser = doBuildLoginUser(sysUser);

        String token = createLoginTokenAndCache(loginUser);
        loginVo.setToken(token);

        return loginVo;
    }

    private boolean doCheckEmailCode(String code) {
        String email = me().getUser().getEmail();
        return doCheckEmailCode(email, code);
    }

    private boolean doCheckEmailCode(String email, String code) {
        return AuthUtil.checkEmailCode(stringRedisTemplate, email, code);
    }
    private AuthResultVo checkEmailCode(String code) {
        String email = me().getUser().getEmail();
        return checkEmailCode(email, code);
    }

    private AuthResultVo checkEmailCode(String email, String code) {

        AuthResultVo authResultVo = new AuthResultVo();
        authResultVo.setSuccess(false);

        if (!doCheckEmailCode(email, code)) {
            authResultVo.setMessage("验证码错误");

        } else {
            authResultVo.setSuccess(true);
            authResultVo.setMessage("修改成功");
        }

        return authResultVo;
    }

    /**
     * @param username 这里的username可以是username或者email
     */
    public AuthResultVo resetPassword(String username, String code, String newPassword) {

        SysUser user = sysUserService.getUserByUsernameOrEmail(username);
        if (user == null) {
            AuthResultVo authResultVo = new AuthResultVo();
            authResultVo.setSuccess(false);
            authResultVo.setMessage("用户不存在");
            return authResultVo;
        }

        return resetPassword(user.getUserId(), user.getEmail(), code, newPassword);

    }

    @Override
    public AuthResultVo resetPassword(Long userId, String email, String code, String newPassword) {
        AuthResultVo authResultVo = checkEmailCode(code);
        if (!authResultVo.isSuccess()) {
            return authResultVo;
        }

        newPassword = passwordEncoder.encode(newPassword);

        sysUserService.update(new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getPassword, newPassword)
                .eq(SysUser::getUserId, email)
        );

        return authResultVo;
    }

    @Override
    public AuthResultVo resetPassword(String code, String newPassword) {
        SysUser user = me().getUser();

        return resetPassword(user.getUserId(), user.getEmail(), code, newPassword);
    }

    @Override
    public AuthResultVo resetEmail(String code, String newEmail) {

        AuthResultVo authResultVo = checkEmailCode(code);
        if (!authResultVo.isSuccess()) {
            return authResultVo;
        }

        Long userId = me().getUser().getUserId();

        try{
            sysUserService.update(new LambdaUpdateWrapper<SysUser>()
                    .set(SysUser::getEmail, newEmail)
                    .eq(SysUser::getUserId, userId)
            );
        } catch (DuplicateKeyException e) {
            authResultVo.setSuccess(false);
            authResultVo.setMessage("邮箱已经被使用了");
            return authResultVo;
        }

        return authResultVo;
    }

    private void doSendEmailCheck(String email, String captchaToken, String captchaCode) {
        if (AuthUtil.hasEmailKey(stringRedisTemplate, email)){
            throw new RuntimeException("已发送验证码请等待");
        }
        if (AuthUtil.checkAndRemoveCaptchaCode(stringRedisTemplate, captchaToken, captchaCode)){
            throw new RuntimeException("验证码错误");
        }
    }


    @Override
    public AuthResultVo sendEmailCode(String email, String captchaToken, String captchaCode) {

        AuthResultVo authResultVo = new AuthResultVo();

        try {
            doSendEmailCheck(email, captchaToken, captchaCode);
        } catch (RuntimeException e) {
            authResultVo.setSuccess(false);
            authResultVo.setMessage(e.getMessage());
            return authResultVo;
        }

        String code = EmailSender.sendEmailCodeAsync(email);

        AuthUtil.cacheEmailCode(stringRedisTemplate, email, code);

        authResultVo.setSuccess(true);
        authResultVo.setMessage("发送成功");

        return authResultVo;

    }

    @Override
    public CaptchaCodeVo sendCaptchaCode() {
        AbstractCaptcha captcha = AuthUtil.generateCaptchaCode();
        String token = IdUtil.fastSimpleUUID();
        AuthUtil.cacheCaptchaCode(stringRedisTemplate, token, captcha.getCode());

        CaptchaCodeVo captchaCodeVo = new CaptchaCodeVo();
        captchaCodeVo.setToken(token);
        captchaCodeVo.setImage(captcha.getImageBase64Data());

        return captchaCodeVo;
    }

    @Override
    public String ban(Long id) {

        SysUser user = new SysUser();
        user.setUserId(id);
        user.setStatus(UserState.BANNED);

        boolean update = sysUserService.save(user);

        if (update) {
            logout(id);
        }

        return update ? "封禁成功" : "封禁失败";
    }

    @Override
    public String unban(Long id) {
        boolean update = sysUserService.update(
                new LambdaUpdateWrapper<SysUser>()
                        .set(SysUser::getStatus, UserState.NORMAL)
                        .eq(SysUser::getUserId, id)
        );

        return update ? "解禁成功" : "解禁失败";

    }

    @Override
    public void logout(Long id) {
        AuthUtil.removeUser(redisTemplate, id);
    }

    @Override
    public void logout() {
        Long userId = me().getUser().getUserId();
        logout(userId);
    }


}
