package com.anishan.user.service;

import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.enumeration.UserState;
import com.anishan.commons.exception.BusinessException;
import com.anishan.user.config.UserConfig;
import com.anishan.user.domain.dto.StepUpAction;
import com.anishan.user.domain.dto.StepUpVerifyRequest;
import com.anishan.user.domain.vo.StepUpVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StepUpServiceTest {
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> values;
    @Mock SysUserService users;
    @Mock PasswordEncoder encoder;
    @Mock AuthUtil authUtil;
    @Mock UserConfig userConfig;
    @Mock EmailService emailService;
    @InjectMocks StepUpService service;
    SysUser user;

    @BeforeEach void login() {
        user = new SysUser().setUserId(42L).setPassword("stored-password-hash").setEmail("old@example.com").setStatus(UserState.NORMAL);
        LoginUser principal = new LoginUser(); principal.setUser(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private StepUpVerifyRequest passwordRequest() {
        StepUpVerifyRequest request = new StepUpVerifyRequest();
        request.setAction(StepUpAction.PASSWORD); request.setMethod(StepUpVerifyRequest.Method.PASSWORD); request.setPassword("current-password");
        return request;
    }
    private void verificationDependencies() {
        when(users.getById(42L)).thenReturn(user);
        when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);
    }

    @Test void wrongPasswordDoesNotCreateGrant() {
        verificationDependencies();
        when(encoder.matches("current-password", "stored-password-hash")).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.verify(passwordRequest()));
        verify(redis, never()).opsForValue();
    }
    @Test void passwordVerificationCreatesOpaqueShortLivedGrant() {
        verificationDependencies();
        when(encoder.matches("current-password", "stored-password-hash")).thenReturn(true);
        when(redis.opsForValue()).thenReturn(values);
        StepUpVo grant = service.verify(passwordRequest());
        assertEquals(43, grant.getStepUpToken().length());
        assertEquals(300, grant.getExpiresIn());
        String stored = StepUpService.grantValue(user, StepUpAction.PASSWORD);
        assertFalse(stored.contains("stored-password-hash"));
        assertFalse(stored.contains("old@example.com"));
        verify(values).set(StepUpService.grantKey(grant.getStepUpToken()), stored, 300L, TimeUnit.SECONDS);
    }
    @Test void noEmailCannotBeUsedAsIdentityProof() {
        verificationDependencies(); user.setEmail(null);
        StepUpVerifyRequest request = passwordRequest(); request.setMethod(StepUpVerifyRequest.Method.EMAIL); request.setCode("123456");
        assertThrows(BusinessException.class, () -> service.verify(request));
        verify(redis, never()).opsForValue();
    }
    @Test void expiredForgedAndCrossPurposeGrantsAreRejected() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(StepUpService.grantKey("token"))).thenReturn(null, StepUpService.grantValue(user, StepUpAction.EMAIL));
        assertThrows(BusinessException.class, () -> service.consumePasswordGrant(user, "token"));
        assertThrows(BusinessException.class, () -> service.consumePasswordGrant(user, "token"));
        verify(redis, never()).execute(any(RedisScript.class), anyList(), anyString());
    }
    @Test void grantForAnotherUserIsRejected() {
        when(redis.opsForValue()).thenReturn(values);
        SysUser other = new SysUser().setUserId(99L).setPassword(user.getPassword()).setEmail(user.getEmail());
        when(values.get(StepUpService.grantKey("token"))).thenReturn(StepUpService.grantValue(other, StepUpAction.PASSWORD));
        assertThrows(BusinessException.class, () -> service.consumePasswordGrant(user, "token"));
    }
    @Test void changingCredentialsInvalidatesOutstandingGrant() {
        when(redis.opsForValue()).thenReturn(values);
        String before = StepUpService.grantValue(user, StepUpAction.PASSWORD);
        user.setEmail("new@example.com");
        when(values.get(StepUpService.grantKey("token"))).thenReturn(before);
        assertThrows(BusinessException.class, () -> service.consumePasswordGrant(user, "token"));
    }
    @Test void tooManyVerificationAttemptsAreRejectedBeforeCheckingPassword() {
        when(users.getById(42L)).thenReturn(user);
        when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(6L);
        assertThrows(BusinessException.class, () -> service.verify(passwordRequest()));
        verifyNoInteractions(encoder);
    }
}
