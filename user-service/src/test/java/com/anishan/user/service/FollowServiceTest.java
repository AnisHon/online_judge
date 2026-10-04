package com.anishan.user.service;

import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.user.controller.FollowController;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.user.domain.dto.FollowPageQuery;
import com.anishan.user.domain.vo.FollowSummaryVo;
import com.anishan.user.mapper.UserFollowMapper;
import com.anishan.user.service.impl.FollowServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private UserFollowMapper userFollowMapper;
    @Mock
    private SysUserService sysUserService;

    private FollowService followService;

    @BeforeEach
    void setUp() {
        followService = new FollowServiceImpl(userFollowMapper, sysUserService);
        authenticate(100L);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsSelfAndNonPositiveTargetWithoutWriting() {
        assertEquals(400, assertThrows(ApiStatusException.class, () -> followService.follow(100L)).getStatusCode());
        assertEquals(400, assertThrows(ApiStatusException.class, () -> followService.follow(0L)).getStatusCode());
        verifyNoInteractions(userFollowMapper, sysUserService);
    }

    @Test
    void rootAccountCannotCreateOrRemoveFollowRelationshipsAndNoManagementPermissionIsRequired() {
        authenticate(0L);
        assertEquals(400, assertThrows(ApiStatusException.class, () -> followService.follow(200L)).getStatusCode());
        assertEquals(400, assertThrows(ApiStatusException.class, () -> followService.unfollow(200L)).getStatusCode());
        PreAuthorize access = FollowController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(access);
        assertEquals("isAuthenticated()", access.value());
        verifyNoInteractions(userFollowMapper, sysUserService);
    }

    @Test
    void missingDeletedOrBannedTargetCannotBeFollowed() {
        when(sysUserService.isFollowableUser(200L)).thenReturn(false);
        when(sysUserService.isFollowableUser(201L)).thenReturn(false);
        when(sysUserService.isFollowableUser(202L)).thenReturn(false);

        assertEquals(404, assertThrows(ApiStatusException.class, () -> followService.follow(200L)).getStatusCode());
        assertEquals(404, assertThrows(ApiStatusException.class, () -> followService.follow(201L)).getStatusCode());
        assertEquals(404, assertThrows(ApiStatusException.class, () -> followService.follow(202L)).getStatusCode());
        verify(userFollowMapper, never()).insert(any());
    }

    @Test
    void duplicatePutReturnsActualRelationshipAndCurrentCounts() {
        AtomicInteger inserts = new AtomicInteger();
        doAnswer(invocation -> {
            if (inserts.incrementAndGet() == 1) return 1;
            throw new DuplicateKeyException("composite follow key already exists");
        }).when(userFollowMapper).insert(any());
        when(sysUserService.isFollowableUser(200L)).thenReturn(true);
        when(userFollowMapper.exists(100L, 200L)).thenReturn(true);
        when(userFollowMapper.countFollowers(200L)).thenReturn(7L);
        when(userFollowMapper.countFollowing(200L)).thenReturn(9L);

        FollowSummaryVo first = followService.follow(200L);
        FollowSummaryVo repeated = followService.follow(200L);

        assertTrue(first.isFollowing());
        assertTrue(repeated.isFollowing());
        assertEquals(7L, repeated.getFollowersCount());
        assertEquals(9L, repeated.getFollowingCount());
        verify(userFollowMapper, times(2)).insert(any());
    }

    @Test
    void repeatedDeleteIsIdempotentAndDoesNotDecrementCountersLocally() {
        when(sysUserService.isVisibleUser(200L)).thenReturn(true);
        when(userFollowMapper.exists(100L, 200L)).thenReturn(false);
        when(userFollowMapper.countFollowers(200L)).thenReturn(3L);
        when(userFollowMapper.countFollowing(200L)).thenReturn(4L);

        FollowSummaryVo first = followService.unfollow(200L);
        FollowSummaryVo repeated = followService.unfollow(200L);

        assertFalse(first.isFollowing());
        assertFalse(repeated.isFollowing());
        assertEquals(3L, repeated.getFollowersCount());
        assertEquals(4L, repeated.getFollowingCount());
        verify(userFollowMapper, times(2)).deleteByFollowerAndFollowee(100L, 200L);
        verify(sysUserService, never()).isFollowableUser(200L);
    }

    @Test
    void invalidPageSizeIsRejectedBeforeRunningQueries() {
        when(sysUserService.isVisibleUser(200L)).thenReturn(true);
        FollowPageQuery query = new FollowPageQuery();
        query.setPageSize(51L);

        assertEquals(400, assertThrows(ApiStatusException.class,
                () -> followService.getFollowing(200L, query)).getStatusCode());
        verifyNoInteractions(userFollowMapper);
    }

    @Test
    void simultaneousPutsAreResolvedByCompositeKeyAndBothReturnFollowing() throws Exception {
        AtomicBoolean rowExists = new AtomicBoolean(false);
        when(sysUserService.isFollowableUser(200L)).thenReturn(true);
        doAnswer(invocation -> {
            if (rowExists.compareAndSet(false, true)) return 1;
            throw new DuplicateKeyException("concurrent composite-key insert");
        }).when(userFollowMapper).insert(any());
        when(userFollowMapper.exists(100L, 200L)).thenReturn(true);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<FollowSummaryVo> request = () -> {
                authenticate(100L);
                return followService.follow(200L);
            };
            Future<FollowSummaryVo> first = executor.submit(request);
            Future<FollowSummaryVo> second = executor.submit(request);
            assertTrue(first.get(5, TimeUnit.SECONDS).isFollowing());
            assertTrue(second.get(5, TimeUnit.SECONDS).isFollowing());
            assertTrue(rowExists.get());
            verify(userFollowMapper, times(2)).insert(any());
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static void authenticate(Long userId) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
        SecurityContextHolder.setContext(context);
    }
}
