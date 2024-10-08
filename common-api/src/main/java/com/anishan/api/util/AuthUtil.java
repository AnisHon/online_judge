package com.anishan.api.util;

import cn.hutool.captcha.AbstractCaptcha;
import com.anishan.api.entity.LoginUser;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class AuthUtil {

    public static final int CODE_TIME_OUT_SECOND = 60 * 2;


    @NotNull
    @Contract(pure = true)
    private static String getLoginKey(@NotNull Long id) {
        return "user-service:userId:" + id;
    }

    @NotNull
    @Contract(pure = true)
    private static String getEmailCodeKey(String email) {
        return "user-service:email:" + email;
    }

    @NotNull
    @Contract(pure = true)
    private static String getCaptchaCodeKey(String captchaToken) {
        return "user-service:captcha:" + captchaToken;
    }

    @NotNull
    @Contract(pure = true)

    public static AbstractCaptcha generateCaptchaCode() {
        return cn.hutool.captcha.CaptchaUtil.createShearCaptcha(150, 75);
    }
    


    public static boolean hasEmailKey(@NotNull StringRedisTemplate redisTemplate, String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getEmailCodeKey(key)));
    }

    public static boolean hasCaptchaKey(@NotNull StringRedisTemplate redisTemplate, String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getCaptchaCodeKey(key)));
    }

    public static String getEmailCode(@NotNull StringRedisTemplate redisTemplate, String email) {
        String emailCodeKey = getEmailCodeKey(email);
        return redisTemplate.opsForValue().get(emailCodeKey);
    }

    public static void removeEmailCode(@NotNull StringRedisTemplate redisTemplate, @NotNull String email) {
        String emailCodeKey = getEmailCodeKey(email);
        redisTemplate.delete(emailCodeKey);
    }

    public static String getAndRemoveCaptchaCode(@NotNull StringRedisTemplate redisTemplate, String captchaToken) {
        String captchaCodeKey = getCaptchaCodeKey(captchaToken);
        String s = redisTemplate.opsForValue().get(captchaCodeKey);
        redisTemplate.delete(captchaCodeKey);
        return s;
    }


    public static void cacheEmailCode(@NotNull StringRedisTemplate redisTemplate, String email, String code) {
        String emailCodeKey = getEmailCodeKey(email);
        redisTemplate.opsForValue().set(emailCodeKey, code, CODE_TIME_OUT_SECOND, TimeUnit.SECONDS);
    }

    public static void cacheCaptchaCode(@NotNull StringRedisTemplate redisTemplate, String captchaToken, String code) {
        String captchaCodeKey = getCaptchaCodeKey(captchaToken);
        redisTemplate.opsForValue().set(captchaCodeKey, code, CODE_TIME_OUT_SECOND, TimeUnit.SECONDS);
    }

    public static void cacheLoginUser(@NotNull RedisTemplate<String, Object> redisTemplate, @NotNull LoginUser user) {
        String loginKey = getLoginKey(user.getUser().getUserId());
        redisTemplate.opsForValue().set(loginKey, user, JwtUtil.EXPIRE_HOUR, TimeUnit.HOURS);
    }

    public static LoginUser getLoginUser(@NotNull RedisTemplate<String, Object> redisTemplate, Long userId) {
        String loginKey = getLoginKey(userId);
        Object o = redisTemplate.opsForValue().get(loginKey);
        return (LoginUser) o;
    }

    public static boolean isUserExisted(@NotNull RedisTemplate<String, Object> redisTemplate, Long userId) {
        String loginKey = getLoginKey(userId);
        return Boolean.TRUE.equals(redisTemplate.opsForValue().getOperations().hasKey(loginKey));
    }

    public static boolean checkEmailCode(@NotNull StringRedisTemplate redisTemplate, String email, String inputCode) {
        String code = getEmailCode(redisTemplate, email);
        return Objects.equals(code, inputCode);
    }

    public static boolean checkAndRemoveCaptchaCode(@NotNull StringRedisTemplate redisTemplate, String captchaToken, String inputCode) {
        String code = getAndRemoveCaptchaCode(redisTemplate, captchaToken);
        return !Objects.equals(code, inputCode);
    }

    public static String createToken(Long userId) {
        return JwtUtil.createToken(userId);
    }


    public static void removeUser(RedisTemplate<String, Object> redisTemplate, Long id) {
        String loginKey = getLoginKey(id);
        redisTemplate.delete(loginKey);
    }
}
