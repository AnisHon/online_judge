package com.anishan.content.service.impl;

import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.api.util.AccountPolicy;
import com.anishan.content.domain.dto.*;
import com.anishan.content.domain.entity.SolutionExplanation;
import com.anishan.content.domain.entity.SolutionModerationAction;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.*;
import com.anishan.content.mapper.*;
import com.anishan.content.service.*;
import com.anishan.commons.exception.ApiStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.anishan.content.service.impl.SolutionTestFixtures.*;

class SolutionModerationTest {
    final SolutionExplanationMapper mapper = mock(SolutionExplanationMapper.class);
    final SolutionExplanationContentService content = mock(SolutionExplanationContentService.class);
    final SolutionModerationActionMapper audits = mock(SolutionModerationActionMapper.class);
    final ContentEventOutboxService outbox = mock(ContentEventOutboxService.class);
    final SolutionLocalWriteService writer = new SolutionLocalWriteService(mapper, content, audits, outbox, CLOCK);
    final SolutionQueryService query = mock(SolutionQueryService.class);
    final SolutionProblemReferenceService references = mock(SolutionProblemReferenceService.class);
    final SolutionAccessService access = new SolutionAccessService(null);
    final SolutionDomainMigrationGate gate = mock(SolutionDomainMigrationGate.class);
    final SolutionModerationService service = new SolutionModerationService(query, references, access, writer, audits, gate);

    @BeforeEach void setUp() {
        login(99L, "problem:solution:edit", "problem:solution:remove", "problem:solution:list");
        when(query.record(ID)).thenReturn(row());
        when(mapper.selectRecordForUpdate(ID)).thenReturn(row());
        when(mapper.moderateByVersion(anyLong(), anyLong(), anyString(), anyBoolean(), any(), any())).thenReturn(1);
        when(mapper.updateFieldsByVersion(anyLong(), anyLong(), anyLong(), anyString(), anyBoolean(), any(), any())).thenReturn(1);
        when(audits.insert(any(SolutionModerationAction.class))).thenAnswer(invocation -> {
            SolutionModerationAction audit = invocation.getArgument(0); audit.setActionId(ID + 1); return 1;
        });
        when(outbox.recordCommunity(any())).thenAnswer(invocation -> {
            CommunityEvent event = invocation.getArgument(0); event.validate(); return event.getEventId();
        });
        when(references.refresh(List.of(77L))).thenReturn(Map.of(77L, problem(true)));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    SolutionModerationRequest request(SolutionModerationRequest.Action action, String reason) {
        SolutionModerationRequest request = new SolutionModerationRequest();
        request.setAction(action); request.setReason(reason); return request;
    }
    List<CommunityEvent> events() {
        ArgumentCaptor<CommunityEvent> capture = ArgumentCaptor.forClass(CommunityEvent.class);
        verify(outbox, atLeastOnce()).recordCommunity(capture.capture());
        return capture.getAllValues();
    }

    @Test void restrictionWritesStateAuditAndContentFreeEventTogether() {
        assertEquals(SolutionModerationState.AUTHOR_ONLY,
                service.moderate(ID, request(SolutionModerationRequest.Action.AUTHOR_ONLY, "  内容不符合要求  ")));
        verifyNoInteractions(references);
        var order = inOrder(mapper, audits, outbox);
        order.verify(mapper).selectRecordForUpdate(ID);
        order.verify(mapper).moderateByVersion(eq(ID), eq(3L), eq("AUTHOR_ONLY"), eq(false), isNull(), any());
        order.verify(audits).insert(any(SolutionModerationAction.class));
        order.verify(outbox).recordCommunity(any());
        CommunityEvent event = events().get(0);
        assertEquals(CommunityEventType.SOLUTION_MODERATED, event.getEventType());
        assertEquals("solution-moderated:" + (ID + 1), event.getDedupeKey());
        assertEquals(List.of("42"), event.getRecipientIds()); assertEquals("99", event.getActorId());
        assertEquals("内容不符合要求", event.getReason());
        assertNull(event.getCommentId());
        assertEquals("2026-10-03T20:00+08:00", event.getOccurredAt());
    }

    @Test void duplicateCurrentStateEvenWithDifferentReasonDoesNotAuditOrNotify() {
        SolutionRecord restricted = row(); restricted.setModerationState(SolutionModerationState.AUTHOR_ONLY);
        when(mapper.selectRecordForUpdate(ID)).thenReturn(restricted);
        service.moderate(ID, request(SolutionModerationRequest.Action.AUTHOR_ONLY, "new reason"));
        verifyNoInteractions(audits, outbox);
        verify(mapper, never()).moderateByVersion(any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test void duplicateRestoreDoesNotRequireRemoteAvailabilityWhenNoStateChanges() {
        assertEquals(SolutionModerationState.NORMAL,
                service.moderate(ID, request(SolutionModerationRequest.Action.RESTORE, "no change")));
        verifyNoInteractions(references, audits, outbox);
    }

    @Test void restoreChecksProblemBeforeTakingSolutionLockAndPublishesOnlyOnce() {
        SolutionRecord restricted = row(); restricted.setModerationState(SolutionModerationState.AUTHOR_ONLY);
        when(query.record(ID)).thenReturn(restricted); when(mapper.selectRecordForUpdate(ID)).thenReturn(restricted);
        assertEquals(SolutionModerationState.NORMAL, service.moderate(ID, request(SolutionModerationRequest.Action.RESTORE, "已修正")));
        var order = inOrder(references, mapper);
        order.verify(references).refresh(List.of(77L)); order.verify(mapper).selectRecordForUpdate(ID);
        var events = events();
        assertEquals(2, events.size()); assertEquals(CommunityEventType.SOLUTION_PUBLISHED, events.get(1).getEventType());
        assertEquals(List.of(), events.get(1).getRecipientIds()); assertEquals("42", events.get(1).getActorId());
        assertEquals("solution-published:" + ID, events.get(1).getDedupeKey());
        verify(mapper).moderateByVersion(ID, 3L, "NORMAL", false, LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK));
        restricted.setModerationState(SolutionModerationState.NORMAL); restricted.setFirstPublishedAt(LocalDateTime.now(CLOCK));
        clearInvocations(outbox, audits);
        service.moderate(ID, request(SolutionModerationRequest.Action.RESTORE, "再次恢复"));
        verifyNoInteractions(outbox, audits);
    }

    @Test void restorePreservesPrivateChoiceAndCannotPublicizeContestContent() {
        SolutionRecord restricted = row(); restricted.setModerationState(SolutionModerationState.AUTHOR_ONLY);
        when(query.record(ID)).thenReturn(restricted); when(mapper.selectRecordForUpdate(ID)).thenReturn(restricted);
        when(references.refresh(anyList())).thenReturn(Map.of(77L, problem(false)));
        assertEquals(400, assertThrows(ApiStatusException.class, () -> service.moderate(ID,
                request(SolutionModerationRequest.Action.RESTORE, "已修正"))).getStatusCode());
        verifyNoInteractions(outbox, audits);
        restricted.setPrivate_(true);
        service.moderate(ID, request(SolutionModerationRequest.Action.RESTORE, "仅自己可见"));
        assertTrue(restricted.getPrivate_()); assertEquals(1, events().size());
        assertEquals(CommunityEventType.SOLUTION_MODERATED, events().get(0).getEventType());
    }

    @Test void concurrentEditFailsWith409WithoutAuditAndOutbox() {
        SolutionRecord changed = row(); changed.setVersion(4L);
        when(mapper.selectRecordForUpdate(ID)).thenReturn(changed);
        assertEquals(409, assertThrows(ApiStatusException.class, () -> service.moderate(ID,
                request(SolutionModerationRequest.Action.AUTHOR_ONLY, "reason"))).getStatusCode());
        verifyNoInteractions(outbox, audits);
    }

    @Test void deletionIsLogicalRetainsHistoryAndIsIdempotent() {
        service.delete(ID, "违反规则");
        verify(mapper).moderateByVersion(eq(ID), eq(3L), eq("NORMAL"), eq(true), isNull(), any());
        assertEquals("DELETE", events().get(0).getAction());
        verifyNoInteractions(content);
        SolutionRecord deleted = row(); deleted.setDelFlag(true);
        when(mapper.selectRecordForUpdate(ID)).thenReturn(deleted);
        clearInvocations(audits, outbox);
        service.delete(ID, "再次删除");
        verifyNoInteractions(audits, outbox);
    }

    @Test void authorBatchDeleteValidatesEntireBatchInSortedLockOrderBeforeAnyWrite() {
        SolutionRecord foreign = row(); foreign.setSolutionId(ID + 1); foreign.setUserId(77L);
        when(mapper.selectRecordForUpdate(ID + 1)).thenReturn(foreign);
        assertEquals(404, assertThrows(ApiStatusException.class,
                () -> writer.deleteOwned(List.of(ID + 1, ID), AUTHOR)).getStatusCode());
        verify(mapper, never()).softDeleteOwned(any(), any());
        var order = inOrder(mapper); order.verify(mapper).selectRecordForUpdate(ID);
        order.verify(mapper).selectRecordForUpdate(ID + 1);
    }

    @Test void batchDeleteHandlesDuplicatesDeletedRowsAndSizeBounds() {
        when(mapper.softDeleteOwned(List.of(ID), AUTHOR)).thenReturn(1);
        assertTrue(writer.deleteOwned(List.of(ID, ID), AUTHOR));
        SolutionRecord deleted = row(); deleted.setDelFlag(true);
        when(mapper.selectRecordForUpdate(ID)).thenReturn(deleted);
        clearInvocations(mapper);
        assertTrue(writer.deleteOwned(List.of(ID), AUTHOR));
        verify(mapper, never()).softDeleteOwned(any(), any());
        assertEquals(400, assertThrows(ApiStatusException.class, () -> writer.deleteOwned(
                LongStream.range(1, 52).boxed().collect(Collectors.toList()), AUTHOR)).getStatusCode());
        assertThrows(ApiStatusException.class, () -> writer.deleteOwned(List.of(0L), AUTHOR));
        assertThrows(ApiStatusException.class, () -> writer.deleteOwned(List.of(), AUTHOR));
    }

    @Test void initialPublicCreateStoresFirstPublishedMarkerAndOneOutboxEvent() {
        when(mapper.insert(any(SolutionExplanation.class))).thenAnswer(invocation -> {
            SolutionExplanation solution = invocation.getArgument(0); solution.setSolutionId(ID);
            assertEquals(LocalDateTime.now(CLOCK), solution.getFirstPublishedAt()); return 1;
        });
        assertTrue(writer.create(AUTHOR, dto(false), false));
        verify(content).save(ID, "body");
        assertEquals(CommunityEventType.SOLUTION_PUBLISHED, events().get(0).getEventType());
        assertEquals(1, events().size());
    }

    @Test void privateOrRestrictedEditsNeverPublishAndAuthorCannotResetRestriction() {
        SolutionRecord restricted = row(); restricted.setModerationState(SolutionModerationState.AUTHOR_ONLY);
        when(mapper.selectRecordForUpdate(ID)).thenReturn(restricted);
        writer.update(AUTHOR, dto(false).setSolutionId(ID), restricted, false);
        verify(mapper).updateFieldsByVersion(ID, AUTHOR, 3L, "edited", false, null, null);
        verifyNoInteractions(outbox, audits);
        assertEquals(SolutionModerationState.AUTHOR_ONLY, restricted.getModerationState());
    }

    @Test void firstPrivateToPublicEditUsesMarkerAndRepublishingDoesNotNotify() {
        SolutionRecord row = row(); row.setPrivate_(true);
        when(mapper.selectRecordForUpdate(ID)).thenReturn(row);
        writer.update(AUTHOR, dto(false).setSolutionId(ID), row, false);
        verify(mapper).updateFieldsByVersion(ID, AUTHOR, 3L, "edited", false, null, LocalDateTime.now(CLOCK));
        assertEquals(CommunityEventType.SOLUTION_PUBLISHED, events().get(0).getEventType());
        row.setFirstPublishedAt(LocalDateTime.now(CLOCK));
        clearInvocations(outbox);
        writer.update(AUTHOR, dto(false).setSolutionId(ID), row, false);
        verifyNoInteractions(outbox);
    }

    @Test void outboxFailurePropagatesToTransactionalBoundaryRatherThanAfterCommit() throws Exception {
        doThrow(new IllegalStateException("outbox unavailable")).when(outbox).recordCommunity(any());
        assertThrows(IllegalStateException.class, () -> service.moderate(ID,
                request(SolutionModerationRequest.Action.AUTHOR_ONLY, "reason")));
        Transactional boundary = SolutionLocalWriteService.class.getMethod("moderate", SolutionRecord.class,
                Long.class, String.class, String.class, boolean.class).getAnnotation(Transactional.class);
        assertTrue(Arrays.asList(boundary.rollbackFor()).contains(Exception.class));
        assertEquals(org.springframework.transaction.annotation.Propagation.REQUIRED, boundary.propagation());
    }

    @Test void reasonAndDenyPermissionRejectBeforeAnyStateWrite() {
        for (String reason : Arrays.asList(null, "", " ", "x".repeat(501), "\u0000bad", "\ud800")) {
            assertEquals(400, assertThrows(ApiStatusException.class, () -> service.delete(ID, reason)).getStatusCode());
        }
        verifyNoInteractions(mapper, audits, outbox);
        login(99L, "problem:solution:edit", AccountPolicy.SOLUTION_DENY);
        assertThrows(AccessDeniedException.class, () -> service.moderate(ID,
                request(SolutionModerationRequest.Action.AUTHOR_ONLY, "reason")));
        login(99L);
        assertThrows(AccessDeniedException.class, () -> service.delete(ID, "reason"));
    }

    @Test void authorHistoryRemainsAvailableAfterDeletionAndNeverExposesOtherAuthorsReasons() throws Exception {
        SolutionRecord deleted = row(); deleted.setDelFlag(true); when(query.record(ID)).thenReturn(deleted);
        ModerationActionVo own = new ModerationActionVo();
        own.setAction("DELETE"); own.setReason("reason"); own.setCreatedAt(LocalDateTime.now(CLOCK));
        when(audits.selectOwnSolutionActions(ID, AUTHOR)).thenReturn(List.of(own));
        login(AUTHOR);
        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(service.ownActions(ID));
        assertTrue(json.contains("reason")); assertFalse(json.contains("operatorId")); assertFalse(json.contains("targetId"));
        login(99L, "problem:solution:list");
        assertEquals(404, assertThrows(ApiStatusException.class, () -> service.ownActions(ID)).getStatusCode());
    }

    @Test void adminHistoryIsPagedBoundedAndSerializesAllIdsAsStrings() throws Exception {
        ModerationPageQuery page = new ModerationPageQuery();
        when(audits.countHistory(ID)).thenReturn(1L);
        ModerationActionVo audit = new ModerationActionVo(); audit.setActionId(ID + 1); audit.setTargetId(ID);
        when(audits.selectHistory(ID, 0, 20)).thenReturn(List.of(audit));
        var result = service.history(ID, page);
        assertEquals(1L, result.getTotalRecords());
        String json = new ObjectMapper().writeValueAsString(result);
        assertTrue(json.contains("\"targetId\":\"" + ID + "\""));
        page.setPageSize(51L);
        assertEquals(400, assertThrows(ApiStatusException.class, () -> service.history(ID, page)).getStatusCode());
    }

    @Test void springTransactionCoversAuditAndOutboxAndRollsBackOnEventFailure() {
        final java.util.concurrent.atomic.AtomicInteger commits = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger rollbacks = new java.util.concurrent.atomic.AtomicInteger();
        var manager = new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object tx, org.springframework.transaction.TransactionDefinition definition) {}
            protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) { commits.incrementAndGet(); }
            protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status) { rollbacks.incrementAndGet(); }
        };
        var factory = new org.springframework.aop.framework.ProxyFactory(writer);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(manager,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        SolutionLocalWriteService transactional = (SolutionLocalWriteService) factory.getProxy();
        doAnswer(invocation -> {
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            SolutionModerationAction audit = invocation.getArgument(0); audit.setActionId(ID + 1); return 1;
        }).when(audits).insert(any(SolutionModerationAction.class));
        doAnswer(invocation -> {
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            throw new IllegalStateException("event insert failure");
        }).when(outbox).recordCommunity(any());
        assertThrows(IllegalStateException.class, () -> transactional.moderate(row(), 99L, "AUTHOR_ONLY", "reason", false));
        assertEquals(0, commits.get()); assertEquals(1, rollbacks.get());
        // This verifies Spring's boundary, not physical MySQL row rollback.
    }
}
