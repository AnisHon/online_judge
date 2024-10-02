package com.anishan.commons.util;

import cn.hutool.jwt.JWT;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import java.util.Date;

public class JwtUtil {

    public static final String KEY_USER_ID = "user_id";
    private static final byte[] KEY_SIGN = "legacy-secret-redacted".getBytes();
    private static final long EXPIRE = 7 * 24 * 60 * 60 * 1000;
    public static String createJwt(Long userId) {
        return JWT.create()
                .setKey(KEY_SIGN)
                .setPayload(KEY_USER_ID, userId.toString())
                .setExpiresAt(new Date(System.currentTimeMillis() + EXPIRE))
                .sign();
    }


    public static boolean verifyJwt(String token) {
        if (StringUtils.isBlank(token)) return false;
        return JWT.of(token).setKey(KEY_SIGN).verify();
    }


}
