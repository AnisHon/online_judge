package com.anishan.user.service;

import com.anishan.api.domain.LoginUser;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.exception.IllegalTokenException;
import com.anishan.commons.util.JwtUtil;
import com.anishan.user.domain.vo.LoginVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/** 只负责令牌签发、轮换和撤销，避免认证业务同时承担 token 生命周期。 */
@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private final AuthUtil authUtil;

    public LoginVo issue(LoginUser loginUser) {
        Long userId = loginUser.getUser().getUserId();
        authUtil.cacheLoginUser(loginUser);
        String accessToken = JwtUtil.createAccessToken(userId);
        String refreshToken = JwtUtil.createRefreshToken(userId);
        authUtil.cacheToken(accessToken, JwtUtil.ACCESS_EXPIRE_MINUTES, TimeUnit.MINUTES);
        authUtil.cacheToken(refreshToken, JwtUtil.REFRESH_EXPIRE_DAYS, TimeUnit.DAYS);

        LoginVo result = new LoginVo();
        result.setSuccess(true);
        result.setMessage("登录成功");
        result.setToken(accessToken); // 兼容旧客户端
        result.setAccessToken(accessToken);
        result.setRefreshToken(refreshToken);
        result.setExpiresIn(JwtUtil.ACCESS_EXPIRE_MINUTES * 60L);
        return result;
    }

    public LoginVo refresh(String refreshToken) {
        Long userId = JwtUtil.parseRefreshJwt(refreshToken);
        if (!authUtil.existToken(refreshToken) || !authUtil.isUserExisted(userId)) {
            throw new IllegalTokenException("刷新令牌已失效");
        }
        // refresh rotation：旧 refresh key 立即失效，降低重放风险。
        authUtil.removeToken(refreshToken);
        LoginUser loginUser = authUtil.getLoginUser(userId);
        if (loginUser == null) throw new IllegalTokenException("用户登录状态已失效");
        return issue(loginUser);
    }

    public void revoke(String token) {
        if (token != null && !token.isBlank()) authUtil.removeToken(token);
    }
}
