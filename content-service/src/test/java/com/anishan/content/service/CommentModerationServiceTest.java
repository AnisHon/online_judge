package com.anishan.content.service;

import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.api.util.AccountPolicy;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.dto.AdminCommentPageQuery;
import com.anishan.content.domain.enumeration.CommentState;
import com.anishan.content.domain.entity.SolutionComment;
import com.anishan.content.domain.entity.SolutionModerationAction;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.SolutionCommentMapper;
import com.anishan.content.mapper.SolutionExplanationMapper;
import com.anishan.content.mapper.SolutionModerationActionMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommentModerationServiceTest {
    private static final long AUTHOR = 101L;
    private static final long MODERATOR = 202L;
    private static final long SOLUTION = 2098755579163770881L;
    private static final long ROOT = 9001L;
    private static final long REPLY = 9002L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    private SolutionCommentMapper comments;
    private SolutionExplanationMapper solutions;
    private SolutionModerationActionMapper actions;
    private ContentEventOutboxService outbox;
    private SolutionDomainMigrationGate migrationGate;
    private CommentModerationService service;

    @BeforeEach
    void setUp() {
        login(MODERATOR);
        comments = mock(SolutionCommentMapper.class);
        solutions = mock(SolutionExplanationMapper.class);
        actions = mock(SolutionModerationActionMapper.class);
        outbox = mock(ContentEventOutboxService.class);
        migrationGate = mock(SolutionDomainMigrationGate.class);
        service = new CommentModerationService(comments, solutions, actions, outbox, migrationGate, CLOCK);
        when(solutions.selectCommentTargetForUpdate(SOLUTION)).thenReturn(solution(AUTHOR, false, true));
        when(comments.markVisibleDeleted(anyLong(), eq(SOLUTION), anyString(), anyLong(), any()))
                .thenReturn(1);
        when(solutions.adjustCommentCount(SOLUTION, -1)).thenReturn(1);
        when(comments.decrementRootReplyCount(ROOT)).thenReturn(1);
        when(actions.insert(any(SolutionModerationAction.class))).thenAnswer(invocation -> {
            ((SolutionModerationAction) invocation.getArgument(0)).setActionId(901L);
            return 1;
        });
        when(outbox.recordCommunity(any(CommunityEvent.class))).thenAnswer(invocation -> {
            CommunityEvent event = invocation.getArgument(0);
            event.validate();
            return event.getEventId();
        });
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authorCanDeleteOwnCommentEvenWhenCommentCreationIsDenied() {
        login(AUTHOR, AccountPolicy.COMMENT_DENY);
        SolutionComment root = root(ROOT, AUTHOR, CommentState.VISIBLE);
        when(comments.selectReplyTarget(ROOT)).thenReturn(root);
        when(comments.selectRootForUpdate(ROOT, SOLUTION)).thenReturn(root);

        assertTrue(service.deleteOwn(ROOT));

        verify(comments).markVisibleDeleted(eq(ROOT), eq(SOLUTION), eq("AUTHOR_DELETED"),
                eq(AUTHOR), eq(LocalDateTime.now(CLOCK)));
        verify(solutions).adjustCommentCount(SOLUTION, -1);
        verify(comments, never()).decrementRootReplyCount(anyLong());
        verifyNoInteractions(actions, outbox);
    }

    @Test
    void thirdPartyCannotDeleteAnotherAuthorsComment() {
        SolutionComment root = root(ROOT, AUTHOR, CommentState.VISIBLE);
        when(comments.selectReplyTarget(ROOT)).thenReturn(root);
        when(comments.selectRootForUpdate(ROOT, SOLUTION)).thenReturn(root);

        ApiStatusException error = assertThrows(ApiStatusException.class, () -> service.deleteOwn(ROOT));

        assertEquals(404, error.getStatusCode());
        verify(comments, never()).markVisibleDeleted(anyLong(), anyLong(), anyString(), anyLong(), any());
        verifyNoInteractions(actions, outbox);
    }

    @Test
    void deletingReplyLocksSolutionThenRootThenTargetAndUpdatesBothVisibleCounters() {
        login(AUTHOR);
        SolutionComment reply = reply(REPLY, AUTHOR, CommentState.VISIBLE);
        SolutionComment deletedRoot = root(ROOT, AUTHOR, CommentState.ADMIN_DELETED);
        when(comments.selectReplyTarget(REPLY)).thenReturn(reply);
        when(comments.selectRootForUpdate(ROOT, SOLUTION)).thenReturn(deletedRoot);
        when(comments.selectReplyParentForUpdate(REPLY, SOLUTION)).thenReturn(reply);

        assertTrue(service.deleteOwn(REPLY));

        org.mockito.InOrder order = inOrder(solutions, comments);
        order.verify(solutions).selectCommentTargetForUpdate(SOLUTION);
        order.verify(comments).selectRootForUpdate(ROOT, SOLUTION);
        order.verify(comments).selectReplyParentForUpdate(REPLY, SOLUTION);
        verify(solutions).adjustCommentCount(SOLUTION, -1);
        verify(comments).decrementRootReplyCount(ROOT);
        verify(comments).markVisibleDeleted(REPLY, SOLUTION, "AUTHOR_DELETED", AUTHOR,
                LocalDateTime.now(CLOCK));
        verifyNoInteractions(actions, outbox);
    }

    @Test
    void deletingAlreadyDeletedRootIsIdempotentAndDoesNotTouchCountsAuditOrOutbox() {
        login(AUTHOR);
        SolutionComment deleted = root(ROOT, AUTHOR, CommentState.AUTHOR_DELETED);
        when(comments.selectReplyTarget(ROOT)).thenReturn(deleted);
        when(comments.selectRootForUpdate(ROOT, SOLUTION)).thenReturn(deleted);

        assertTrue(service.deleteOwn(ROOT));

        verify(comments, never()).markVisibleDeleted(anyLong(), anyLong(), anyString(), anyLong(), any());
        verify(solutions, never()).adjustCommentCount(anyLong(), anyInt());
        verify(comments, never()).decrementRootReplyCount(anyLong());
        verifyNoInteractions(actions, outbox);
    }

    @Test
    void repeatedAdminDeleteProducesOnlyOneCountChangeAuditAndNotification() {
        login(MODERATOR, "problem:comment:remove");
        SolutionComment visible = root(ROOT, AUTHOR, CommentState.VISIBLE);
        SolutionComment deleted = root(ROOT, AUTHOR, CommentState.ADMIN_DELETED);
        when(comments.selectReplyTarget(ROOT)).thenReturn(visible, deleted);
        when(comments.selectRootForUpdate(ROOT, SOLUTION)).thenReturn(visible, deleted);

        assertTrue(service.deleteAsAdmin(ROOT, "policy reason"));
        assertTrue(service.deleteAsAdmin(ROOT, "duplicate request"));

        verify(comments, times(1)).markVisibleDeleted(eq(ROOT), eq(SOLUTION), eq("ADMIN_DELETED"),
                eq(MODERATOR), any());
        verify(solutions, times(1)).adjustCommentCount(SOLUTION, -1);
        verify(actions, times(1)).insert(any(SolutionModerationAction.class));
        verify(outbox, times(1)).recordCommunity(any(CommunityEvent.class));
    }

    @Test
    void adminDeletionStoresLiteralReasonAndAtomicallyRecordsAuthorEvent() {
        login(AUTHOR, "problem:comment:remove"); // Admin may moderate their own comment; recipient is retained.
        SolutionComment root = root(ROOT, AUTHOR, CommentState.VISIBLE);
        when(comments.selectReplyTarget(ROOT)).thenReturn(root);
        when(comments.selectRootForUpdate(ROOT, SOLUTION)).thenReturn(root);

        assertTrue(service.deleteAsAdmin(ROOT, "  <b>review</b>\r\nreason  "));

        ArgumentCaptor<SolutionModerationAction> actionCapture =
                ArgumentCaptor.forClass(SolutionModerationAction.class);
        verify(actions).insert(actionCapture.capture());
        SolutionModerationAction action = actionCapture.getValue();
        assertEquals("COMMENT", action.getTargetType());
        assertEquals(ROOT, action.getTargetId());
        assertEquals(AUTHOR, action.getAuthorId());
        assertEquals(AUTHOR, action.getOperatorId());
        assertEquals("DELETE", action.getAction());
        assertEquals("<b>review</b>\nreason", action.getReason());

        ArgumentCaptor<CommunityEvent> eventCapture = ArgumentCaptor.forClass(CommunityEvent.class);
        verify(outbox).recordCommunity(eventCapture.capture());
        CommunityEvent event = eventCapture.getValue();
        assertEquals(CommunityEventType.COMMENT_MODERATED, event.getEventType());
        assertEquals("comment-moderated:901", event.getDedupeKey());
        assertEquals(Collections.singletonList(String.valueOf(AUTHOR)), event.getRecipientIds());
        assertEquals("DELETE", event.getAction());
        assertEquals("<b>review</b>\nreason", event.getReason());
        verify(solutions).adjustCommentCount(SOLUTION, -1);
    }

    @Test
    void adminReasonIsRequiredBoundedAndControlFreeButHtmlRemainsLiteral() {
        login(MODERATOR, "problem:comment:remove");
        for (String reason : Arrays.asList(null, "", "  ", "x".repeat(501), "bad\u0000reason",
                Character.toString((char) 0xD800))) {
            assertEquals(400, assertThrows(ApiStatusException.class,
                    () -> service.deleteAsAdmin(ROOT, reason)).getStatusCode());
        }
        verifyNoInteractions(comments, solutions, actions, outbox);
    }

    @Test
    void adminDeleteRequiresDedicatedPermissionAndSolutionDenyPolicyStillWins() {
        login(MODERATOR);
        assertThrows(AccessDeniedException.class, () -> service.deleteAsAdmin(ROOT, "reason"));
        login(MODERATOR, "problem:comment:remove", AccountPolicy.SOLUTION_DENY);
        assertThrows(AccessDeniedException.class, () -> service.deleteAsAdmin(ROOT, "reason"));
        verifyNoInteractions(comments, solutions, actions, outbox);
    }

    @Test
    void authorHistoryIsBoundToCommentOwnerAndAdminPageIsBoundedAndBatched() {
        SolutionComment root = root(ROOT, AUTHOR, CommentState.ADMIN_DELETED);
        when(comments.selectReplyTarget(ROOT)).thenReturn(root);
        when(actions.selectOwnCommentActions(ROOT, AUTHOR)).thenReturn(Collections.emptyList());
        login(AUTHOR);
        assertTrue(service.ownActions(ROOT).isEmpty());
        verify(actions).selectOwnCommentActions(ROOT, AUTHOR);

        login(MODERATOR, "problem:comment:list");
        SolutionComment row = root(ROOT, AUTHOR, CommentState.ADMIN_DELETED);
        when(comments.countAdminComments(SOLUTION, "ADMIN_DELETED")).thenReturn(1L);
        when(comments.selectAdminComments(SOLUTION, "ADMIN_DELETED", 0L, 20L))
                .thenReturn(Collections.singletonList(row));
        SolutionModerationAction audit = new SolutionModerationAction().setActionId(901L)
                .setTargetId(ROOT).setAction("DELETE").setReason("reason").setOperatorId(MODERATOR)
                .setCreatedAt(LocalDateTime.now(CLOCK));
        when(actions.selectCommentActionsByTargets(Collections.singletonList(ROOT)))
                .thenReturn(Collections.singletonList(audit));

        AdminCommentPageQuery query = new AdminCommentPageQuery();
        query.setSolutionId(SOLUTION);
        query.setState(CommentState.ADMIN_DELETED);
        var page = service.pageAdmin(query);
        assertEquals(1L, page.getTotalRecords());
        assertEquals("preserved body", page.getData().get(0).getContent());
        assertEquals("reason", page.getData().get(0).getReason());
        assertEquals(MODERATOR, page.getData().get(0).getOperatorId());

        query.setPageSize(51L);
        assertEquals(400, assertThrows(ApiStatusException.class, () -> service.pageAdmin(query)).getStatusCode());
    }

    @Test
    void mapperUsesAtomicTombstoneAndCommentHistoryIncludesCommentTargets() throws Exception {
        Configuration configuration = new Configuration();
        parseMapper(configuration, "/mapper/SolutionCommentMapper.xml");
        parseMapper(configuration, "/mapper/SolutionModerationActionMapper.xml");
        BoundSql deleted = configuration.getMappedStatement(
                SolutionCommentMapper.class.getName() + ".markVisibleDeleted")
                .getBoundSql(Map.of("commentId", ROOT, "solutionId", SOLUTION,
                        "newState", "ADMIN_DELETED", "operatorId", MODERATOR,
                        "now", LocalDateTime.now(CLOCK)));
        String deleteSql = normalize(deleted);
        assertTrue(deleteSql.contains("state=?"), deleteSql);
        assertTrue(deleteSql.contains("deleted_by=?"), deleteSql);
        assertTrue(deleteSql.contains("deleted_at=?"), deleteSql);

        BoundSql ownHistory = configuration.getMappedStatement(
                SolutionModerationActionMapper.class.getName() + ".selectOwnCommentActions")
                .getBoundSql(Map.of("commentId", ROOT, "authorId", AUTHOR));
        String ownerSql = normalize(ownHistory);
        assertTrue(ownerSql.contains("target_type='comment'"), ownerSql);
        assertTrue(ownerSql.contains("author_id=?"), ownerSql);
        assertFalse(ownerSql.contains("operator_id"), ownerSql);

        BoundSql history = configuration.getMappedStatement(
                SolutionModerationActionMapper.class.getName() + ".selectHistory")
                .getBoundSql(Map.of("solutionId", SOLUTION, "offset", 0L, "limit", 20L));
        String adminSql = normalize(history);
        assertTrue(adminSql.contains("where solution_id = ?"), adminSql);
        assertFalse(adminSql.substring(adminSql.indexOf(" where ")).contains("target_type"), adminSql);
    }

    @Test
    void deleteAndAdminModerationAreTransactional() throws Exception {
        assertNotNull(CommentModerationService.class.getMethod("deleteOwn", Long.class)
                .getAnnotation(Transactional.class));
        assertNotNull(CommentModerationService.class.getMethod("deleteAsAdmin", Long.class, String.class)
                .getAnnotation(Transactional.class));
    }

    private void parseMapper(Configuration configuration, String resource) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private String normalize(BoundSql sql) {
        return sql.getSql().replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private SolutionRecord solution(Long author, boolean deleted, boolean commentsOpen) {
        SolutionRecord row = new SolutionRecord();
        row.setSolutionId(SOLUTION);
        row.setUserId(author);
        row.setDelFlag(deleted);
        row.setCommentsOpen(commentsOpen);
        return row;
    }

    private SolutionComment root(Long id, Long author, CommentState state) {
        return new SolutionComment().setCommentId(id).setSolutionId(SOLUTION).setUserId(author)
                .setRootId(null).setParentId(null).setContent("preserved body").setState(state)
                .setLikeCount(0L).setReplyCount(3L).setCreatedAt(LocalDateTime.now(CLOCK));
    }

    private SolutionComment reply(Long id, Long author, CommentState state) {
        return new SolutionComment().setCommentId(id).setSolutionId(SOLUTION).setUserId(author)
                .setRootId(ROOT).setParentId(ROOT).setContent("reply body").setState(state)
                .setLikeCount(0L).setReplyCount(0L).setCreatedAt(LocalDateTime.now(CLOCK));
    }

    private void login(long userId, String... permissions) {
        LoginUser user = new LoginUser();
        user.setUser(new SysUser().setUserId(userId));
        user.setAuths(Arrays.asList(permissions));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
