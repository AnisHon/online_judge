package com.anishan.user.service;

import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.enumeration.UserState;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.user.config.UserConfig;
import com.anishan.user.domain.dto.*;
import com.anishan.user.domain.vo.StepUpVo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Current-identity verification and single-use grants, independent of login tokens. */
@Service
@RequiredArgsConstructor
public class StepUpService {
    static final long GRANT_SECONDS = 300;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "user-service:step-up:";
    private static final DefaultRedisScript<Long> LIMIT = new DefaultRedisScript<>(
            "local n=redis.call('incr',KEYS[1]); if n==1 then redis.call('expire',KEYS[1],ARGV[1]) end; return n", Long.class);
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>(
            "if redis.call('get',KEYS[1])~=ARGV[1] then return 0 end; redis.call('del',KEYS[1]); return 1", Long.class);
    private static final DefaultRedisScript<Long> CONSUME_EMAIL = new DefaultRedisScript<>(
            "if redis.call('get',KEYS[1])~=ARGV[1] then return 0 end; "
                    + "if redis.call('get',KEYS[2])~=ARGV[2] then return 0 end; "
                    + "redis.call('del',KEYS[1],KEYS[2]); return 1", Long.class);

    private final StringRedisTemplate redis;
    private final SysUserService users;
    private final PasswordEncoder encoder;
    private final AuthUtil authUtil;
    private final UserConfig userConfig;
    private final EmailService emailService;

    public SysUser currentUser() {
        Long id = AuthUtil.getUserId();
        ThrowUtil.businessError(id == null, "请先登录");
        SysUser user = users.getById(id);
        ThrowUtil.businessError(user == null || user.getStatus() == UserState.BANNED, "当前账号无法进行此操作");
        return user;
    }

    public StepUpVo verify(StepUpVerifyRequest request) {
        SysUser user = currentUser();
        limitAttempts(user.getUserId(), "verify");
        if (request.getMethod() == StepUpVerifyRequest.Method.PASSWORD) {
            ThrowUtil.businessError(request.getPassword() == null || request.getPassword().isBlank()
                    || !encoder.matches(request.getPassword(), user.getPassword()), "当前密码不正确");
        } else {
            ThrowUtil.businessError(user.getEmail() == null || user.getEmail().isBlank(), "请使用当前密码验证身份");
            String key = identityCodeKey(user, request.getAction());
            ThrowUtil.businessError(request.getCode() == null || request.getCode().isBlank()
                    || !Long.valueOf(1).equals(redis.execute(CONSUME, Collections.singletonList(key), hash(request.getCode()))),
                    "邮箱验证码错误或已过期");
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(grantKey(token), grantValue(user, request.getAction()), GRANT_SECONDS, TimeUnit.SECONDS);
        return new StepUpVo(token, GRANT_SECONDS);
    }

    public void sendIdentityCode(StepUpEmailCodeRequest request) {
        SysUser user = currentUser();
        ThrowUtil.businessError(user.getEmail() == null || user.getEmail().isBlank(), "当前账号尚未绑定邮箱，请使用密码验证");
        checkCaptcha(request.getCaptchaToken(), request.getCaptchaCode());
        sendCode(user.getUserId(), identityCodeKey(user, request.getAction()), user.getEmail());
    }

    public void sendNewEmailCode(NewEmailCodeRequest request) {
        SysUser user = currentUser();
        requireGrant(user, StepUpAction.EMAIL, request.getStepUpToken());
        String email = normalizeEmail(request.getNewEmail());
        checkNewEmail(user, email);
        checkCaptcha(request.getCaptchaToken(), request.getCaptchaCode());
        sendCode(user.getUserId(), newEmailCodeKey(user, email), email);
    }

    private void sendCode(Long userId, String key, String email) {
        // Shared per-account cooldown prevents bypassing it by changing the action or target email.
        String stage = key.startsWith(PREFIX + "identity-code:") ? "identity" : "new-email";
        String cooldownKey = PREFIX + "mail-cooldown:" + userId + ":" + stage;
        ThrowUtil.businessError(!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(cooldownKey, "1", 60, TimeUnit.SECONDS)),
                "请稍后再获取验证码");
        try {
            String code = emailService.sendEmailCode(email);
            redis.opsForValue().set(key, hash(code), GRANT_SECONDS, TimeUnit.SECONDS);
        } catch (RuntimeException error) {
            redis.delete(cooldownKey);
            throw error;
        }
    }

    public void checkNewEmail(SysUser user, String email) {
        ThrowUtil.businessError(email.equalsIgnoreCase(Objects.toString(user.getEmail(), "")), "新邮箱与当前邮箱相同");
        ThrowUtil.businessError(users.existsEmail(email), "邮箱已经被使用");
    }

    public void consumePasswordGrant(SysUser user, String token) {
        requireGrant(user, StepUpAction.PASSWORD, token);
        ThrowUtil.businessError(!Long.valueOf(1).equals(redis.execute(CONSUME, Collections.singletonList(grantKey(token)),
                grantValue(user, StepUpAction.PASSWORD))), "身份验证已失效，请重新验证");
    }

    public void consumeEmailGrant(SysUser user, String token, String email, String code) {
        requireGrant(user, StepUpAction.EMAIL, token);
        limitAttempts(user.getUserId(), "new-email");
        ThrowUtil.businessError(code == null || code.isBlank()
                || !Long.valueOf(1).equals(redis.execute(CONSUME_EMAIL,
                Arrays.asList(grantKey(token), newEmailCodeKey(user, email)), grantValue(user, StepUpAction.EMAIL), hash(code))),
                "身份验证已失效或新邮箱验证码不正确");
    }

    private void requireGrant(SysUser user, StepUpAction action, String token) {
        ThrowUtil.businessError(token == null || token.isBlank() || token.length() > 64
                || !grantValue(user, action).equals(redis.opsForValue().get(grantKey(token))), "身份验证已失效，请重新验证");
    }

    private void limitAttempts(Long userId, String purpose) {
        Long count = redis.execute(LIMIT, Collections.singletonList(PREFIX + "attempts:" + userId + ":" + purpose), "600");
        ThrowUtil.businessError(count == null || count > 5, "验证次数过多，请在十分钟后重试");
    }

    private void checkCaptcha(String token, String code) {
        ThrowUtil.businessError(!authUtil.checkAndRemoveCaptchaCode(token, code, userConfig.getCaptchaType()), "图形验证码错误或已过期");
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String grantKey(String token) { return PREFIX + "grant:" + hash(token); }
    static String grantValue(SysUser user, StepUpAction action) {
        // A password/email change invalidates every previously issued grant automatically.
        return user.getUserId() + ":" + action + ":" + hash(user.getPassword() + "\n" + Objects.toString(user.getEmail(), ""));
    }
    static String identityCodeKey(SysUser user, StepUpAction action) {
        return PREFIX + "identity-code:" + user.getUserId() + ":" + action + ":" + hash(Objects.toString(user.getEmail(), ""));
    }
    static String newEmailCodeKey(SysUser user, String email) {
        return PREFIX + "new-email-code:" + user.getUserId() + ":" + hash(email);
    }
    private static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
