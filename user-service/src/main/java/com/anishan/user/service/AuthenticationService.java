package com.anishan.user.service;

import com.anishan.api.domain.LoginUser;
import com.anishan.user.domain.dto.LoginForm;
import com.anishan.user.domain.dto.RegistrationForm;
import com.anishan.user.domain.vo.CaptchaCodeVo;
import com.anishan.user.domain.vo.LoginVo;
import com.anishan.user.domain.vo.AuthResultVo;

public interface AuthenticationService {

    LoginUser me();

    Long myId();

    LoginUser loadUserCache(Long userId);

    void cacheUser(LoginUser loginUser);

    // login
    LoginVo login(LoginForm loginForm);

    // registration
    LoginVo registration(RegistrationForm registrationForm);


    AuthResultVo resetPassword(Long userId, String email, String code, String newPassword);

    // resetPassword
    AuthResultVo resetPassword(String code, String newPassword);

    // resetEmail
    AuthResultVo resetEmail(String code, String newEmail);

    // sendEmailCode
    AuthResultVo sendEmailCode(String email, String captchaToken, String captchaCode);

    // captchaCode
    CaptchaCodeVo sendCaptchaCode();

    // 封禁
    String ban(Long id);

    // 解封
    String unban(Long id);


    void logout(Long id);

    void logout();
}
