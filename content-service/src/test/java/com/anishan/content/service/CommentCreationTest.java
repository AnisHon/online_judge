package com.anishan.content.service;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.api.client.user.domain.vo.UserSummaryVo;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.event.CommunityEvent;
import com.anishan.api.util.AccountPolicy;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.dto.CommentCreateRequest;
import com.anishan.content.domain.dto.CommentPageQuery;
import com.anishan.content.domain.entity.SolutionComment;
import com.anishan.content.domain.enumeration.CommentState;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.CommentVo;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.CommentLikeMapper;
import com.anishan.content.mapper.SolutionCommentMapper;
import com.anishan.content.mapper.SolutionExplanationMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import com.anishan.content.service.impl.CommentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommentCreationTest {
    private static final long ACTOR = 202L;
    private static final long AUTHOR = 101L;
    private static final long SOLUTION = 2098755579163770881L;
    private static final String REQUEST_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    private SolutionCommentMapper commentMapper;
    private CommentLikeMapper commentLikeMapper;
    private SolutionQueryService queryService;
    private SolutionProblemReferenceService referenceService;
    private SolutionAccessService accessService;
    private UserInternalClient userClient;
    private CommentCreationService writer;
    private CommentRateLimiter limiter;
    private SolutionDomainMigrationGate migrationGate;
    private SolutionExplanationMapper solutionMapper;
    private ContentEventOutboxService outbox;
    private CommentServiceImpl service;
    private ContentProblemReadVo publicProblem;
    private SolutionRecord solution;

    @BeforeEach
    void setUp() {
        LoginUser login = new LoginUser();
        login.setUser(new SysUser().setUserId(ACTOR));
        login.setAuths(Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, null, login.getAuthorities()));

        commentMapper = mock(SolutionCommentMapper.class);
        commentLikeMapper = mock(CommentLikeMapper.class);
        queryService = mock(SolutionQueryService.class);
        referenceService = mock(SolutionProblemReferenceService.class);
        accessService = mock(SolutionAccessService.class);
        userClient = mock(UserInternalClient.class);
        limiter = mock(CommentRateLimiter.class);
        migrationGate = mock(SolutionDomainMigrationGate.class);
        solutionMapper = mock(SolutionExplanationMapper.class);
        outbox = mock(ContentEventOutboxService.class);
        writer = new CommentCreationService(solutionMapper, commentMapper, accessService, outbox, CLOCK);
        service = new CommentServiceImpl(commentMapper, commentLikeMapper, queryService, referenceService,
                accessService, userClient, writer, new CommentContentValidator(), limiter, migrationGate);

        solution = new SolutionRecord();
        solution.setSolutionId(SOLUTION); solution.setProblemId(77L); solution.setUserId(AUTHOR);
        solution.setPrivate_(false); solution.setDelFlag(false);
        solution.setModerationState(SolutionModerationState.NORMAL);
        solution.setVersion(4L); solution.setCommentsOpen(true); solution.setCommentCount(0L);
        publicProblem = new ContentProblemReadVo("77", true, false, 1, true, false, "public problem");
        when(queryService.commentSnapshot(SOLUTION)).thenAnswer(ignored -> copy(solution));
        when(referenceService.refresh(Collections.singletonList(77L))).thenReturn(Collections.singletonMap(77L, publicProblem));
        when(accessService.isPubliclyReadable(publicProblem)).thenReturn(true);
        when(commentMapper.countRootComments(SOLUTION)).thenReturn(0L);
        when(commentMapper.selectRootComments(SOLUTION, 0L, 20L)).thenReturn(Collections.emptyList());
        when(commentLikeMapper.selectActiveCommentIdsForUser(anyLong(), anyList())).thenReturn(Collections.emptyList());
        when(userClient.userSummaries(any())).thenReturn(R.success(Collections.emptyList()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sameSuccessfulRequestReturnsOneRecordEvenAfterCommentsCloseWithoutRateLimit() {
        solution.setCommentsOpen(false);
        SolutionComment existing = comment(1L, SOLUTION, ACTOR, "same text", CommentState.VISIBLE);
        existing.setClientRequestId(REQUEST_ID);
        when(commentMapper.selectByUserAndClientRequestId(ACTOR, REQUEST_ID)).thenReturn(existing);

        CommentVo result = service.createRoot(SOLUTION,
                new CommentCreateRequest().setContent(" same text ").setClientRequestId(REQUEST_ID));

        assertEquals(1L, result.getCommentId());
        assertEquals("same text", result.getContent());
        verify(limiter, never()).acquire(anyLong());
        verify(commentMapper, never()).insert(any(SolutionComment.class));
        verify(outbox, never()).recordCommunity(any(CommunityEvent.class));
    }

    @Test
    void conflictingRequestIdIsRejectedAndHiddenSolutionIsNotReadable() {
        SolutionComment existing = comment(1L, SOLUTION, ACTOR, "other text", CommentState.VISIBLE);
        existing.setClientRequestId(REQUEST_ID);
        when(commentMapper.selectByUserAndClientRequestId(ACTOR, REQUEST_ID)).thenReturn(existing);
        ApiStatusException conflict = assertThrows(ApiStatusException.class, () -> service.createRoot(SOLUTION,
                new CommentCreateRequest().setContent("different").setClientRequestId(REQUEST_ID)));
        assertEquals(409, conflict.getStatusCode());

        when(commentMapper.selectByUserAndClientRequestId(ACTOR, REQUEST_ID)).thenReturn(null);
        solution.setPrivate_(true);
        ApiStatusException hidden = assertThrows(ApiStatusException.class, () -> service.pageRoots(SOLUTION, new CommentPageQuery()));
        assertEquals(404, hidden.getStatusCode());
        verify(commentMapper, never()).selectRootComments(anyLong(), anyLong(), anyLong());
    }

    @Test
    void visibilityQualificationRejectsRestrictedAndOtherAuthorsCannotBypassIt() {
        solution.setModerationState(SolutionModerationState.AUTHOR_ONLY);
        ApiStatusException restricted = assertThrows(ApiStatusException.class,
                () -> service.pageRoots(SOLUTION, new CommentPageQuery()));
        assertEquals(404, restricted.getStatusCode());
        verify(commentMapper, never()).countRootComments(anyLong());
    }

    @Test
    void directCommentServiceCallHonorsTheNegativePermission() {
        LoginUser denied = new LoginUser();
        denied.setUser(new SysUser().setUserId(ACTOR));
        denied.setAuths(Collections.singletonList(AccountPolicy.COMMENT_DENY));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(denied, null, denied.getAuthorities()));

        assertThrows(AccessDeniedException.class, () -> service.createRoot(SOLUTION,
                new CommentCreateRequest().setContent("not allowed").setClientRequestId(REQUEST_ID)));
        verify(commentMapper, never()).selectByUserAndClientRequestId(anyLong(), anyString());
        verify(limiter, never()).acquire(anyLong());
        verifyNoInteractions(outbox);
    }

    @Test
    void closedBetweenPreflightAndLockedWriteIsRejectedBeforeAnyMutation() {
        SolutionRecord locked = copy(solution);
        locked.setCommentsOpen(false);
        when(solutionMapper.selectCommentTargetForUpdate(SOLUTION)).thenReturn(locked);

        ApiStatusException closed = assertThrows(ApiStatusException.class,
                () -> writer.createRoot(ACTOR, SOLUTION, 4L, 77L, publicProblem, "text", REQUEST_ID));

        assertEquals(409, closed.getStatusCode());
        verify(commentMapper, never()).insert(any());
        verify(solutionMapper, never()).adjustCommentCount(anyLong(), anyInt());
        verifyNoInteractions(outbox);
    }

    @Test
    void duplicateConcurrentRequestIsResolvedAfterWriterRollbackAndOnlyOneCommentRemains() {
        SolutionComment existing = comment(8L, SOLUTION, ACTOR, "normalized", CommentState.VISIBLE);
        existing.setClientRequestId(REQUEST_ID);
        when(commentMapper.selectByUserAndClientRequestId(ACTOR, REQUEST_ID))
                .thenReturn(null, existing);
        when(solutionMapper.selectCommentTargetForUpdate(SOLUTION))
                .thenThrow(new DuplicateKeyException("duplicate request id"));

        CommentVo result = service.createRoot(SOLUTION,
                new CommentCreateRequest().setContent(" normalized ").setClientRequestId(REQUEST_ID));

        assertEquals(8L, result.getCommentId());
        verify(commentMapper, times(2)).selectByUserAndClientRequestId(ACTOR, REQUEST_ID);
        verify(outbox, never()).recordCommunity(any(CommunityEvent.class));
    }

    @Test
    void rootPageKeepsDeletedTombstonesAndBatchesSummariesAndLikedState() throws Exception {
        solution.setCommentsOpen(false);
        SolutionComment deleted = comment(3L, SOLUTION, 301L, "must not leak", CommentState.AUTHOR_DELETED)
                .setDeletedBy(999L);
        SolutionComment visible = comment(2L, SOLUTION, 302L, "visible", CommentState.VISIBLE);
        when(commentMapper.countRootComments(SOLUTION)).thenReturn(2L);
        when(commentMapper.selectRootComments(SOLUTION, 0L, 20L)).thenReturn(Arrays.asList(deleted, visible));
        UserSummaryVo first = new UserSummaryVo(); first.setUserId(301L); first.setNikeName("A");
        UserSummaryVo second = new UserSummaryVo(); second.setUserId(302L); second.setNikeName("B");
        when(userClient.userSummaries(any())).thenReturn(R.success(Arrays.asList(first, second)));

        PagedResult<CommentVo> result = service.pageRoots(SOLUTION, new CommentPageQuery());

        assertEquals(2L, result.getTotalRecords());
        assertNull(result.getData().get(0).getContent());
        assertEquals(CommentState.AUTHOR_DELETED, result.getData().get(0).getState());
        assertFalse(result.getData().get(0).getCanDelete());
        String publicTombstone = new ObjectMapper().registerModule(new JavaTimeModule())
                .writeValueAsString(result.getData().get(0));
        assertFalse(publicTombstone.contains("must not leak"));
        assertFalse(publicTombstone.contains("deletedBy"));
        assertFalse(publicTombstone.contains("reason"));
        assertEquals("visible", result.getData().get(1).getContent());
        verify(userClient, times(1)).userSummaries(any());
        verify(commentLikeMapper, times(1)).selectActiveCommentIdsForUser(eq(ACTOR), anyList());
    }

    @Test
    void successfulWriterUpdatesCountAndRecordsAContentFreeAuthorEvent() {
        when(solutionMapper.selectCommentTargetForUpdate(SOLUTION)).thenReturn(copy(solution));
        when(commentMapper.insert(any(SolutionComment.class))).thenAnswer(invocation -> {
            ((SolutionComment) invocation.getArgument(0)).setCommentId(900L);
            return 1;
        });
        when(solutionMapper.adjustCommentCount(SOLUTION, 1)).thenReturn(1);

        SolutionComment created = writer.createRoot(ACTOR, SOLUTION, 4L, 77L, publicProblem,
                "plain text", REQUEST_ID);

        assertEquals(900L, created.getCommentId());
        assertNull(created.getRootId());
        assertNull(created.getParentId());
        org.mockito.ArgumentCaptor<CommunityEvent> event = org.mockito.ArgumentCaptor.forClass(CommunityEvent.class);
        verify(outbox).recordCommunity(event.capture());
        assertEquals("solution-commented:900", event.getValue().getDedupeKey());
        assertEquals(Collections.singletonList(String.valueOf(AUTHOR)), event.getValue().getRecipientIds());
        assertNull(event.getValue().getReason());
        assertNull(event.getValue().getAction());
    }

    @Test
    void mapperUsesLockedMetadataAndStableTombstoneRootPagination() throws Exception {
        Configuration configuration = new Configuration();
        parseMapper(configuration, "/mapper/SolutionExplanationMapper.xml");
        parseMapper(configuration, "/mapper/SolutionCommentMapper.xml");
        Map<String, Object> args = Map.of("solutionId", SOLUTION, "offset", 20L, "limit", 20L);
        BoundSql roots = configuration.getMappedStatement(
                SolutionCommentMapper.class.getName() + ".selectRootComments").getBoundSql(args);
        String rootSql = roots.getSql().replaceAll("\\s+", " ").trim().toLowerCase();
        assertTrue(rootSql.contains("root_id is null and parent_id is null"), rootSql);
        assertFalse(rootSql.contains("state ="), rootSql);
        assertTrue(rootSql.contains("order by created_at desc, comment_id desc"), rootSql);

        BoundSql lock = configuration.getMappedStatement(
                SolutionExplanationMapper.class.getName() + ".selectCommentTargetForUpdate")
                .getBoundSql(Collections.singletonMap("solutionId", SOLUTION));
        String lockSql = lock.getSql().replaceAll("\\s+", " ").trim().toLowerCase();
        assertTrue(lockSql.endsWith("for update"), lockSql);
        assertFalse(lockSql.contains("solution_explanation_content"), lockSql);
        BoundSql count = configuration.getMappedStatement(
                SolutionExplanationMapper.class.getName() + ".adjustCommentCount")
                .getBoundSql(Map.of("solutionId", SOLUTION, "delta", 1));
        assertTrue(count.getSql().toLowerCase().contains("comment_count + ? >= 0"), count.getSql());
        assertNotNull(CommentCreationService.class.getMethod("createRoot", Long.class, Long.class, Long.class,
                Long.class, ContentProblemReadVo.class, String.class, String.class).getAnnotation(Transactional.class));
    }

    private void parseMapper(Configuration configuration, String resource) throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private SolutionComment comment(Long id, Long solutionId, Long userId, String content, CommentState state) {
        return new SolutionComment().setCommentId(id).setSolutionId(solutionId).setUserId(userId)
                .setRootId(null).setParentId(null).setContent(content).setState(state)
                .setLikeCount(0L).setReplyCount(0L).setCreatedAt(LocalDateTime.now(CLOCK));
    }

    private SolutionRecord copy(SolutionRecord source) {
        SolutionRecord result = new SolutionRecord();
        result.setSolutionId(source.getSolutionId()); result.setProblemId(source.getProblemId());
        result.setUserId(source.getUserId()); result.setPrivate_(source.getPrivate_());
        result.setDelFlag(source.getDelFlag()); result.setModerationState(source.getModerationState());
        result.setVersion(source.getVersion()); result.setCommentsOpen(source.getCommentsOpen());
        result.setCommentCount(source.getCommentCount());
        return result;
    }
}
