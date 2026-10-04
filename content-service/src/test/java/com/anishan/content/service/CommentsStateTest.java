package com.anishan.content.service;

import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.util.AccountPolicy;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.dto.CommentsStateRequest;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.SolutionCommentMapper;
import com.anishan.content.mapper.SolutionExplanationMapper;
import com.anishan.content.mapper.SolutionModerationActionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommentsStateTest {
    private static final long AUTHOR = 101L;
    private static final long MANAGER = 202L;
    private static final long SOLUTION = 2098755579163770881L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    private SolutionExplanationMapper solutions;
    private SolutionDomainMigrationGate migrationGate;
    private CommentModerationService service;

    @BeforeEach
    void setUp() {
        login(AUTHOR);
        solutions = mock(SolutionExplanationMapper.class);
        migrationGate = mock(SolutionDomainMigrationGate.class);
        service = new CommentModerationService(mock(SolutionCommentMapper.class), solutions,
                mock(SolutionModerationActionMapper.class), mock(ContentEventOutboxService.class),
                migrationGate, CLOCK);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authorClosesAndReopensCommentsUnderTheSolutionLock() {
        SolutionRecord open = solution(AUTHOR, false, true);
        SolutionRecord closed = solution(AUTHOR, false, false);
        when(solutions.selectCommentTargetForUpdate(SOLUTION)).thenReturn(open, closed, closed);
        when(solutions.setCommentsOpen(SOLUTION, false)).thenReturn(1);
        when(solutions.setCommentsOpen(SOLUTION, true)).thenReturn(1);

        assertTrue(service.setCommentsOpen(SOLUTION, new CommentsStateRequest().setOpen(false)));
        assertTrue(service.setCommentsOpen(SOLUTION, new CommentsStateRequest().setOpen(false)));
        assertTrue(service.setCommentsOpen(SOLUTION, new CommentsStateRequest().setOpen(true)));

        verify(solutions, times(3)).selectCommentTargetForUpdate(SOLUTION);
        verify(solutions, times(1)).setCommentsOpen(SOLUTION, false);
        verify(solutions, times(1)).setCommentsOpen(SOLUTION, true);
        verifyNoMoreInteractions(solutions);
    }

    @Test
    void nonOwnerNeedsEditPermissionButNegativeSolutionPolicyStillTakesPrecedence() {
        when(solutions.selectCommentTargetForUpdate(SOLUTION)).thenReturn(solution(AUTHOR, false, true));
        login(MANAGER);
        assertEquals(404, assertThrows(ApiStatusException.class,
                () -> service.setCommentsOpen(SOLUTION, new CommentsStateRequest().setOpen(false))).getStatusCode());
        verify(solutions).selectCommentTargetForUpdate(SOLUTION);
        verify(solutions, never()).setCommentsOpen(anyLong(), anyBoolean());

        login(MANAGER, "problem:solution:edit", AccountPolicy.SOLUTION_DENY);
        assertThrows(AccessDeniedException.class,
                () -> service.setCommentsOpen(SOLUTION, new CommentsStateRequest().setOpen(false)));
        verifyNoMoreInteractions(solutions);
    }

    @Test
    void authorAlsoCannotBypassTheNegativeSolutionPolicy() {
        login(AUTHOR, AccountPolicy.SOLUTION_DENY);
        assertThrows(AccessDeniedException.class,
                () -> service.setCommentsOpen(SOLUTION, new CommentsStateRequest().setOpen(false)));
        verifyNoInteractions(solutions);
    }

    @Test
    void missingOrDeletedSolutionAndInvalidRequestDoNotChangeState() {
        when(solutions.selectCommentTargetForUpdate(SOLUTION)).thenReturn(null,
                solution(AUTHOR, true, true));
        assertEquals(404, assertThrows(ApiStatusException.class,
                () -> service.setCommentsOpen(SOLUTION, new CommentsStateRequest().setOpen(false))).getStatusCode());
        assertEquals(404, assertThrows(ApiStatusException.class,
                () -> service.setCommentsOpen(SOLUTION, new CommentsStateRequest().setOpen(false))).getStatusCode());
        assertEquals(400, assertThrows(ApiStatusException.class,
                () -> service.setCommentsOpen(SOLUTION, null)).getStatusCode());
        assertEquals(400, assertThrows(ApiStatusException.class,
                () -> service.setCommentsOpen(SOLUTION, new CommentsStateRequest())).getStatusCode());
        verify(solutions, times(2)).selectCommentTargetForUpdate(SOLUTION);
        verify(solutions, never()).setCommentsOpen(anyLong(), anyBoolean());
    }

    @Test
    void stateChangeUsesTransactionalSolutionRowLockAlsoUsedByCommentWriters() throws Exception {
        when(solutions.selectCommentTargetForUpdate(SOLUTION)).thenReturn(solution(AUTHOR, false, true));
        when(solutions.setCommentsOpen(SOLUTION, false)).thenReturn(1);
        assertTrue(service.setCommentsOpen(SOLUTION, new CommentsStateRequest().setOpen(false)));

        assertNotNull(CommentModerationService.class.getMethod("setCommentsOpen", Long.class,
                CommentsStateRequest.class).getAnnotation(Transactional.class));
        // CommentCreationService takes this same selectCommentTargetForUpdate lock before checking commentsOpen.
        assertNotNull(CommentCreationService.class.getMethod("createRoot", Long.class, Long.class, Long.class,
                Long.class, com.anishan.api.client.problem.domain.vo.ContentProblemReadVo.class,
                String.class, String.class));
        verify(solutions).selectCommentTargetForUpdate(SOLUTION);
        verify(solutions).setCommentsOpen(SOLUTION, false);
        verify(migrationGate).requireCutover();
    }

    private SolutionRecord solution(Long author, boolean deleted, boolean commentsOpen) {
        SolutionRecord row = new SolutionRecord();
        row.setSolutionId(SOLUTION);
        row.setUserId(author);
        row.setDelFlag(deleted);
        row.setCommentsOpen(commentsOpen);
        return row;
    }

    private void login(long userId, String... permissions) {
        LoginUser user = new LoginUser();
        user.setUser(new SysUser().setUserId(userId));
        user.setAuths(Arrays.asList(permissions));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
