package com.anishan.user.service;

import com.anishan.api.domain.LoginUser;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.exception.IllegalTokenException;
import com.anishan.commons.util.JwtUtil;
import com.anishan.user.domain.vo.LoginVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/** 只负责令牌签发、轮换和撤销，避免认证业务同时承担 token 生命周期。 */
@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private final AuthUtil authUtil;
    private final RefreshCookieService refreshCookieService;
    private final RefreshSessionService refreshSessionService;

    public LoginVo issue(LoginUser loginUser, HttpServletResponse response) {
        Long userId = loginUser.getUser().getUserId();
        authUtil.cacheLoginUser(loginUser);

        String sessionId = UUID.randomUUID().toString();
        String tokenId = UUID.randomUUID().toString();
        String refreshToken = JwtUtil.createRefreshToken(userId, sessionId, tokenId);
        refreshSessionService.create(sessionId, userId, tokenId);
        refreshCookieService.write(response, refreshToken);

        return issueAccessToken(userId);
    }

    public LoginVo refresh(String refreshToken) {
        JwtUtil.RefreshClaims claims = JwtUtil.parseRefreshClaims(refreshToken);
        if (!refreshSessionService.isActive(claims) || !authUtil.isUserExisted(claims.getUserId())) {
            throw new IllegalTokenException("刷新会话已失效");
        }
        LoginUser loginUser = authUtil.getLoginUser(claims.getUserId());
        if (loginUser == null) throw new IllegalTokenException("用户登录状态已失效");

        // 不做 rotation：多个标签页可以安全地并发刷新同一个服务端会话。
        return issueAccessToken(claims.getUserId());
    }

    public void revoke(String refreshToken, HttpServletResponse response) {
        try {
            if (refreshToken != null && !refreshToken.isBlank()) {
                JwtUtil.RefreshClaims claims = JwtUtil.parseRefreshClaims(refreshToken);
                refreshSessionService.revoke(claims);
            }
        } catch (IllegalTokenException ignored) {
            // 登出必须幂等；非法或过期 Cookie 也必须被清除。
        } finally {
            refreshCookieService.clear(response);
        }
    }

    private LoginVo issueAccessToken(Long userId) {
        String accessToken = JwtUtil.createAccessToken(userId);

        LoginVo result = new LoginVo();
        result.setSuccess(true);
        result.setMessage("登录成功");
        result.setAccessToken(accessToken);
        result.setExpiresIn(JwtUtil.ACCESS_EXPIRE_MINUTES * 60L);
        return result;
    }
}
