package com.anishan.api.util;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import com.anishan.api.config.ConstConfig;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.commons.enumeration.CaptchaCodeType;
import com.anishan.commons.exception.IllegalTokenException;
import com.anishan.commons.util.JwtUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@DependsOn("constConfig")
public class AuthUtil {



    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public final Long CODE_TIME_OUT_SECOND;
    public final Long CAPTCHA_TIME_OUT_SECOND;

    public AuthUtil(
            RedisTemplate<String, Object> redisTemplate,
            StringRedisTemplate stringRedisTemplate,
            ConstConfig config
    ) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.CODE_TIME_OUT_SECOND = config.getEmailCodeLifespan();
        this.CAPTCHA_TIME_OUT_SECOND = config.getCaptchaCodeLifespan();
    }


    @NotNull
    @Contract(pure = true)
    private static String getLoginKey(@NotNull Long id) {
        return "user-service:userId:" + id;
    }
    private static String getTokenKey(@NotNull Long id) {
        return "user-service:token:userId:" + id;
    }

    private static LoginUser getLoginUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof LoginUser) {
            return (LoginUser) principal;
        } else {
            throw new RuntimeException("用户未登录");
        }
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



    public static AbstractCaptcha generateCaptchaCode(CaptchaCodeType captchaType) {
        AbstractCaptcha captcha = null;
        final int width = 150, height = 75;
        switch (captchaType) {
            case Gif:
                captcha = CaptchaUtil.createGifCaptcha(width, height);
                break;
            case Line:
                captcha = CaptchaUtil.createLineCaptcha(width, height);
                break;
            case Shear:
                captcha = CaptchaUtil.createShearCaptcha(width, height);
                break;
            case Circle:
                captcha = CaptchaUtil.createCircleCaptcha(width, height);
                break;
        }
        return captcha;
    }
    


    public boolean hasEmailKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(getEmailCodeKey(key)));
    }

    public boolean hasCaptchaKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(getCaptchaCodeKey(key)));
    }

    public String getEmailCode(String email) {
        String emailCodeKey = getEmailCodeKey(email);
        return stringRedisTemplate.opsForValue().get(emailCodeKey);
    }

    public void removeEmailCode(@NotNull String email) {
        String emailCodeKey = getEmailCodeKey(email);
        stringRedisTemplate.delete(emailCodeKey);
    }

    public String getAndRemoveCaptchaCode(String captchaToken) {
        String captchaCodeKey = getCaptchaCodeKey(captchaToken);
        String s = stringRedisTemplate.opsForValue().get(captchaCodeKey);
        stringRedisTemplate.delete(captchaCodeKey);
        return s;
    }


    public void cacheEmailCode(String email, String code) {
        String emailCodeKey = getEmailCodeKey(email);
        stringRedisTemplate.opsForValue().set(emailCodeKey, code, CODE_TIME_OUT_SECOND, TimeUnit.SECONDS);
    }

    public void cacheCaptchaCode(String captchaToken, String code) {
        String captchaCodeKey = getCaptchaCodeKey(captchaToken);
        stringRedisTemplate.opsForValue().set(captchaCodeKey, code, CAPTCHA_TIME_OUT_SECOND, TimeUnit.SECONDS);
    }

    public void cacheLoginUser(@NotNull LoginUser user) {
        String loginKey = getLoginKey(user.getUser().getUserId());
        redisTemplate.opsForValue().set(loginKey, user, JwtUtil.EXPIRE_HOUR, TimeUnit.HOURS);
    }

    public void cacheToken(Long userId, @NotNull String token) {
        String tokenKey = getTokenKey(userId);
        redisTemplate.opsForValue().set(tokenKey, token, JwtUtil.EXPIRE_HOUR, TimeUnit.HOURS);
    }

    public LoginUser getLoginUser(Long userId) {
        String loginKey = getLoginKey(userId);
        Object o = redisTemplate.opsForValue().get(loginKey);
        return (LoginUser) o;
    }

    public boolean isUserExisted(@NotNull Long userId) {
        String loginKey = getLoginKey(userId);
        return Boolean.TRUE.equals(redisTemplate.opsForValue().getOperations().hasKey(loginKey));
    }

    public boolean checkEmailCode(String email, String inputCode) {
        String code = getEmailCode(email);
        return Objects.equals(code, inputCode);
    }

    public boolean checkAndRemoveCaptchaCode(String captchaToken, String inputCode) {
        String code = getAndRemoveCaptchaCode(captchaToken);
        return !Objects.equals(code.toLowerCase(), inputCode.toLowerCase());
    }

    public static String createToken(Long userId) {
        return JwtUtil.createToken(userId);
    }


    public void removeUser(Long id) {
        String loginKey = getLoginKey(id);
        String tokenKey = getTokenKey(id);

        redisTemplate.delete(loginKey);
        redisTemplate.delete(tokenKey);

    }

    public static LoginUser getContextUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//          todo
        if (authentication == null) {
            LoginUser loginUser = new LoginUser();
            loginUser.setUser(new SysUser());
            return loginUser;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            throw new IllegalTokenException("用户未登录");
        }
        return (LoginUser) principal;
    }

    public static LoginUser getNonThrowUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//          todo
        if (authentication == null) {
            LoginUser loginUser = new LoginUser();
            loginUser.setUser(new SysUser());
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            return null;
        }
        return (LoginUser) principal;

    }

}
