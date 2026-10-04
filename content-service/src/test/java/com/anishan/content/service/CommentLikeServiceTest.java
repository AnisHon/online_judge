package com.anishan.content.service;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.api.util.AccountPolicy;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.entity.CommentLike;
import com.anishan.content.domain.entity.SolutionComment;
import com.anishan.content.domain.enumeration.CommentState;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.LikeResultVo;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.CommentLikeMapper;
import com.anishan.content.mapper.SolutionCommentMapper;
import com.anishan.content.mapper.SolutionExplanationMapper;
import com.anishan.content.service.impl.CommentLikeServiceImpl;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommentLikeServiceTest {
    private static final Long SOLUTION_ID = 2098755579163770881L;
    private static final Long ROOT_ID = 9223372036854775000L;
    private static final Long COMMENT_ID = 9223372036854775001L;
    private static final Long ACTOR_ID = 202L;
    private static final Long AUTHOR_ID = 101L;
    private static final Long PROBLEM_ID = 77L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final ContentProblemReadVo PUBLIC_PROBLEM =
            new ContentProblemReadVo("77", true, false, 1, true, false, "公开题");
    private static final ContentProblemReadVo PRIVATE_PROBLEM =
            new ContentProblemReadVo("77", true, false, 2, false, false, null);

    private SolutionExplanationMapper solutionMapper;
    private SolutionCommentMapper commentMapper;
    private CommentLikeMapper likeMapper;
    private SolutionAccessService accessService;
    private ContentEventOutboxService outbox;
    private SolutionQueryService queryService;
    private SolutionProblemReferenceService referenceService;
    private SolutionDomainMigrationGate migrationGate;
    private CommentLikeLocalWriteService writer;
    private CommentLikeServiceImpl service;
    private AtomicReference<CommentLike> relation;
    private AtomicLong count;
    private SolutionRecord solution;
    private SolutionComment root;
    private SolutionComment target;

    @BeforeEach
    void setUp() {
        login(ACTOR_ID);
        solutionMapper = mock(SolutionExplanationMapper.class);
        commentMapper = mock(SolutionCommentMapper.class);
        likeMapper = mock(CommentLikeMapper.class);
        accessService = mock(SolutionAccessService.class);
        outbox = mock(ContentEventOutboxService.class);
        queryService = mock(SolutionQueryService.class);
        referenceService = mock(SolutionProblemReferenceService.class);
        migrationGate = mock(SolutionDomainMigrationGate.class);
        relation = new AtomicReference<>();
        count = new AtomicLong();
        solution = solution(AUTHOR_ID, false);
        root = comment(ROOT_ID, null, null, CommentState.VISIBLE, AUTHOR_ID);
        target = root;
        writer = new CommentLikeLocalWriteService(solutionMapper, commentMapper, likeMapper,
                accessService, outbox, CLOCK);
        service = new CommentLikeServiceImpl(queryService, referenceService, accessService,
                commentMapper, writer, migrationGate);

        when(solutionMapper.selectCommentTargetForUpdate(SOLUTION_ID)).thenAnswer(ignored -> copy(solution));
        when(commentMapper.selectRootForUpdate(ROOT_ID, SOLUTION_ID)).thenAnswer(ignored -> copy(root));
        when(commentMapper.selectTargetForUpdate(COMMENT_ID, SOLUTION_ID)).thenAnswer(ignored -> copy(target));
        when(likeMapper.selectByCommentAndUserForUpdate(anyLong(), eq(ACTOR_ID)))
                .thenAnswer(ignored -> relation.get());
        when(likeMapper.insertState(any())).thenAnswer(invocation -> {
            CommentLike inserted = invocation.getArgument(0);
            if (relation.get() != null) throw new DuplicateKeyException("duplicate relation");
            relation.set(inserted);
            return 1;
        });
        when(likeMapper.updateActiveState(anyLong(), eq(ACTOR_ID), anyBoolean(), any()))
                .thenAnswer(invocation -> {
                    CommentLike current = relation.get();
                    boolean active = invocation.getArgument(2);
                    if (current == null || Boolean.valueOf(active).equals(current.getActive())) return 0;
                    current.setActive(active).setUpdatedAt(invocation.getArgument(3));
                    return 1;
                });
        when(likeMapper.markFirstNotified(anyLong(), eq(ACTOR_ID), any()))
                .thenAnswer(invocation -> {
                    CommentLike current = relation.get();
                    if (current == null || !Boolean.TRUE.equals(current.getActive())
                            || Boolean.TRUE.equals(current.getFirstNotified())) return 0;
                    current.setFirstNotified(true);
                    return 1;
                });
        when(commentMapper.selectLikeCount(anyLong())).thenAnswer(ignored -> count.get());
        when(commentMapper.adjustLikeCount(anyLong(), anyInt(), any())).thenAnswer(invocation -> {
            long next = count.get() + (Integer) invocation.getArgument(1);
            if (next < 0) return 0;
            count.set(next);
            return 1;
        });
        when(accessService.isPubliclyReadable(PUBLIC_PROBLEM)).thenReturn(true);
        when(accessService.isPubliclyReadable(PRIVATE_PROBLEM)).thenReturn(false);
        when(commentMapper.selectReplyTarget(COMMENT_ID)).thenAnswer(ignored -> copy(target));
        when(commentMapper.selectReplyTarget(ROOT_ID)).thenAnswer(ignored -> copy(root));
        when(queryService.likeSnapshot(SOLUTION_ID)).thenAnswer(ignored -> copy(solution));
        when(referenceService.refresh(Collections.singletonList(PROBLEM_ID)))
                .thenReturn(Collections.singletonMap(PROBLEM_ID, PUBLIC_PROBLEM));
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
    void repeatedHundredLikeCancelCyclesKeepOneRelationAndOneFirstLikeNotification() {
        for (int i = 0; i < 100; i++) {
            LikeResultVo liked = writer.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, ROOT_ID,
                    true, PUBLIC_PROBLEM);
            assertTrue(liked.isLiked());
            LikeResultVo unliked = writer.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, ROOT_ID,
                    false, PUBLIC_PROBLEM);
            assertFalse(unliked.isLiked());
        }

        assertNotNull(relation.get());
        assertFalse(relation.get().getActive());
        assertTrue(relation.get().getFirstNotified());
        assertEquals(0L, count.get());
        verify(likeMapper, times(1)).insertState(any());
        verify(outbox, times(1)).recordCommunity(any(CommunityEvent.class));
    }

    @Test
    void firstOtherLikeEmitsValidatedCommentEventAndSelfLikeNeverNotifies() {
        LikeResultVo result = writer.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, ROOT_ID,
                true, PUBLIC_PROBLEM);
        assertEquals(1L, result.getLikeCount());
        verify(outbox).recordCommunity(argThat(event -> event.getEventType() == CommunityEventType.COMMENT_LIKED
                && ("comment-liked:" + ROOT_ID + ":" + ACTOR_ID).equals(event.getDedupeKey())
                && String.valueOf(SOLUTION_ID).equals(event.getSolutionId())
                && String.valueOf(ROOT_ID).equals(event.getCommentId())
                && Collections.singletonList(String.valueOf(AUTHOR_ID)).equals(event.getRecipientIds())));

        relation.set(null);
        count.set(0L);
        target = comment(ROOT_ID, null, null, CommentState.VISIBLE, ACTOR_ID);
        root = target;
        clearInvocations(outbox);
        writer.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, ROOT_ID, true, PUBLIC_PROBLEM);
        assertTrue(relation.get().getFirstNotified());
        verifyNoInteractions(outbox);
    }

    @Test
    void relikeAfterCancelDoesNotNotifyTwiceAndAtomicCounterNeverGoesNegative() {
        writer.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, ROOT_ID, true, PUBLIC_PROBLEM);
        writer.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, ROOT_ID, false, PUBLIC_PROBLEM);
        writer.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, ROOT_ID, false, PUBLIC_PROBLEM);
        writer.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, ROOT_ID, true, PUBLIC_PROBLEM);
        assertEquals(1L, count.get());
        assertEquals(1, mockingDetails(outbox).getInvocations().size());
        // Simulate a pre-existing corrupt counter: the SQL-level guard must reject a decrement below zero.
        count.set(0L);
        assertThrows(ApiStatusException.class,
                () -> writer.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, ROOT_ID, false, PUBLIC_PROBLEM));
        assertEquals(0L, count.get());
    }

    @Test
    void hiddenDeletedOwnTargetCanBeUnlikedButItsCountIsNeverReturned() {
        target = comment(ROOT_ID, null, null, CommentState.ADMIN_DELETED, AUTHOR_ID);
        root = target;
        solution.setPrivate_(true);
        relation.set(new CommentLike().setCommentId(ROOT_ID).setUserId(ACTOR_ID)
                .setActive(true).setFirstNotified(true));
        count.set(1L);

        LikeResultVo result = writer.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, ROOT_ID,
                false, PRIVATE_PROBLEM);

        assertFalse(result.isLiked());
        assertNull(result.getLikeCount());
        assertEquals(0L, count.get());
    }

    @Test
    void deletedCommentCannotReceiveANewLikeButVisibleReplyUnderDeletedRootCan() {
        target = comment(ROOT_ID, null, null, CommentState.ADMIN_DELETED, AUTHOR_ID);
        root = target;
        assertEquals(404, assertThrows(ApiStatusException.class,
                () -> writer.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, ROOT_ID, true, PUBLIC_PROBLEM))
                .getStatusCode());
        verify(likeMapper, never()).insertState(any());

        root = comment(ROOT_ID, null, null, CommentState.AUTHOR_DELETED, AUTHOR_ID);
        target = comment(COMMENT_ID, ROOT_ID, ROOT_ID, CommentState.VISIBLE, AUTHOR_ID);
        LikeResultVo result = writer.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, COMMENT_ID,
                true, PUBLIC_PROBLEM);
        assertTrue(result.isLiked());
        assertEquals(1L, result.getLikeCount());
        verify(commentMapper, times(2)).selectRootForUpdate(ROOT_ID, SOLUTION_ID);
        verify(commentMapper).selectTargetForUpdate(COMMENT_ID, SOLUTION_ID);
    }

    @Test
    void closedCommentAreaAndCommentDenyDoNotDisableLikes() {
        solution.setCommentsOpen(false);
        login(ACTOR_ID, AccountPolicy.COMMENT_DENY);
        LikeResultVo result = service.like(ACTOR_ID, ROOT_ID);
        assertTrue(result.isLiked());
        verify(migrationGate).requireCutover();
        verify(referenceService).refresh(Collections.singletonList(PROBLEM_ID));
        assertEquals(1L, count.get());
        assertTrue(relation.get().getActive());
    }

    @Test
    void unlikeOfMissingOrNowPrivateSolutionIsSafeAndDoesNotCreateRelation() {
        when(queryService.likeSnapshot(SOLUTION_ID)).thenReturn(null);
        LikeResultVo missing = service.unlike(ACTOR_ID, COMMENT_ID);
        assertFalse(missing.isLiked());
        assertNull(missing.getLikeCount());
        verifyNoInteractions(referenceService);
        verify(likeMapper, never()).insertState(any());
        assertNull(relation.get());

        when(queryService.likeSnapshot(SOLUTION_ID)).thenReturn(copy(solution));
        when(referenceService.refresh(Collections.singletonList(PROBLEM_ID)))
                .thenReturn(Collections.singletonMap(PROBLEM_ID, PRIVATE_PROBLEM));
        solution.setPrivate_(true);
        // No active row means this is an idempotent cancel, not an insert.
        LikeResultVo hidden = writer.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, ROOT_ID,
                false, PRIVATE_PROBLEM);
        assertFalse(hidden.isLiked());
        assertNull(hidden.getLikeCount());
        verify(likeMapper, never()).insertState(any());
    }

    @Test
    void orchestrationQualifiesPublicProblemBeforeEnteringLocalTransactionAndRejectsHiddenLike() throws Exception {
        SolutionCommentMapper comments = mock(SolutionCommentMapper.class);
        SolutionQueryService queries = mock(SolutionQueryService.class);
        SolutionProblemReferenceService references = mock(SolutionProblemReferenceService.class);
        SolutionAccessService access = mock(SolutionAccessService.class);
        SolutionDomainMigrationGate gate = mock(SolutionDomainMigrationGate.class);
        CommentLikeLocalWriteService local = mock(CommentLikeLocalWriteService.class);
        SolutionComment rootRow = comment(ROOT_ID, null, null, CommentState.VISIBLE, AUTHOR_ID);
        SolutionRecord solutionRow = solution(AUTHOR_ID, false);
        when(comments.selectReplyTarget(ROOT_ID)).thenReturn(rootRow);
        when(queries.likeSnapshot(SOLUTION_ID)).thenReturn(solutionRow);
        when(references.refresh(Collections.singletonList(PROBLEM_ID)))
                .thenReturn(Collections.singletonMap(PROBLEM_ID, PUBLIC_PROBLEM));
        when(access.isPubliclyReadable(PUBLIC_PROBLEM)).thenReturn(true);
        when(local.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, ROOT_ID, true, PUBLIC_PROBLEM))
                .thenReturn(new LikeResultVo().setLiked(true).setLikeCount(1L));
        CommentLikeServiceImpl orchestrator = new CommentLikeServiceImpl(queries, references, access,
                comments, local, gate);

        assertTrue(orchestrator.like(ACTOR_ID, ROOT_ID).isLiked());
        org.mockito.InOrder order = inOrder(queries, references, access, local);
        order.verify(queries).likeSnapshot(SOLUTION_ID);
        order.verify(references).refresh(Collections.singletonList(PROBLEM_ID));
        order.verify(access).isPubliclyReadable(PUBLIC_PROBLEM);
        order.verify(local).setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, ROOT_ID, true, PUBLIC_PROBLEM);
        assertNull(CommentLikeServiceImpl.class.getMethod("like", Long.class, Long.class)
                .getAnnotation(Transactional.class));
        assertNotNull(CommentLikeLocalWriteService.class.getMethod("setState", Long.class, Long.class,
                Long.class, Long.class, boolean.class, ContentProblemReadVo.class)
                .getAnnotation(Transactional.class));

        when(references.refresh(Collections.singletonList(PROBLEM_ID)))
                .thenReturn(Collections.singletonMap(PROBLEM_ID, PRIVATE_PROBLEM));
        when(access.isPubliclyReadable(PRIVATE_PROBLEM)).thenReturn(false);
        assertEquals(404, assertThrows(ApiStatusException.class,
                () -> orchestrator.like(ACTOR_ID, ROOT_ID)).getStatusCode());
        verify(local, times(1)).setState(anyLong(), anyLong(), anyLong(), anyLong(), anyBoolean(), any());
    }

    @Test
    void lockOrderIsSolutionThenRootThenTargetThenLikeAndCountSqlIsGuarded() throws Exception {
        target = comment(COMMENT_ID, ROOT_ID, ROOT_ID, CommentState.VISIBLE, AUTHOR_ID);
        org.mockito.InOrder order = inOrder(solutionMapper, commentMapper, likeMapper);
        writer.setState(ACTOR_ID, SOLUTION_ID, ROOT_ID, COMMENT_ID, true, PUBLIC_PROBLEM);
        order.verify(solutionMapper).selectCommentTargetForUpdate(SOLUTION_ID);
        order.verify(commentMapper).selectRootForUpdate(ROOT_ID, SOLUTION_ID);
        order.verify(commentMapper).selectTargetForUpdate(COMMENT_ID, SOLUTION_ID);
        order.verify(likeMapper).selectByCommentAndUserForUpdate(COMMENT_ID, ACTOR_ID);

        Configuration configuration = new Configuration();
        parse(configuration, "/mapper/SolutionCommentMapper.xml");
        parse(configuration, "/mapper/CommentLikeMapper.xml");
        BoundSql rootLock = configuration.getMappedStatement(SolutionCommentMapper.class.getName()
                + ".selectRootForUpdate").getBoundSql(Map.of("rootId", ROOT_ID, "solutionId", SOLUTION_ID));
        BoundSql targetLock = configuration.getMappedStatement(SolutionCommentMapper.class.getName()
                + ".selectTargetForUpdate").getBoundSql(Map.of("commentId", COMMENT_ID, "solutionId", SOLUTION_ID));
        BoundSql relationLock = configuration.getMappedStatement(CommentLikeMapper.class.getName()
                + ".selectByCommentAndUserForUpdate").getBoundSql(Map.of("commentId", COMMENT_ID, "userId", ACTOR_ID));
        BoundSql decrement = configuration.getMappedStatement(SolutionCommentMapper.class.getName()
                + ".adjustLikeCount").getBoundSql(Map.of("commentId", COMMENT_ID, "delta", -1,
                "updatedAt", LocalDateTime.now(CLOCK)));
        BoundSql delete = configuration.getMappedStatement(SolutionCommentMapper.class.getName()
                + ".markVisibleDeleted").getBoundSql(Map.of("commentId", COMMENT_ID,
                "solutionId", SOLUTION_ID, "newState", CommentState.ADMIN_DELETED.name(),
                "operatorId", ACTOR_ID, "now", LocalDateTime.now(CLOCK)));
        assertTrue(normalize(rootLock.getSql()).endsWith("for update"));
        assertTrue(normalize(targetLock.getSql()).endsWith("for update"));
        assertTrue(normalize(relationLock.getSql()).endsWith("for update"));
        assertTrue(normalize(decrement.getSql()).contains("like_count+? >= 0"), decrement.getSql());
        assertFalse(normalize(delete.getSql()).contains("like_count"),
                "comment deletion keeps historical like relations and count untouched");
    }

    private void login(Long userId, String... authorities) {
        LoginUser login = new LoginUser();
        login.setUser(new SysUser().setUserId(userId));
        login.setAuths(List.of(authorities));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, null, login.getAuthorities()));
    }

    private static SolutionRecord solution(Long authorId, boolean commentsOpen) {
        SolutionRecord record = new SolutionRecord();
        record.setSolutionId(SOLUTION_ID);
        record.setProblemId(PROBLEM_ID);
        record.setUserId(authorId);
        record.setPrivate_(false);
        record.setDelFlag(false);
        record.setModerationState(SolutionModerationState.NORMAL);
        record.setCommentsOpen(commentsOpen);
        return record;
    }

    private static SolutionRecord copy(SolutionRecord record) {
        if (record == null) return null;
        SolutionRecord copy = solution(record.getUserId(), Boolean.TRUE.equals(record.getCommentsOpen()));
        copy.setPrivate_(record.getPrivate_());
        copy.setDelFlag(record.getDelFlag());
        copy.setModerationState(record.getModerationState());
        return copy;
    }

    private static SolutionComment comment(Long id, Long rootId, Long parentId,
                                           CommentState state, Long authorId) {
        return new SolutionComment().setCommentId(id).setSolutionId(SOLUTION_ID).setUserId(authorId)
                .setRootId(rootId).setParentId(parentId).setState(state).setLikeCount(0L);
    }

    private static SolutionComment copy(SolutionComment row) {
        if (row == null) return null;
        return new SolutionComment().setCommentId(row.getCommentId()).setSolutionId(row.getSolutionId())
                .setUserId(row.getUserId()).setRootId(row.getRootId()).setParentId(row.getParentId())
                .setState(row.getState()).setLikeCount(row.getLikeCount());
    }

    private void parse(Configuration configuration, String resource) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private String normalize(String sql) {
        return sql.toLowerCase().replaceAll("\\s+", " ").trim();
    }
}
