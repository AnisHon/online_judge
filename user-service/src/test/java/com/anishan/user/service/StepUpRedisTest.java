package com.anishan.user.service;

import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.enumeration.UserState;
import com.anishan.commons.exception.BusinessException;
import com.anishan.commons.util.JwtUtil;
import com.anishan.user.domain.dto.PasswordResetRequest;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.anishan.user.config.UserConfig;
import com.anishan.user.domain.dto.NewEmailCodeRequest;
import com.anishan.user.domain.dto.StepUpAction;
import com.anishan.user.domain.dto.StepUpEmailCodeRequest;
import com.anishan.user.domain.dto.StepUpVerifyRequest;
import com.anishan.user.domain.vo.StepUpVo;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Opt-in Redis checks. Only isolated keys for a synthetic user are touched; no DB or email is used. */
@EnabledIfEnvironmentVariable(named = "OJ_SECURITY_REDIS_TEST", matches = "true")
class StepUpRedisTest {
    LettuceConnectionFactory connection;
    StringRedisTemplate redis;
    StepUpService service;
    SysUserService users;
    PasswordEncoder encoder;
    SysUser user;
    final Set<String> grants = new HashSet<>();
    static final String EMAIL = "step-up-test@example.invalid";

    @BeforeAll static void mapperMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "redis-test"), SysUser.class);
    }

    @BeforeEach void setup() {
        connection = new LettuceConnectionFactory("127.0.0.1", 6379);
        connection.afterPropertiesSet();
        redis = new StringRedisTemplate(connection);
        long syntheticId = 8_000_000_000_000_000_000L + Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 100_000_000_000_000_000L);
        user = new SysUser().setUserId(syntheticId).setPassword("test-only-hash").setEmail(EMAIL).setStatus(UserState.NORMAL);
        LoginUser principal = new LoginUser(); principal.setUser(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));
        users = mock(SysUserService.class);
        when(users.getById(syntheticId)).thenReturn(user);
        encoder = mock(PasswordEncoder.class);
        when(encoder.matches("test-password", "test-only-hash")).thenReturn(true);
        AuthUtil auth = mock(AuthUtil.class);
        UserConfig config = new UserConfig();
        when(auth.checkAndRemoveCaptchaCode("test-captcha", "1", config.getCaptchaType())).thenReturn(true);
        EmailService email = mock(EmailService.class);
        when(email.sendEmailCode(anyString())).thenReturn("123456");
        service = new StepUpService(redis, users, encoder, auth, config, email);
    }

    @AfterEach void cleanup() {
        try {
            if (redis != null && user != null) {
                Set<String> keys = new HashSet<>(grants);
                String prefix = "user-service:step-up:";
                keys.add(prefix + "attempts:" + user.getUserId() + ":verify");
                keys.add(prefix + "attempts:" + user.getUserId() + ":new-email");
                keys.add(prefix + "mail-cooldown:" + user.getUserId() + ":identity");
                keys.add(prefix + "mail-cooldown:" + user.getUserId() + ":new-email");
                keys.add(StepUpService.identityCodeKey(user, StepUpAction.EMAIL));
                keys.add(StepUpService.newEmailCodeKey(user, "new@example.invalid"));
                redis.delete(keys);
            }
        } finally {
            SecurityContextHolder.clearContext();
            if (connection != null) connection.destroy();
        }
    }

    private StepUpVo grant(StepUpAction action) {
        StepUpVerifyRequest request = new StepUpVerifyRequest();
        request.setMethod(StepUpVerifyRequest.Method.PASSWORD); request.setAction(action); request.setPassword("test-password");
        StepUpVo result = service.verify(request);
        grants.add(StepUpService.grantKey(result.getStepUpToken()));
        return result;
    }

    @Test void grantHasTtlAndIsConsumedOnlyOnce() {
        StepUpVo grant = grant(StepUpAction.PASSWORD);
        Long ttl = redis.getExpire(StepUpService.grantKey(grant.getStepUpToken()));
        assertNotNull(ttl); assertTrue(ttl > 0 && ttl <= 300);
        service.consumePasswordGrant(user, grant.getStepUpToken());
        assertThrows(BusinessException.class, () -> service.consumePasswordGrant(user, grant.getStepUpToken()));
    }

    @Test void expiredGrantCannotAuthorizeMutation() {
        StepUpVo grant = grant(StepUpAction.PASSWORD);
        redis.expire(StepUpService.grantKey(grant.getStepUpToken()), 0, TimeUnit.SECONDS);
        assertThrows(BusinessException.class, () -> service.consumePasswordGrant(user, grant.getStepUpToken()));
    }

    @Test void passwordChangeRevokesEveryRefreshSessionAndLoginCache() {
        StepUpVo grant = grant(StepUpAction.PASSWORD);
        RefreshSessionService sessions = new RefreshSessionService(redis);
        String sessionId = UUID.randomUUID().toString();
        sessions.create(sessionId, user.getUserId(), "test-jti");
        grants.add("user-service:refresh-session:" + sessionId);
        grants.add("user-service:refresh-sessions:user:" + user.getUserId());
        String loginKey = "user-service:userId:" + user.getUserId();
        grants.add(loginKey);
        redis.opsForValue().set(loginKey, "test-only-login-cache", 60, TimeUnit.SECONDS);
        JwtUtil.RefreshClaims claims = new JwtUtil.RefreshClaims(user.getUserId(), sessionId, "test-jti");
        assertTrue(sessions.isActive(claims));
        when(users.update(any(Wrapper.class))).thenReturn(true);
        when(encoder.encode("new-password")).thenReturn("new-test-hash");
        PasswordResetRequest request = new PasswordResetRequest();
        request.setStepUpToken(grant.getStepUpToken()); request.setPassword("new-password");
        new AccountSecurityService(service, users, encoder, sessions, redis, mock(AuthUtil.class)).changePassword(request);
        assertFalse(sessions.isActive(claims));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(loginKey)));
    }

    @Test void onlyOneConcurrentConsumerCanProceed() throws Exception {
        StepUpVo grant = grant(StepUpAction.PASSWORD);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Boolean>> attempts = new ArrayList<>();
            for (int i = 0; i < 4; i++) attempts.add(() -> {
                try { service.consumePasswordGrant(user, grant.getStepUpToken()); return true; }
                catch (BusinessException expected) { return false; }
            });
            int successes = 0;
            for (Future<Boolean> result : executor.invokeAll(attempts)) if (result.get()) successes++;
            assertEquals(1, successes);
        } finally { executor.shutdownNow(); }
    }

    @Test void wrongUserCannotConsumeGrant() {
        StepUpVo grant = grant(StepUpAction.PASSWORD);
        SysUser other = new SysUser().setUserId(user.getUserId() - 1).setEmail(user.getEmail()).setPassword(user.getPassword());
        assertThrows(BusinessException.class, () -> service.consumePasswordGrant(other, grant.getStepUpToken()));
        service.consumePasswordGrant(user, grant.getStepUpToken());
    }

    @Test void wrongActionCannotConsumeGrant() {
        StepUpVo grant = grant(StepUpAction.EMAIL);
        assertThrows(BusinessException.class, () -> service.consumePasswordGrant(user, grant.getStepUpToken()));
        assertTrue(Boolean.TRUE.equals(redis.hasKey(StepUpService.grantKey(grant.getStepUpToken()))));
    }

    @Test void boundEmailOtpCannotBeReusedAndIsSeparateFromRegistrationCode() {
        StepUpEmailCodeRequest send = new StepUpEmailCodeRequest();
        send.setAction(StepUpAction.EMAIL); send.setCaptchaToken("test-captcha"); send.setCaptchaCode("1");
        service.sendIdentityCode(send);
        assertNotEquals("123456", redis.opsForValue().get(StepUpService.identityCodeKey(user, StepUpAction.EMAIL)));
        StepUpVerifyRequest verify = new StepUpVerifyRequest();
        verify.setAction(StepUpAction.EMAIL); verify.setMethod(StepUpVerifyRequest.Method.EMAIL); verify.setCode("123456");
        StepUpVo grant = service.verify(verify); grants.add(StepUpService.grantKey(grant.getStepUpToken()));
        assertThrows(BusinessException.class, () -> service.verify(verify));
    }

    @Test void newEmailCodeAndGrantAreConsumedAtomically() {
        StepUpVo grant = grant(StepUpAction.EMAIL);
        NewEmailCodeRequest send = new NewEmailCodeRequest();
        send.setStepUpToken(grant.getStepUpToken()); send.setNewEmail("new@example.invalid"); send.setCaptchaToken("test-captcha"); send.setCaptchaCode("1");
        service.sendNewEmailCode(send);
        assertThrows(BusinessException.class, () -> service.consumeEmailGrant(user, grant.getStepUpToken(), "new@example.invalid", "000000"));
        assertTrue(Boolean.TRUE.equals(redis.hasKey(StepUpService.grantKey(grant.getStepUpToken()))));
        assertTrue(Boolean.TRUE.equals(redis.hasKey(StepUpService.newEmailCodeKey(user, "new@example.invalid"))));
        service.consumeEmailGrant(user, grant.getStepUpToken(), "new@example.invalid", "123456");
        assertFalse(Boolean.TRUE.equals(redis.hasKey(StepUpService.grantKey(grant.getStepUpToken()))));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(StepUpService.newEmailCodeKey(user, "new@example.invalid"))));
        assertThrows(BusinessException.class, () -> service.consumeEmailGrant(user, grant.getStepUpToken(), "new@example.invalid", "123456"));
    }
}
