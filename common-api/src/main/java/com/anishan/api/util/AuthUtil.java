package com.anishan.api.util;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.StrUtil;
import com.anishan.api.config.ConstConfig;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.commons.enumeration.CaptchaCodeType;
import com.anishan.commons.exception.IllegalTokenException;
import com.anishan.commons.util.JwtUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@DependsOn("constConfig")
public class AuthUtil {

    private static final CodeGenerator mathGenerator;

    static {
        mathGenerator = new MathGenerator(1);
    }


    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public final Long CODE_TIME_OUT_SECOND;
    public final Long CAPTCHA_TIME_OUT_SECOND;


    @Autowired
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

    private static String getAllLoginKey() {
        return "user-service:userId:*";
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
        final int width = 200, height = 75;

        switch (captchaType) {
            case GIF:
                captcha = CaptchaUtil.createGifCaptcha(width, height, 4);
                break;
            case LINE:
                captcha = CaptchaUtil.createLineCaptcha(width, height, 4, 150);
                break;
            case SHEAR:
                captcha = CaptchaUtil.createShearCaptcha(width, height, 4, 1);
                break;
            case CIRCLE:
                captcha = CaptchaUtil.createCircleCaptcha(width, height, 4, 15);
                break;
            case MATH:
                captcha = CaptchaUtil.createShearCaptcha(width, height, mathGenerator, 1);
                break;
        }
        return captcha;
    }


    public synchronized boolean hasEmailKey(String key) {
        return stringRedisTemplate.hasKey(getEmailCodeKey(key));
    }

    public boolean hasCaptchaKey(String key) {
        return stringRedisTemplate.hasKey(getCaptchaCodeKey(key));
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
        return stringRedisTemplate.opsForValue().getAndDelete(captchaCodeKey);
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

    /**
     * 检查并且删除验证码，需要注意该方法返回的是验证码是否错误
     *
     * @param captchaToken 验证码UUID
     * @param inputCode    用户输入的验证码
     * @return 是否正确
     */
    public boolean checkAndRemoveCaptchaCode(String captchaToken, String inputCode, CaptchaCodeType type) {
        String code = getAndRemoveCaptchaCode(captchaToken);
        if (code == null || inputCode == null) {
            return false;
        }

        if (type == CaptchaCodeType.MATH) {
            return mathGenerator.verify(code, inputCode);
        } else {
            return StrUtil.equalsIgnoreCase(code, inputCode);
        }
    }

    public static String createToken(Long userId) {
        return JwtUtil.createToken(userId);
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

    public static Long getUserId() {
        return getContextUser().getUser().getUserId();
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

    public Long countUser() {
        String key = getAllLoginKey();
        long count = 0;
        ScanOptions scanOptions = ScanOptions.scanOptions().match(key).build();

        Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(connection -> connection.scan(scanOptions));

        while (cursor.hasNext()) {
            cursor.next();
            count++;  // 每扫描一个 key，计数增加
        }

        return count;
    }
}
