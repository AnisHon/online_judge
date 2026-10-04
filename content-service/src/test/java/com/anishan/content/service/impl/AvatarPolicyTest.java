package com.anishan.content.service.impl;

import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.file.FileOperation;
import com.anishan.api.util.AccountPolicy;
import com.anishan.content.service.FileInfoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AvatarPolicyTest {
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    private void login(String... permissions) {
        LoginUser user = new LoginUser();
        user.setUser(new SysUser().setUserId(42L));
        user.setAuths(List.of(permissions));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test void noPositiveGrantIsNeededButDenyWinsOverManagementGrants() {
        login();
        assertEquals(42L, AccountPolicy.requireAllowed(AccountPolicy.AVATAR_DENY));
        login(AccountPolicy.AVATAR_DENY, AccountPolicy.AVATAR_RESET, "system:backend:access");
        assertThrows(AccessDeniedException.class, () -> AccountPolicy.requireAllowed(AccountPolicy.AVATAR_DENY));
        assertDoesNotThrow(() -> AccountPolicy.requireAuthority(AccountPolicy.AVATAR_RESET));
    }

    @Test void serviceRejectsDeniedUploadBeforeReadingOrWritingStorage() {
        FileOperation storage = mock(FileOperation.class);
        FileServiceImpl service = new FileServiceImpl(mock(FileInfoService.class), storage);
        login(AccountPolicy.AVATAR_DENY);
        assertThrows(AccessDeniedException.class, () -> service.uploadAvatar(
                new MockMultipartFile("avatar", new byte[]{1}), 42L));
        verifyNoInteractions(storage);
    }

    @Test void noSessionOrAnotherActorCannotWriteAvatar() {
        assertThrows(AccessDeniedException.class, () -> AccountPolicy.requireAllowed(AccountPolicy.AVATAR_DENY));
        login();
        assertThrows(AccessDeniedException.class, () -> AccountPolicy.requireAllowedActor(AccountPolicy.AVATAR_DENY, 43L));
    }
}
