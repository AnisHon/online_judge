package com.anishan.content.service;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.dto.CommentPageQuery;
import com.anishan.content.domain.dto.CommentReplyRequest;
import com.anishan.content.domain.entity.SolutionComment;
import com.anishan.content.domain.enumeration.CommentState;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.CommentVo;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.CommentLikeMapper;
import com.anishan.content.mapper.SolutionCommentMapper;
import com.anishan.content.mapper.SolutionExplanationMapper;
import com.anishan.content.service.impl.CommentServiceImpl;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommentReplyServiceTest {
    private static final long ACTOR = 202L;
    private static final long AUTHOR = 101L;
    private static final long SOLUTION = 2098755579163770881L;
    private static final long ROOT = 9001L;
    private static final long PARENT = 9002L;
    private static final long PROBLEM = 77L;
    private static final String REQUEST_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    private SolutionCommentMapper commentMapper;
    private CommentLikeMapper commentLikeMapper;
    private SolutionExplanationMapper solutionMapper;
    private SolutionQueryService queryService;
    private SolutionProblemReferenceService referenceService;
    private SolutionAccessService accessService;
    private UserInternalClient userClient;
    private ContentEventOutboxService outbox;
    private CommentRateLimiter limiter;
    private SolutionDomainMigrationGate migrationGate;
    private CommentCreationService writer;
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
        solutionMapper = mock(SolutionExplanationMapper.class);
        queryService = mock(SolutionQueryService.class);
        referenceService = mock(SolutionProblemReferenceService.class);
        accessService = mock(SolutionAccessService.class);
        userClient = mock(UserInternalClient.class);
        outbox = mock(ContentEventOutboxService.class);
        limiter = mock(CommentRateLimiter.class);
        migrationGate = mock(SolutionDomainMigrationGate.class);
        writer = new CommentCreationService(solutionMapper, commentMapper, accessService, outbox, CLOCK);
        service = new CommentServiceImpl(commentMapper, commentLikeMapper, queryService, referenceService,
                accessService, userClient, writer, new CommentContentValidator(), limiter, migrationGate);

        solution = new SolutionRecord();
        solution.setSolutionId(SOLUTION);
        solution.setProblemId(PROBLEM);
        solution.setUserId(AUTHOR);
        solution.setPrivate_(false);
        solution.setDelFlag(false);
        solution.setModerationState(SolutionModerationState.NORMAL);
        solution.setVersion(4L);
        solution.setCommentsOpen(true);
        solution.setCommentCount(0L);
        publicProblem = new ContentProblemReadVo(String.valueOf(PROBLEM), true, false, 1,
                true, false, "public problem");
        when(queryService.commentSnapshot(SOLUTION)).thenAnswer(ignored -> copy(solution));
        when(referenceService.refresh(Collections.singletonList(PROBLEM)))
                .thenReturn(Collections.singletonMap(PROBLEM, publicProblem));
        when(accessService.isPubliclyReadable(publicProblem)).thenReturn(true);
        when(commentLikeMapper.selectActiveCommentIdsForUser(anyLong(), anyList()))
                .thenReturn(Collections.emptyList());
        when(userClient.userSummaries(any())).thenReturn(R.success(Collections.emptyList()));
        when(commentMapper.incrementRootReplyCount(ROOT)).thenReturn(1);
        when(solutionMapper.adjustCommentCount(SOLUTION, 1)).thenReturn(1);
        when(commentMapper.insert(any(SolutionComment.class))).thenAnswer(invocation -> {
            ((SolutionComment) invocation.getArgument(0)).setCommentId(9901L);
            return 1;
        });
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsRootReplyWithOneDeduplicatedRecipientAndAtomicCounters() throws Exception {
        SolutionComment root = root(ROOT, SOLUTION, AUTHOR, CommentState.VISIBLE);
        when(commentMapper.selectByUserAndClientRequestId(ACTOR, REQUEST_ID)).thenReturn(null);
        when(commentMapper.selectReplyTarget(ROOT)).thenReturn(root);
        when(solutionMapper.selectCommentTargetForUpdate(SOLUTION)).thenReturn(copy(solution));
        when(commentMapper.selectRootForUpdate(ROOT, SOLUTION)).thenReturn(root);

        CommentVo result = service.createReply(ROOT,
                new CommentReplyRequest().setContent("  hello  ").setClientRequestId(REQUEST_ID));

        assertEquals(9901L, result.getCommentId());
        ArgumentCaptor<SolutionComment> inserted = ArgumentCaptor.forClass(SolutionComment.class);
        verify(commentMapper).insert(inserted.capture());
        assertEquals(ROOT, inserted.getValue().getRootId());
        assertEquals(ROOT, inserted.getValue().getParentId());
        assertEquals(AUTHOR, inserted.getValue().getReplyToUserId());
        verify(commentMapper).incrementRootReplyCount(ROOT);
        verify(solutionMapper).adjustCommentCount(SOLUTION, 1);
        verify(commentMapper, never()).selectReplyParentForUpdate(anyLong(), anyLong());

        ArgumentCaptor<CommunityEvent> eventCapture = ArgumentCaptor.forClass(CommunityEvent.class);
        verify(outbox).recordCommunity(eventCapture.capture());
        CommunityEvent event = eventCapture.getValue();
        assertEquals(CommunityEventType.COMMENT_REPLIED, event.getEventType());
        assertEquals("comment-replied:9901", event.getDedupeKey());
        assertEquals(Collections.singletonList(String.valueOf(AUTHOR)), event.getRecipientIds());
        event.validate();
        verify(outbox, times(1)).recordCommunity(any(CommunityEvent.class));
        assertNotNull(CommentCreationService.class.getMethod("createReply", Long.class, Long.class,
                Long.class, Long.class, Long.class, Long.class, ContentProblemReadVo.class,
                String.class, String.class).getAnnotation(Transactional.class));
    }

    @Test
    void replyToArbitrarilyDeepVisibleChildUsesOnlyStoredRootAndParentLocks() {
        SolutionComment deepParent = reply(PARENT, SOLUTION, 303L, ROOT, 8080L, CommentState.VISIBLE);
        SolutionComment root = root(ROOT, SOLUTION, AUTHOR, CommentState.AUTHOR_DELETED);
        when(commentMapper.selectByUserAndClientRequestId(ACTOR, REQUEST_ID)).thenReturn(null);
        when(commentMapper.selectReplyTarget(PARENT)).thenReturn(deepParent);
        when(commentMapper.selectReplyTarget(ROOT)).thenReturn(root);
        when(solutionMapper.selectCommentTargetForUpdate(SOLUTION)).thenReturn(copy(solution));
        when(commentMapper.selectRootForUpdate(ROOT, SOLUTION)).thenReturn(root);
        when(commentMapper.selectReplyParentForUpdate(PARENT, SOLUTION)).thenReturn(deepParent);

        service.createReply(PARENT,
                new CommentReplyRequest().setContent("reply to depth 100").setClientRequestId(REQUEST_ID));

        verify(commentMapper, times(2)).selectReplyTarget(anyLong());
        verify(commentMapper).selectRootForUpdate(ROOT, SOLUTION);
        verify(commentMapper).selectReplyParentForUpdate(PARENT, SOLUTION);
        verify(commentMapper, never()).selectReplyTarget(8080L);
        ArgumentCaptor<SolutionComment> inserted = ArgumentCaptor.forClass(SolutionComment.class);
        verify(commentMapper).insert(inserted.capture());
        assertEquals(ROOT, inserted.getValue().getRootId());
        assertEquals(PARENT, inserted.getValue().getParentId());
        assertEquals(303L, inserted.getValue().getReplyToUserId());
    }

    @Test
    void selfReplyHasNoSelfNotificationAndActorIsRemovedFromBothRecipientCandidates() {
        solution.setUserId(ACTOR);
        when(queryService.commentSnapshot(SOLUTION)).thenAnswer(ignored -> copy(solution));
        SolutionComment root = root(ROOT, SOLUTION, ACTOR, CommentState.VISIBLE);
        when(commentMapper.selectByUserAndClientRequestId(ACTOR, REQUEST_ID)).thenReturn(null);
        when(commentMapper.selectReplyTarget(ROOT)).thenReturn(root);
        when(solutionMapper.selectCommentTargetForUpdate(SOLUTION)).thenReturn(copy(solution));
        when(commentMapper.selectRootForUpdate(ROOT, SOLUTION)).thenReturn(root);

        service.createReply(ROOT,
                new CommentReplyRequest().setContent("self").setClientRequestId(REQUEST_ID));

        ArgumentCaptor<CommunityEvent> eventCapture = ArgumentCaptor.forClass(CommunityEvent.class);
        verify(outbox).recordCommunity(eventCapture.capture());
        assertTrue(eventCapture.getValue().getRecipientIds().isEmpty());
        eventCapture.getValue().validate();
    }

    @Test
    void deletedParentCannotBeRepliedTo() {
        SolutionComment deleted = root(ROOT, SOLUTION, AUTHOR, CommentState.AUTHOR_DELETED);
        when(commentMapper.selectByUserAndClientRequestId(ACTOR, REQUEST_ID)).thenReturn(null);
        when(commentMapper.selectReplyTarget(ROOT)).thenReturn(deleted);

        ApiStatusException error = assertThrows(ApiStatusException.class, () -> service.createReply(ROOT,
                new CommentReplyRequest().setContent("no").setClientRequestId(REQUEST_ID)));

        assertEquals(404, error.getStatusCode());
        verify(solutionMapper, never()).selectCommentTargetForUpdate(anyLong());
        verify(commentMapper, never()).insert(any(SolutionComment.class));
        verifyNoInteractions(outbox);
    }

    @Test
    void wrongRootOrCrossSolutionParentIsNotExposed() {
        SolutionComment notRoot = reply(PARENT, SOLUTION, AUTHOR, ROOT, ROOT, CommentState.VISIBLE);
        when(commentMapper.selectReplyTarget(ROOT)).thenReturn(notRoot);
        ApiStatusException wrongRoot = assertThrows(ApiStatusException.class,
                () -> service.pageReplies(ROOT, new CommentPageQuery()));
        assertEquals(404, wrongRoot.getStatusCode());
        verify(commentMapper, never()).countRootReplies(anyLong());

        SolutionComment root = root(ROOT, SOLUTION + 1, AUTHOR, CommentState.VISIBLE);
        SolutionComment parent = reply(PARENT, SOLUTION, AUTHOR, ROOT, ROOT, CommentState.VISIBLE);
        when(commentMapper.selectReplyTarget(PARENT)).thenReturn(parent);
        when(commentMapper.selectReplyTarget(ROOT)).thenReturn(root);
        ApiStatusException wrongSolution = assertThrows(ApiStatusException.class,
                () -> service.createReply(PARENT, new CommentReplyRequest()
                        .setContent("no cross-solution reply").setClientRequestId(REQUEST_ID)));
        assertEquals(404, wrongSolution.getStatusCode());
        verify(commentMapper, never()).insert(any(SolutionComment.class));
    }

    @Test
    void reusedRootCommentRequestIdCannotCreateAReply() {
        SolutionComment existingRoot = root(ROOT, SOLUTION, ACTOR, CommentState.VISIBLE)
                .setContent("same text");
        existingRoot.setClientRequestId(REQUEST_ID);
        when(commentMapper.selectByUserAndClientRequestId(ACTOR, REQUEST_ID)).thenReturn(existingRoot);

        ApiStatusException error = assertThrows(ApiStatusException.class, () -> service.createReply(ROOT,
                new CommentReplyRequest().setContent("same text").setClientRequestId(REQUEST_ID)));

        assertEquals(409, error.getStatusCode());
        verify(commentMapper, never()).selectReplyTarget(anyLong());
        verify(limiter, never()).acquire(anyLong());
    }

    @Test
    void reusedReplyRequestIdForAnotherParentReturnsConflict() {
        SolutionComment existing = reply(7001L, SOLUTION, ACTOR, ROOT, 7000L, CommentState.VISIBLE)
                .setContent("same text");
        existing.setClientRequestId(REQUEST_ID);
        when(commentMapper.selectByUserAndClientRequestId(ACTOR, REQUEST_ID)).thenReturn(existing);

        ApiStatusException error = assertThrows(ApiStatusException.class, () -> service.createReply(PARENT,
                new CommentReplyRequest().setContent("same text").setClientRequestId(REQUEST_ID)));

        assertEquals(409, error.getStatusCode());
        verify(commentMapper, never()).selectReplyTarget(anyLong());
        verify(limiter, never()).acquire(anyLong());
        verifyNoInteractions(outbox);
    }

    @Test
    void lockedStateRechecksRejectConcurrentParentDeletionAndCommentClosure() {
        SolutionComment child = reply(PARENT, SOLUTION, 303L, ROOT, ROOT, CommentState.VISIBLE);
        SolutionComment deletedChild = reply(PARENT, SOLUTION, 303L, ROOT, ROOT, CommentState.ADMIN_DELETED);
        SolutionComment root = root(ROOT, SOLUTION, AUTHOR, CommentState.VISIBLE);
        when(commentMapper.selectByUserAndClientRequestId(ACTOR, REQUEST_ID)).thenReturn(null);
        when(commentMapper.selectReplyTarget(PARENT)).thenReturn(child);
        when(commentMapper.selectReplyTarget(ROOT)).thenReturn(root);
        when(solutionMapper.selectCommentTargetForUpdate(SOLUTION)).thenReturn(copy(solution));
        when(commentMapper.selectRootForUpdate(ROOT, SOLUTION)).thenReturn(root);
        when(commentMapper.selectReplyParentForUpdate(PARENT, SOLUTION)).thenReturn(deletedChild);

        ApiStatusException deleted = assertThrows(ApiStatusException.class, () -> service.createReply(PARENT,
                new CommentReplyRequest().setContent("racing deletion").setClientRequestId(REQUEST_ID)));
        assertEquals(404, deleted.getStatusCode());
        verify(commentMapper, never()).insert(any(SolutionComment.class));

        reset(commentMapper, solutionMapper, outbox);
        when(queryService.commentSnapshot(SOLUTION)).thenAnswer(ignored -> copy(solution));
        SolutionRecord closed = copy(solution);
        closed.setCommentsOpen(false);
        when(commentMapper.selectByUserAndClientRequestId(ACTOR, REQUEST_ID)).thenReturn(null);
        when(commentMapper.selectReplyTarget(ROOT)).thenReturn(root);
        when(solutionMapper.selectCommentTargetForUpdate(SOLUTION)).thenReturn(closed);
        when(commentMapper.selectRootForUpdate(ROOT, SOLUTION)).thenReturn(root);

        ApiStatusException closedError = assertThrows(ApiStatusException.class, () -> service.createReply(ROOT,
                new CommentReplyRequest().setContent("racing close").setClientRequestId(REQUEST_ID)));
        assertEquals(409, closedError.getStatusCode());
        verify(commentMapper, never()).insert(any(SolutionComment.class));
        verify(commentMapper, never()).incrementRootReplyCount(anyLong());
        verifyNoInteractions(outbox);
    }

    @Test
    void idempotentReplyRetrySucceedsAfterCommentsCloseWithoutRateLimitOrDuplicateEvent() {
        solution.setCommentsOpen(false);
        when(queryService.commentSnapshot(SOLUTION)).thenAnswer(ignored -> copy(solution));
        SolutionComment existing = reply(9901L, SOLUTION, ACTOR, ROOT, PARENT, CommentState.VISIBLE)
                .setContent("same");
        existing.setClientRequestId(REQUEST_ID);
        when(commentMapper.selectByUserAndClientRequestId(ACTOR, REQUEST_ID)).thenReturn(existing);

        CommentVo result = service.createReply(PARENT,
                new CommentReplyRequest().setContent("same").setClientRequestId(REQUEST_ID));

        assertEquals(9901L, result.getCommentId());
        verify(limiter, never()).acquire(anyLong());
        verify(commentMapper, never()).insert(any(SolutionComment.class));
        verifyNoInteractions(outbox);
    }

    @Test
    void repliesArePagedFlatInAscendingOrderAndOnlyAgainstARealRoot() throws Exception {
        SolutionComment root = root(ROOT, SOLUTION, AUTHOR, CommentState.VISIBLE);
        SolutionComment deepReply = reply(9901L, SOLUTION, ACTOR, ROOT, 8000L, CommentState.VISIBLE);
        when(commentMapper.selectReplyTarget(ROOT)).thenReturn(root, root);
        when(commentMapper.countRootReplies(ROOT)).thenReturn(1L);
        when(commentMapper.selectRootReplies(ROOT, 0L, 20L)).thenReturn(Collections.singletonList(deepReply));

        PagedResult<CommentVo> page = service.pageReplies(ROOT, new CommentPageQuery());

        assertEquals(1L, page.getTotalRecords());
        assertEquals(9901L, page.getData().get(0).getCommentId());
        assertEquals(8000L, page.getData().get(0).getParentId());

        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getResourceAsStream("/mapper/SolutionCommentMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, "SolutionCommentMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        BoundSql sql = configuration.getMappedStatement(
                SolutionCommentMapper.class.getName() + ".selectRootReplies")
                .getBoundSql(Map.of("rootId", ROOT, "offset", 0L, "limit", 20L));
        String normalizedSql = sql.getSql().replaceAll("\\s+", " ").toLowerCase();
        assertTrue(normalizedSql.contains("where root_id=?"), normalizedSql);
        assertTrue(normalizedSql.contains("order by created_at asc, comment_id asc"), normalizedSql);
        assertFalse(normalizedSql.contains("with recursive"), normalizedSql);
        assertFalse(normalizedSql.contains("parent_id ="), normalizedSql);

        BoundSql rootLock = configuration.getMappedStatement(
                SolutionCommentMapper.class.getName() + ".selectRootForUpdate")
                .getBoundSql(Map.of("rootId", ROOT, "solutionId", SOLUTION));
        assertTrue(rootLock.getSql().toLowerCase().trim().endsWith("for update"));
        BoundSql parentLock = configuration.getMappedStatement(
                SolutionCommentMapper.class.getName() + ".selectReplyParentForUpdate")
                .getBoundSql(Map.of("parentId", PARENT, "solutionId", SOLUTION));
        assertTrue(parentLock.getSql().toLowerCase().trim().endsWith("for update"));
    }

    @Test
    void replyWriterUsesSolutionRootParentLockOrder() {
        SolutionComment parent = reply(PARENT, SOLUTION, 303L, ROOT, ROOT, CommentState.VISIBLE);
        SolutionComment root = root(ROOT, SOLUTION, AUTHOR, CommentState.VISIBLE);
        when(commentMapper.selectByUserAndClientRequestId(ACTOR, REQUEST_ID)).thenReturn(null);
        when(commentMapper.selectReplyTarget(PARENT)).thenReturn(parent);
        when(commentMapper.selectReplyTarget(ROOT)).thenReturn(root);
        when(solutionMapper.selectCommentTargetForUpdate(SOLUTION)).thenReturn(copy(solution));
        when(commentMapper.selectRootForUpdate(ROOT, SOLUTION)).thenReturn(root);
        when(commentMapper.selectReplyParentForUpdate(PARENT, SOLUTION)).thenReturn(parent);

        service.createReply(PARENT,
                new CommentReplyRequest().setContent("ordered").setClientRequestId(REQUEST_ID));

        InOrder locks = inOrder(solutionMapper, commentMapper);
        locks.verify(solutionMapper).selectCommentTargetForUpdate(SOLUTION);
        locks.verify(commentMapper).selectRootForUpdate(ROOT, SOLUTION);
        locks.verify(commentMapper).selectReplyParentForUpdate(PARENT, SOLUTION);
    }

    private SolutionComment root(Long id, Long solutionId, Long userId, CommentState state) {
        return new SolutionComment().setCommentId(id).setSolutionId(solutionId).setUserId(userId)
                .setRootId(null).setParentId(null).setReplyToUserId(null).setState(state)
                .setLikeCount(0L).setReplyCount(0L);
    }

    private SolutionComment reply(Long id, Long solutionId, Long userId,
                                  Long rootId, Long parentId, CommentState state) {
        return new SolutionComment().setCommentId(id).setSolutionId(solutionId).setUserId(userId)
                .setRootId(rootId).setParentId(parentId).setReplyToUserId(userId).setState(state)
                .setContent("text").setLikeCount(0L).setReplyCount(0L);
    }

    private SolutionRecord copy(SolutionRecord source) {
        SolutionRecord result = new SolutionRecord();
        result.setSolutionId(source.getSolutionId());
        result.setProblemId(source.getProblemId());
        result.setUserId(source.getUserId());
        result.setPrivate_(source.getPrivate_());
        result.setDelFlag(source.getDelFlag());
        result.setModerationState(source.getModerationState());
        result.setVersion(source.getVersion());
        result.setCommentsOpen(source.getCommentsOpen());
        result.setCommentCount(source.getCommentCount());
        return result;
    }
}
