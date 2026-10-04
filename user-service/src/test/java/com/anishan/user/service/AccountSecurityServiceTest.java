package com.anishan.user.service;

import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.exception.BusinessException;
import com.anishan.user.domain.dto.EmailResetRequest;
import com.anishan.user.domain.dto.PasswordResetRequest;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountSecurityServiceTest {
    @Mock StepUpService stepUp;
    @Mock SysUserService users;
    @Mock PasswordEncoder encoder;
    @Mock RefreshSessionService refreshSessions;
    @Mock StringRedisTemplate redis;
    @Mock AuthUtil authUtil;
    @InjectMocks AccountSecurityService service;
    SysUser user;
    PasswordResetRequest password;

    @BeforeAll static void initMapperMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "unit-test"), SysUser.class);
    }

    @BeforeEach void setup() {
        user = new SysUser().setUserId(42L).setPassword("old-hash").setEmail("old@example.com");
        password = new PasswordResetRequest(); password.setStepUpToken("grant"); password.setPassword("new-password");
        when(stepUp.currentUser()).thenReturn(user);
    }
    @Test void passwordMutationRequiresProofAndRevokesAllSessions() {
        when(users.update(any(Wrapper.class))).thenReturn(true);
        when(encoder.encode("new-password")).thenReturn("new-hash");
        service.changePassword(password);
        verify(stepUp).consumePasswordGrant(user, "grant");
        verify(refreshSessions).revokeAll(42L);
        verify(redis).delete("user-service:userId:42");
    }
    @Test void invalidGrantDoesNotUpdateDatabase() {
        doThrow(new BusinessException("invalid proof")).when(stepUp).consumePasswordGrant(user, "grant");
        assertThrows(BusinessException.class, () -> service.changePassword(password));
        verifyNoInteractions(users, refreshSessions);
    }
    @Test void concurrentCredentialChangeFailsWithoutRevokingSessions() {
        when(encoder.encode("new-password")).thenReturn("new-hash");
        when(users.update(any(Wrapper.class))).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.changePassword(password));
        verifyNoInteractions(refreshSessions);
    }
    @Test void newEmailRequiresTargetEmailProofBeforeDatabaseMutation() {
        EmailResetRequest email = new EmailResetRequest(); email.setStepUpToken("grant"); email.setNewEmail("new@example.com"); email.setNewEmailCode("123456");
        doThrow(new BusinessException("invalid email proof")).when(stepUp).consumeEmailGrant(user, "grant", "new@example.com", "123456");
        assertThrows(BusinessException.class, () -> service.changeEmail(email));
        verifyNoInteractions(users, refreshSessions);
    }
}
