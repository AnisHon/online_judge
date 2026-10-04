package com.anishan.user.service;

import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.user.domain.dto.EmailResetRequest;
import com.anishan.user.domain.dto.PasswordResetRequest;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Account mutations use the same verification service; login remains independent. */
@Service
@RequiredArgsConstructor
public class AccountSecurityService {
    private final StepUpService stepUp;
    private final SysUserService users;
    private final PasswordEncoder encoder;
    private final RefreshSessionService refreshSessions;
    private final StringRedisTemplate redis;
    private final AuthUtil authUtil;

    @Transactional
    public void changePassword(PasswordResetRequest request) {
        SysUser user = stepUp.currentUser();
        ThrowUtil.businessError(encoder.matches(request.getPassword(), user.getPassword()), "新密码不能与当前密码相同");
        stepUp.consumePasswordGrant(user, request.getStepUpToken());
        ThrowUtil.businessError(!users.update(securityCondition(user).set(SysUser::getPassword, encoder.encode(request.getPassword()))),
                "账号信息已变更，请重新验证身份");
        revokeSessionsAfterCommit(user.getUserId());
    }

    @Transactional
    public void changeEmail(EmailResetRequest request) {
        SysUser user = stepUp.currentUser();
        String email = StepUpService.normalizeEmail(request.getNewEmail());
        stepUp.checkNewEmail(user, email);
        stepUp.consumeEmailGrant(user, request.getStepUpToken(), email, request.getNewEmailCode());
        try {
            ThrowUtil.businessError(!users.update(securityCondition(user).set(SysUser::getEmail, email)),
                    "账号信息已变更，请重新验证身份");
        } catch (DuplicateKeyException duplicate) {
            ThrowUtil.businessError(true, "邮箱已经被使用");
        }
        afterCommit(() -> authUtil.cacheLoginUser(users.getLoginUser(user.getUserId())));
    }

    private LambdaUpdateWrapper<SysUser> securityCondition(SysUser user) {
        LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, user.getUserId()).eq(SysUser::getPassword, user.getPassword());
        if (user.getEmail() == null) update.isNull(SysUser::getEmail);
        else update.eq(SysUser::getEmail, user.getEmail());
        return update;
    }

    public void revokeSessionsAfterCommit(Long userId) {
        afterCommit(() -> {
            refreshSessions.revokeAll(userId);
            redis.delete("user-service:userId:" + userId);
        });
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
    }
}
