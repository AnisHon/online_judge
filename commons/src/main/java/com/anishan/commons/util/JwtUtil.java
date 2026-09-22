package com.anishan.commons.util;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import com.anishan.commons.exception.IllegalTokenException;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JwtUtil {

    public static final String KEY_USER_ID = "user_id";


    private static final String ACCESS_SECRET_ENV = "OJ_JWT_ACCESS_SECRET";
    private static final String REFRESH_SECRET_ENV = "OJ_JWT_REFRESH_SECRET";
    private static final String TOKEN_TYPE = "token_type";
    private static final String SESSION_ID = "sid";
    private static final String TOKEN_ID = "jti";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";
    public static final int ACCESS_EXPIRE_MINUTES = 30;
    public static final int REFRESH_EXPIRE_DAYS = 7;
    public static final int EXPIRE_HOUR = REFRESH_EXPIRE_DAYS * 24;

    public static String createToken(Long id) {
        return createAccessToken(id);
    }

    public static String createAccessToken(Long id) {
        return create(id, ACCESS, ACCESS_EXPIRE_MINUTES, DateField.MINUTE, ACCESS_SECRET_ENV);
    }

    public static String createRefreshToken(Long id) {
        return createRefreshToken(id, UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    public static String createRefreshToken(Long id, String sessionId, String tokenId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(SESSION_ID, sessionId);
        claims.put(TOKEN_ID, tokenId);
        return create(id, REFRESH, REFRESH_EXPIRE_DAYS, DateField.DAY_OF_YEAR, REFRESH_SECRET_ENV, claims);
    }

    private static String create(Long id, String type, int amount, DateField field, String env) {
        return create(id, type, amount, field, env, new HashMap<>());
    }

    private static String create(Long id, String type, int amount, DateField field, String env,
                                 Map<String, Object> additionalClaims) {
        DateTime now = DateTime.now();
        DateTime expTime = now.offsetNew(field, amount);
        Map<String, Object> payload = new HashMap<>();
        // 签发时间
        payload.put(JWTPayload.ISSUED_AT, now);
        // 过期时间
        payload.put(JWTPayload.EXPIRES_AT, expTime);
        // 生效时间
        payload.put(JWTPayload.NOT_BEFORE, now);
        // 内容
        payload.put(KEY_USER_ID, id);
        payload.put(TOKEN_TYPE, type);
        payload.putAll(additionalClaims);
        return JWTUtil.createToken(payload, signingKey(env));
    }


    public static boolean validate(String token) {
        return validateAccess(token);
    }

    public static boolean validateAccess(String token) {
        return validate(token, ACCESS_SECRET_ENV, ACCESS);
    }

    public static boolean validateRefresh(String token) {
        return validate(token, REFRESH_SECRET_ENV, REFRESH);
    }

    private static boolean validate(String token, String env, String type) {
        try {
            JWT jwt = JWTUtil.parseToken(token).setKey(signingKey(env));
            return jwt.validate(0) && type.equals(jwt.getPayload().getClaim(TOKEN_TYPE));
        } catch (RuntimeException e) {
            return false;
        }
    }


    @NotNull
    public static Long parseJwt(String token) throws IllegalTokenException {
        return parseAccessJwt(token);
    }

    public static Long parseAccessJwt(String token) throws IllegalTokenException {
        return parse(token, ACCESS_SECRET_ENV, ACCESS);
    }

    public static Long parseRefreshJwt(String token) throws IllegalTokenException {
        return parse(token, REFRESH_SECRET_ENV, REFRESH);
    }

    /**
     * Parse and validate the claims required to look up a server-side refresh session.
     * The refresh token itself must not be stored in Redis; only these identifiers are.
     */
    public static RefreshClaims parseRefreshClaims(String token) throws IllegalTokenException {
        JWT jwt = parseAndValidate(token, REFRESH_SECRET_ENV, REFRESH);
        try {
            Long userId = Long.valueOf(jwt.getPayload().getClaim(KEY_USER_ID).toString());
            String sessionId = String.valueOf(jwt.getPayload().getClaim(SESSION_ID));
            String tokenId = String.valueOf(jwt.getPayload().getClaim(TOKEN_ID));
            if (StrUtil.isBlank(sessionId) || "null".equals(sessionId)
                    || StrUtil.isBlank(tokenId) || "null".equals(tokenId)) {
                throw new IllegalTokenException("刷新会话无效");
            }
            return new RefreshClaims(userId, sessionId, tokenId);
        } catch (IllegalTokenException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalTokenException("刷新会话无效");
        }
    }

    private static Long parse(String token, String env, String type) throws IllegalTokenException {
        JWT jwt = parseAndValidate(token, env, type);
        try {
            return Long.valueOf(jwt.getPayload().getClaim(KEY_USER_ID).toString());
        } catch (IllegalTokenException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalTokenException("令牌失效");
        }
    }

    private static JWT parseAndValidate(String token, String env, String type) throws IllegalTokenException {
        if (StrUtil.isBlank(token)) {
            throw new IllegalTokenException("非法令牌");
        }
        try {
            JWT jwt = JWTUtil.parseToken(token).setKey(signingKey(env));
            if (!jwt.validate(0) || !type.equals(jwt.getPayload().getClaim(TOKEN_TYPE))) {
                throw new IllegalTokenException("令牌失效");
            }
            return jwt;
        } catch (IllegalTokenException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalTokenException("令牌失效");
        }
    }

    public static final class RefreshClaims {
        private final Long userId;
        private final String sessionId;
        private final String tokenId;

        public RefreshClaims(Long userId, String sessionId, String tokenId) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.tokenId = tokenId;
        }

        public Long getUserId() {
            return userId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getTokenId() {
            return tokenId;
        }
    }

    private static byte[] signingKey(String env) {
        String secret = System.getenv(env);
        if (StrUtil.isBlank(secret) && ACCESS_SECRET_ENV.equals(env)) secret = System.getenv("OJ_JWT_SECRET");
        if (StrUtil.isBlank(secret) || secret.length() < 32) {
            throw new IllegalStateException(env + " must be set to at least 32 characters");
        }
        return secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }


}
