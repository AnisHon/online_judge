package com.anishan.user.service;

import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.file.FileOperation;
import com.anishan.api.util.AccountPolicy;
import com.anishan.commons.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserAvatarServiceTest {
    private final SysUserService users = mock(SysUserService.class);
    private final FileOperation storage = mock(FileOperation.class);
    private final UserAvatarService service = new UserAvatarService(users, storage);

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }
    private void login(String... permissions) {
        LoginUser user = new LoginUser();
        user.setUser(new SysUser().setUserId(1L));
        user.setAuths(List.of(permissions));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test void generalUserEditCannotResetAvatars() {
        login("user:user:edit");
        assertThrows(AccessDeniedException.class, () -> service.resetAvatar(42L));
        verifyNoInteractions(users, storage);
    }

    @Test void dedicatedPermissionCanModerateRestrictedAccounts() {
        login(AccountPolicy.AVATAR_RESET, AccountPolicy.AVATAR_DENY);
        when(users.existsId(42L)).thenReturn(true);
        service.resetAvatar(42L);
        verify(storage).deleteFile("avatar/42/default_avatar");
    }

    @Test void nonexistentTargetNeverDeletesStorage() {
        login(AccountPolicy.AVATAR_RESET);
        assertThrows(BusinessException.class, () -> service.resetAvatar(42L));
        verifyNoInteractions(storage);
    }

    @Test void failedDeletionMustNotReportSuccess() {
        login(AccountPolicy.AVATAR_RESET);
        when(users.existsId(42L)).thenReturn(true);
        when(storage.fileExists("avatar/42/default_avatar")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.resetAvatar(42L));
    }
}
