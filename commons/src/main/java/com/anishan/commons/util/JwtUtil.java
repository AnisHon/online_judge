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

public class JwtUtil {

    public static final String KEY_USER_ID = "user_id";
//    todo
    private static final byte[] KEY_SIGN = "legacy-secret-redacted".getBytes();
    public static final int EXPIRE_HOUR = 7 * 24;

    public static String createToken(Long id) {
        DateTime now = DateTime.now();
        DateTime expTime = now.offsetNew(DateField.HOUR, EXPIRE_HOUR);
        Map<String, Object> payload = new HashMap<>();
        // 签发时间
        payload.put(JWTPayload.ISSUED_AT, now);
        // 过期时间
        payload.put(JWTPayload.EXPIRES_AT, expTime);
        // 生效时间
        payload.put(JWTPayload.NOT_BEFORE, now);
        // 内容
        payload.put(KEY_USER_ID, id);
        return JWTUtil.createToken(payload, KEY_SIGN);
    }


    public static boolean validate(String token) {
        JWT jwt = JWTUtil.parseToken(token).setKey(KEY_SIGN);
        // validate包含了verify
        return jwt.validate(0);
    }


    @NotNull
    public static Long parseJwt(String token) throws IllegalTokenException {
        if (StrUtil.isBlank(token)) {
            throw new IllegalTokenException("非法令牌");
        }

        if (!validate(token)) {
            throw new IllegalTokenException("令牌失效");
        }


        JWT jwt = JWTUtil
                .parseToken(token)
                .setKey(KEY_SIGN);

        String userId = jwt.getPayload().getClaim(KEY_USER_ID).toString();
        return Long.valueOf(userId);

    }


}
