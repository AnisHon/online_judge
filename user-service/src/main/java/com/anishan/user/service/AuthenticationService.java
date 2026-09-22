package com.anishan.user.service;

import com.anishan.api.domain.LoginUser;
import com.anishan.user.domain.dto.LoginForm;
import com.anishan.user.domain.dto.RegistrationForm;
import com.anishan.user.domain.vo.*;

import javax.validation.constraints.NotNull;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface AuthenticationService {

    LoginUserVo me();

    Long myId();

    LoginUser loadUserCache(Long userId);

    void cacheUser(LoginUser loginUser);

    // login
    LoginVo login(LoginForm loginForm, HttpServletResponse response);

    LoginVo refresh(String refreshToken);

    // registration
    LoginVo registration(RegistrationForm registrationForm, HttpServletResponse response);


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
    String ban(@NotNull List<Long> id);

    // 解封
    String unban(Long id);

    void logout(String refreshToken, HttpServletResponse response);

    List<MenuVo> getAuths(Long userId);

    List<TreedMenuVo> getTreedMenuByRole();

    boolean resetDefault(@NotNull Long id);
}
