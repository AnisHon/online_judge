package com.anishan.content.service;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.LikeResultVo;
import com.anishan.content.domain.vo.SolutionCandidate;
import com.anishan.content.domain.vo.SolutionCandidatePage;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.SolutionExplanationMapper;
import com.anishan.content.mapper.SolutionLikeMapper;
import com.anishan.content.service.impl.SolutionExplanationServiceImpl;
import com.anishan.content.service.impl.SolutionLikeServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SolutionLikeServiceTest {
    private static final Long SOLUTION_ID = 2098755579163770881L;
    private static final Long AUTHOR_ID = 101L;
    private static final Long ACTOR_ID = 202L;
    private static final Long PROBLEM_ID = 77L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final ContentProblemReadVo PUBLIC_PROBLEM =
            new ContentProblemReadVo("77", true, false, 1, true, false, "公开题");
    private static final ContentProblemReadVo PRIVATE_PROBLEM =
            new ContentProblemReadVo("77", true, false, 2, false, false, null);

    private SolutionExplanationMapper solutionMapper;
    private SolutionLikeMapper likeMapper;
    private ContentEventOutboxService outbox;
    private SolutionAccessService accessService;
    private SolutionLikeLocalWriteService writer;
    private AtomicReference<com.anishan.content.domain.entity.SolutionLike> relation;
    private AtomicLong likeCount;
    private SolutionRecord target;

    @BeforeEach
    void setUp() {
        solutionMapper = mock(SolutionExplanationMapper.class);
        likeMapper = mock(SolutionLikeMapper.class);
        outbox = mock(ContentEventOutboxService.class);
        accessService = mock(SolutionAccessService.class);
        relation = new AtomicReference<>();
        likeCount = new AtomicLong();
        target = target(AUTHOR_ID);
        writer = new SolutionLikeLocalWriteService(solutionMapper, likeMapper, accessService, outbox, CLOCK);

        when(solutionMapper.selectLikeTargetForUpdate(SOLUTION_ID)).thenAnswer(ignored -> copy(target));
        when(solutionMapper.selectLikeCount(SOLUTION_ID)).thenAnswer(ignored -> likeCount.get());
        when(solutionMapper.adjustLikeCount(eq(SOLUTION_ID), anyInt())).thenAnswer(invocation -> {
            int delta = invocation.getArgument(1);
            long next = likeCount.get() + delta;
            if (next < 0) return 0;
            likeCount.set(next);
            return 1;
        });
        when(likeMapper.selectBySolutionAndUserForUpdate(SOLUTION_ID, ACTOR_ID))
                .thenAnswer(ignored -> relation.get());
        when(likeMapper.insertState(any())).thenAnswer(invocation -> {
            relation.set(invocation.getArgument(0));
            return 1;
        });
        when(likeMapper.updateActiveState(eq(SOLUTION_ID), eq(ACTOR_ID), anyBoolean(), any()))
                .thenAnswer(invocation -> {
                    com.anishan.content.domain.entity.SolutionLike current = relation.get();
                    if (current == null) return 0;
                    current.setActive(invocation.getArgument(2));
                    current.setUpdatedAt(invocation.getArgument(3));
                    return 1;
                });
        when(likeMapper.markFirstNotified(eq(SOLUTION_ID), eq(ACTOR_ID), any())).thenAnswer(ignored -> {
            com.anishan.content.domain.entity.SolutionLike current = relation.get();
            if (current == null || !Boolean.TRUE.equals(current.getActive())
                    || Boolean.TRUE.equals(current.getFirstNotified())) return 0;
            current.setFirstNotified(true);
            return 1;
        });
        when(accessService.isPubliclyReadable(PUBLIC_PROBLEM)).thenReturn(true);
        when(accessService.isPubliclyReadable(PRIVATE_PROBLEM)).thenReturn(false);
    }

    @Test
    void firstLikeUpdatesRelationCounterAndWritesExactlyOneAuthorEvent() {
        LikeResultVo result = writer.setState(ACTOR_ID, SOLUTION_ID, true, PUBLIC_PROBLEM);

        assertTrue(result.isLiked());
        assertEquals(1L, result.getLikeCount());
        assertTrue(relation.get().getActive());
        assertTrue(relation.get().getFirstNotified());
        var event = captureEvent();
        assertEquals(CommunityEventType.SOLUTION_LIKED, event.getEventType());
        assertEquals("solution-liked:" + SOLUTION_ID + ":" + ACTOR_ID, event.getDedupeKey());
        assertEquals(Collections.singletonList(String.valueOf(AUTHOR_ID)), event.getRecipientIds());
        assertEquals(String.valueOf(ACTOR_ID), event.getActorId());
        event.validate();
    }

    @Test
    void duplicatePutAndRepeatedDeleteAreIdempotentAndCannotMakeCountNegative() {
        writer.setState(ACTOR_ID, SOLUTION_ID, true, PUBLIC_PROBLEM);
        writer.setState(ACTOR_ID, SOLUTION_ID, true, PUBLIC_PROBLEM);
        assertEquals(1L, likeCount.get());
        assertEquals(1, eventCount());

        LikeResultVo removed = writer.setState(ACTOR_ID, SOLUTION_ID, false, PUBLIC_PROBLEM);
        LikeResultVo repeated = writer.setState(ACTOR_ID, SOLUTION_ID, false, PUBLIC_PROBLEM);
        assertFalse(removed.isLiked());
        assertEquals(0L, removed.getLikeCount());
        assertFalse(repeated.isLiked());
        assertEquals(0L, repeated.getLikeCount());
        assertEquals(0L, likeCount.get());
        assertEquals(1, eventCount());
    }

    @Test
    void canceledAndRecreatedLikeDoesNotNotifyAgainAndSelfLikeIsConsumedWithoutNotification() {
        writer.setState(ACTOR_ID, SOLUTION_ID, true, PUBLIC_PROBLEM);
        writer.setState(ACTOR_ID, SOLUTION_ID, false, PUBLIC_PROBLEM);
        writer.setState(ACTOR_ID, SOLUTION_ID, true, PUBLIC_PROBLEM);
        assertEquals(1L, likeCount.get());
        assertEquals(1, eventCount());

        relation.set(null);
        likeCount.set(0L);
        target = target(ACTOR_ID);
        clearInvocations(outbox);
        writer.setState(ACTOR_ID, SOLUTION_ID, true, PUBLIC_PROBLEM);
        assertTrue(relation.get().getFirstNotified());
        verifyNoInteractions(outbox);
    }

    @Test
    void hiddenSolutionCanBeUnlikedButNeverReturnsItsCount() {
        relation.set(new com.anishan.content.domain.entity.SolutionLike()
                .setSolutionId(SOLUTION_ID).setUserId(ACTOR_ID).setActive(true).setFirstNotified(true));
        likeCount.set(1L);
        target.setPrivate_(true);

        LikeResultVo result = writer.setState(ACTOR_ID, SOLUTION_ID, false, PRIVATE_PROBLEM);
        assertFalse(result.isLiked());
        assertNull(result.getLikeCount());
        assertEquals(0L, likeCount.get());
        verifyNoInteractions(outbox);
    }

    @Test
    void noPreviousRelationDeleteIsFalseNullAndDoesNotCreateARow() {
        LikeResultVo result = writer.setState(ACTOR_ID, SOLUTION_ID, false, PUBLIC_PROBLEM);
        assertFalse(result.isLiked());
        assertNull(result.getLikeCount());
        assertEquals(0L, likeCount.get());
        verify(likeMapper, never()).insertState(any());
        verify(likeMapper, never()).updateActiveState(any(), any(), anyBoolean(), any());
    }

    @Test
    void hiddenTargetCannotBeLikedAndActiveStatusIsOnlyFetchedForTheViewer() {
        assertEquals(404, assertThrows(ApiStatusException.class,
                () -> writer.setState(ACTOR_ID, SOLUTION_ID, true, PRIVATE_PROBLEM)).getStatusCode());
        verifyNoInteractions(likeMapper, outbox);

        SolutionLikeMapper reads = mock(SolutionLikeMapper.class);
        when(reads.selectActiveSolutionIdsForUser(eq(ACTOR_ID), anyList()))
                .thenReturn(List.of(SOLUTION_ID));
        SolutionLikeServiceImpl reader = new SolutionLikeServiceImpl(null, null, null, reads, null, null);
        assertEquals(Set.of(SOLUTION_ID), reader.likedSolutionIds(ACTOR_ID, List.of(SOLUTION_ID, 2098755579163770882L)));
        verify(reads, times(1)).selectActiveSolutionIdsForUser(eq(ACTOR_ID), anyList());
        assertThrows(ApiStatusException.class,
                () -> reader.likedSolutionIds(ACTOR_ID, ids(51)));
    }

    @Test
    void oneHundredConcurrentPutRequestsSerializeToOneStateTransition() throws Exception {
        SolutionQueryService query = mock(SolutionQueryService.class);
        SolutionProblemReferenceService references = mock(SolutionProblemReferenceService.class);
        SolutionDomainMigrationGate gate = mock(SolutionDomainMigrationGate.class);
        when(query.likeSnapshot(SOLUTION_ID)).thenReturn(target(AUTHOR_ID));
        when(references.refresh(List.of(PROBLEM_ID))).thenReturn(Map.of(PROBLEM_ID, PUBLIC_PROBLEM));
        SolutionLikeLocalWriteService serializedWriter = new SolutionLikeLocalWriteService(solutionMapper,
                likeMapper, accessService, outbox, CLOCK) {
            @Override
            public synchronized LikeResultVo setState(Long userId, Long solutionId, boolean requestedLike,
                                                      ContentProblemReadVo freshProblem) {
                return super.setState(userId, solutionId, requestedLike, freshProblem);
            }
        };
        SolutionLikeServiceImpl service = new SolutionLikeServiceImpl(query, references, accessService,
                likeMapper, serializedWriter, gate);

        ExecutorService executor = Executors.newFixedThreadPool(16);
        try {
            List<Future<LikeResultVo>> requests = new ArrayList<>();
            for (int i = 0; i < 100; i++) requests.add(executor.submit(() -> service.like(ACTOR_ID, SOLUTION_ID)));
            for (Future<LikeResultVo> request : requests) {
                assertTrue(request.get().isLiked());
                assertEquals(1L, request.get().getLikeCount());
            }
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1L, likeCount.get());
        assertTrue(relation.get().getActive());
        assertTrue(relation.get().getFirstNotified());
        verify(likeMapper, times(1)).insertState(any());
        verify(outbox, times(1)).recordCommunity(any());
    }

    @Test
    void publicQualificationCompletesBeforeTheTransactionalLocalWriterIsCalled() throws Exception {
        SolutionQueryService query = mock(SolutionQueryService.class);
        SolutionProblemReferenceService references = mock(SolutionProblemReferenceService.class);
        SolutionAccessService access = mock(SolutionAccessService.class);
        SolutionLikeMapper reads = mock(SolutionLikeMapper.class);
        SolutionLikeLocalWriteService localWriter = mock(SolutionLikeLocalWriteService.class);
        SolutionDomainMigrationGate gate = mock(SolutionDomainMigrationGate.class);
        when(query.likeSnapshot(SOLUTION_ID)).thenReturn(target(AUTHOR_ID));
        when(references.refresh(List.of(PROBLEM_ID))).thenReturn(Map.of(PROBLEM_ID, PUBLIC_PROBLEM));
        when(access.isPubliclyReadable(PUBLIC_PROBLEM)).thenReturn(true);
        when(localWriter.setState(ACTOR_ID, SOLUTION_ID, true, PUBLIC_PROBLEM))
                .thenReturn(new LikeResultVo().setLiked(true).setLikeCount(1L));
        SolutionLikeServiceImpl service = new SolutionLikeServiceImpl(query, references, access, reads,
                localWriter, gate);

        assertTrue(service.like(ACTOR_ID, SOLUTION_ID).isLiked());
        org.mockito.InOrder order = inOrder(query, references, access, localWriter);
        order.verify(query).likeSnapshot(SOLUTION_ID);
        order.verify(references).refresh(List.of(PROBLEM_ID));
        order.verify(access).isPubliclyReadable(PUBLIC_PROBLEM);
        order.verify(localWriter).setState(ACTOR_ID, SOLUTION_ID, true, PUBLIC_PROBLEM);
        assertNull(SolutionLikeServiceImpl.class.getMethod("like", Long.class, Long.class)
                .getAnnotation(Transactional.class));
        assertNotNull(SolutionLikeLocalWriteService.class.getMethod("setState", Long.class, Long.class,
                boolean.class, ContentProblemReadVo.class).getAnnotation(Transactional.class));
    }

    @Test
    void listDetailAndRecentPopulateLikedByMeWithoutAViewerSharedCache() {
        SolutionQueryService query = mock(SolutionQueryService.class);
        SolutionProblemReferenceService references = mock(SolutionProblemReferenceService.class);
        SolutionAccessService access = mock(SolutionAccessService.class);
        SolutionLikeService likes = mock(SolutionLikeService.class);
        SolutionDomainMigrationGate gate = mock(SolutionDomainMigrationGate.class);
        UserInternalClient users = mock(UserInternalClient.class);
        SolutionRecord row = target(AUTHOR_ID);
        SolutionExplanationServiceImpl service = new SolutionExplanationServiceImpl(query, access, references,
                gate, mock(com.anishan.content.service.SolutionLocalWriteService.class), likes, users);
        when(query.record(SOLUTION_ID)).thenReturn(row);
        when(references.refresh(List.of(PROBLEM_ID))).thenReturn(Map.of(PROBLEM_ID, PUBLIC_PROBLEM));
        when(access.isPubliclyReadable(PUBLIC_PROBLEM)).thenReturn(true);
        when(likes.isLiked(ACTOR_ID, SOLUTION_ID)).thenReturn(true);

        assertTrue(service.get(SOLUTION_ID, ACTOR_ID).isLikedByMe());
        verify(likes).isLiked(ACTOR_ID, SOLUTION_ID);

        SolutionCandidate candidate = new SolutionCandidate();
        candidate.setSolutionId(SOLUTION_ID);
        candidate.setProblemId(PROBLEM_ID);
        candidate.setUserId(AUTHOR_ID);
        candidate.setVersion(3L);
        candidate.setTopUp(false);
        candidate.setCreateTime(LocalDateTime.of(2026, 10, 1, 0, 0));
        when(query.firstPage(ACTOR_ID, false, false, null, null, 0L))
                .thenReturn(new SolutionCandidatePage(1L, List.of(candidate)));
        when(query.records(List.of(SOLUTION_ID))).thenReturn(List.of(row));
        when(likes.likedSolutionIds(ACTOR_ID, List.of(SOLUTION_ID))).thenReturn(Set.of(SOLUTION_ID));
        assertTrue(service.pagedQuery(ACTOR_ID, page()).getData().get(0).isLikedByMe());

        when(query.firstPage(ACTOR_ID, false, true, null, null, 0L))
                .thenReturn(new SolutionCandidatePage(1L, List.of(candidate)));
        assertTrue(service.recent(ACTOR_ID).get(0).isLikedByMe());
        verify(likes, times(2)).likedSolutionIds(ACTOR_ID, List.of(SOLUTION_ID));
    }

    @Test
    void mapperLocksParentThenRelationAndCountUpdateCannotGoNegative() throws Exception {
        Configuration configuration = new Configuration();
        parse(configuration, "/mapper/SolutionExplanationMapper.xml");
        parse(configuration, "/mapper/SolutionLikeMapper.xml");
        BoundSql parentLock = configuration.getMappedStatement(
                SolutionExplanationMapper.class.getName() + ".selectLikeTargetForUpdate")
                .getBoundSql(Map.of("solutionId", SOLUTION_ID));
        BoundSql relationLock = configuration.getMappedStatement(
                SolutionLikeMapper.class.getName() + ".selectBySolutionAndUserForUpdate")
                .getBoundSql(Map.of("solutionId", SOLUTION_ID, "userId", ACTOR_ID));
        BoundSql decrement = configuration.getMappedStatement(
                SolutionExplanationMapper.class.getName() + ".adjustLikeCount")
                .getBoundSql(Map.of("solutionId", SOLUTION_ID, "delta", -1));
        String targetSql = normalize(parentLock.getSql());
        assertTrue(targetSql.endsWith("for update"), targetSql);
        assertFalse(targetSql.contains("solution_explanation_content"), targetSql);
        assertTrue(normalize(relationLock.getSql()).endsWith("for update"));
        assertTrue(normalize(decrement.getSql()).contains("like_count + ? >= 0"), decrement.getSql());
        assertNotNull(SolutionLikeLocalWriteService.class.getMethod("setState", Long.class, Long.class,
                boolean.class, ContentProblemReadVo.class).getAnnotation(Transactional.class));
    }

    @Test
    void responseAndEventKeepLongIdsSafeAndExplicitlySerializeHiddenCountAsNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode hidden = mapper.readTree(mapper.writeValueAsBytes(new LikeResultVo().setLiked(false).setLikeCount(null)));
        assertTrue(hidden.has("likeCount"));
        assertTrue(hidden.get("likeCount").isNull());
        assertFalse(hidden.get("liked").asBoolean());

        CommunityEvent event = new CommunityEvent().setSchemaVersion(1)
                .setEventId("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
                .setEventType(CommunityEventType.SOLUTION_LIKED)
                .setDedupeKey("solution-liked:9223372036854775807:9223372036854775806")
                .setOccurredAt("2026-10-03T20:00:00+08:00")
                .setActorId("9223372036854775806").setSolutionId("9223372036854775807")
                .setRecipientIds(List.of("9223372036854775805"));
        JsonNode json = mapper.readTree(mapper.writeValueAsBytes(event));
        assertTrue(json.get("solutionId").isTextual());
        assertEquals("9223372036854775807", json.get("solutionId").asText());
        assertTrue(json.get("actorId").isTextual());
        event.validate(mapper);
    }

    private CommunityEvent captureEvent() {
        org.mockito.ArgumentCaptor<CommunityEvent> captor = org.mockito.ArgumentCaptor.forClass(CommunityEvent.class);
        verify(outbox).recordCommunity(captor.capture());
        return captor.getValue();
    }

    private int eventCount() {
        return mockingDetails(outbox).getInvocations().size();
    }

    private static SolutionRecord target(Long authorId) {
        SolutionRecord row = new SolutionRecord();
        row.setSolutionId(SOLUTION_ID);
        row.setProblemId(PROBLEM_ID);
        row.setUserId(authorId);
        row.setPrivate_(false);
        row.setDelFlag(false);
        row.setModerationState(SolutionModerationState.NORMAL);
        row.setVersion(3L);
        row.setLikeCount(0L);
        row.setContent(null);
        return row;
    }

    private static SolutionRecord copy(SolutionRecord row) {
        SolutionRecord copy = new SolutionRecord();
        copy.setSolutionId(row.getSolutionId());
        copy.setProblemId(row.getProblemId());
        copy.setUserId(row.getUserId());
        copy.setPrivate_(row.getPrivate_());
        copy.setDelFlag(row.getDelFlag());
        copy.setModerationState(row.getModerationState());
        copy.setVersion(row.getVersion());
        copy.setLikeCount(row.getLikeCount());
        return copy;
    }

    private static List<Long> ids(int count) {
        List<Long> ids = new ArrayList<>(count);
        for (long id = 1; id <= count; id++) ids.add(id);
        return ids;
    }

    private static com.anishan.content.domain.dto.PagedSolution page() {
        com.anishan.content.domain.dto.PagedSolution page = new com.anishan.content.domain.dto.PagedSolution();
        page.setCurrentPage(1L);
        page.setPageSize(20L);
        return page;
    }

    private static void parse(Configuration configuration, String resource) throws Exception {
        try (InputStream input = SolutionLikeServiceTest.class.getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private static String normalize(String sql) {
        return sql.toLowerCase().replaceAll("\\s+", " ").trim();
    }
}
