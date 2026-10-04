package com.anishan.user.service;

import com.anishan.api.event.PointAwardEvent;
import com.anishan.user.domain.entity.UserPointAwardReceipt;
import com.anishan.user.mapper.SysUserMapper;
import com.anishan.user.mapper.UserPointAwardReceiptMapper;
import com.anishan.user.service.impl.PointAwardApplicationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PointAwardApplicationTest {

    private final UserPointAwardReceiptMapper receiptMapper = mock(UserPointAwardReceiptMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final PointAwardApplicationServiceImpl service = new PointAwardApplicationServiceImpl(
            receiptMapper, sysUserMapper, new ObjectMapper(),
            Clock.fixed(Instant.parse("2026-10-03T04:00:00Z"), ZoneId.of("Asia/Shanghai")));

    @Test
    void newReceiptAndOriginalPointUpdateRunInThatOrder() throws NoSuchMethodException {
        when(sysUserMapper.selectVisibleUserIdForUpdate(7L)).thenReturn(7L);
        when(receiptMapper.insertReceipt(any())).thenReturn(1);
        when(sysUserMapper.addPoints(7L, new BigDecimal("2.00"))).thenReturn(1);

        service.apply(event("2.00"));

        org.mockito.InOrder order = inOrder(sysUserMapper, receiptMapper);
        order.verify(sysUserMapper).selectVisibleUserIdForUpdate(7L);
        order.verify(receiptMapper).insertReceipt(any(UserPointAwardReceipt.class));
        order.verify(sysUserMapper).addPoints(7L, new BigDecimal("2.00"));
        org.mockito.ArgumentCaptor<UserPointAwardReceipt> receiptCaptor =
                forClass(UserPointAwardReceipt.class);
        verify(receiptMapper).insertReceipt(receiptCaptor.capture());
        assertEquals(new BigDecimal("2.00"), receiptCaptor.getValue().getAmount());
        assertEquals(Exception.class, PointAwardApplicationServiceImpl.class
                .getMethod("apply", PointAwardEvent.class).getAnnotation(Transactional.class)
                .rollbackFor()[0]);
    }

    @Test
    void duplicateUserProblemReceiptNeverAddsPointsAgain() {
        UserPointAwardReceipt existing = receipt("old-event-id", "2.00");
        when(sysUserMapper.selectVisibleUserIdForUpdate(7L)).thenReturn(7L);
        when(receiptMapper.insertReceipt(any())).thenThrow(new DuplicateKeyException("duplicate"));
        when(receiptMapper.selectByUserAndProblem(7L, 11L)).thenReturn(existing);

        service.apply(event("2.00").setEventId("6ba7b810-9dad-11d1-80b4-00c04fd430c8"));

        verify(sysUserMapper, never()).addPoints(eq(7L), any(BigDecimal.class));
    }

    @Test
    void reusedEventIdForAnotherUserProblemIsNotAcknowledgedAsADuplicate() {
        UserPointAwardReceipt conflicting = receipt("550e8400-e29b-41d4-a716-446655440000", "2.00");
        conflicting.setUserId(99L);
        conflicting.setProblemId(12L);
        when(sysUserMapper.selectVisibleUserIdForUpdate(7L)).thenReturn(7L);
        when(receiptMapper.insertReceipt(any())).thenThrow(new DuplicateKeyException("duplicate event id"));
        when(receiptMapper.selectByUserAndProblem(7L, 11L)).thenReturn(null);
        when(receiptMapper.selectByEventId("550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(conflicting);

        assertThrows(IllegalStateException.class, () -> service.apply(event("2.00")));
        verify(sysUserMapper, never()).addPoints(eq(7L), any(BigDecimal.class));
    }

    @Test
    void deletedOrMissingUserIsAcknowledgedWithoutAnEndlessReceiptFailure() {
        when(sysUserMapper.selectVisibleUserIdForUpdate(7L)).thenReturn(null);

        service.apply(event("2.00"));

        verify(receiptMapper, never()).insertReceipt(any());
        verify(sysUserMapper, never()).addPoints(eq(7L), any(BigDecimal.class));
    }

    @Test
    void pointUpdateFailurePropagatesSoReceiptAndPointsRollBackTogether() throws NoSuchMethodException {
        when(sysUserMapper.selectVisibleUserIdForUpdate(7L)).thenReturn(7L);
        when(receiptMapper.insertReceipt(any())).thenReturn(1);
        when(sysUserMapper.addPoints(7L, new BigDecimal("2.00"))).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.apply(event("2.00")));
        assertEquals(Exception.class, PointAwardApplicationServiceImpl.class
                .getMethod("apply", PointAwardEvent.class).getAnnotation(Transactional.class)
                .rollbackFor()[0]);
    }

    @Test
    void amountMustFitReceiptDecimalWithoutRounding() {
        assertThrows(IllegalArgumentException.class, () -> service.apply(event("2.001")));
        verify(sysUserMapper, never()).selectVisibleUserIdForUpdate(any());
    }

    private static UserPointAwardReceipt receipt(String eventId, String amount) {
        UserPointAwardReceipt receipt = new UserPointAwardReceipt();
        receipt.setUserId(7L);
        receipt.setProblemId(11L);
        receipt.setEventId(eventId);
        receipt.setAmount(new BigDecimal(amount));
        receipt.setCreatedAt(LocalDateTime.of(2026, 10, 3, 12, 0));
        return receipt;
    }

    private static PointAwardEvent event(String amount) {
        return new PointAwardEvent().setSchemaVersion(1)
                .setEventId("550e8400-e29b-41d4-a716-446655440000")
                .setDedupeKey("points-awarded:7:11")
                .setOccurredAt("2026-10-03T12:00:00.123+08:00")
                .setUserId("7").setProblemId("11").setAmount(amount);
    }
}
