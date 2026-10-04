package com.anishan.user.service.impl;

import com.anishan.api.event.PointAwardEvent;
import com.anishan.user.domain.entity.UserPointAwardReceipt;
import com.anishan.user.mapper.SysUserMapper;
import com.anishan.user.mapper.UserPointAwardReceiptMapper;
import com.anishan.user.service.PointAwardApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class PointAwardApplicationServiceImpl implements PointAwardApplicationService {

    private final UserPointAwardReceiptMapper receiptMapper;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PointAwardApplicationServiceImpl(UserPointAwardReceiptMapper receiptMapper,
                                            SysUserMapper sysUserMapper,
                                            ObjectMapper objectMapper,
                                            @org.springframework.beans.factory.annotation.Qualifier("userCommunityClock")
                                            Clock clock) {
        this.receiptMapper = receiptMapper;
        this.sysUserMapper = sysUserMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(PointAwardEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Points event is required");
        }
        event.validate(objectMapper);
        Long userId = Long.parseLong(event.getUserId());
        Long problemId = Long.parseLong(event.getProblemId());
        BigDecimal amount = parsePersistableAmount(event.getAmount());

        // Locking the user row prevents deletion between the visibility check and receipt insert.
        if (sysUserMapper.selectVisibleUserIdForUpdate(userId) == null) {
            log.warn("Point award skipped eventId={} userId={} problemId={} reason=USER_NOT_FOUND",
                    event.getEventId(), userId, problemId);
            return;
        }

        UserPointAwardReceipt receipt = new UserPointAwardReceipt();
        receipt.setUserId(userId);
        receipt.setProblemId(problemId);
        receipt.setEventId(event.getEventId());
        receipt.setAmount(amount);
        receipt.setCreatedAt(LocalDateTime.now(clock));
        try {
            if (receiptMapper.insertReceipt(receipt) != 1) {
                throw new IllegalStateException("Point receipt insert did not create a row");
            }
        } catch (DuplicateKeyException duplicate) {
            if (handleDuplicate(userId, problemId, event, amount)) {
                return;
            }
            throw new IllegalStateException("Point receipt uniqueness conflict");
        }

        if (sysUserMapper.addPoints(userId, amount) != 1) {
            throw new IllegalStateException("Point balance update failed");
        }
    }

    private boolean handleDuplicate(Long userId, Long problemId, PointAwardEvent event, BigDecimal amount) {
        UserPointAwardReceipt byUserProblem = receiptMapper.selectByUserAndProblem(userId, problemId);
        if (byUserProblem != null) {
            if (!Objects.equals(byUserProblem.getEventId(), event.getEventId())
                    || byUserProblem.getAmount() == null || byUserProblem.getAmount().compareTo(amount) != 0) {
                log.warn("Point award deduplicated eventId={} userId={} problemId={} reason=RECEIPT_ALREADY_EXISTS",
                        event.getEventId(), userId, problemId);
            }
            return true;
        }
        UserPointAwardReceipt byEventId = receiptMapper.selectByEventId(event.getEventId());
        if (byEventId != null && Objects.equals(byEventId.getUserId(), userId)
                && Objects.equals(byEventId.getProblemId(), problemId)
                && byEventId.getAmount() != null && byEventId.getAmount().compareTo(amount) == 0) {
            return true;
        }
        if (byEventId != null) {
            log.error("Point award event id collision eventId={} userId={} problemId={} code=EVENT_ID_CONFLICT",
                    event.getEventId(), userId, problemId);
        }
        // A different event may legitimately lose to the permanent user/problem receipt above,
        // but an eventId reused for another receipt is a producer/data-integrity failure. Do not ACK it.
        return false;
    }

    private static BigDecimal parsePersistableAmount(String amountText) {
        final BigDecimal amount;
        try {
            amount = new BigDecimal(amountText).setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException | NumberFormatException invalid) {
            throw new IllegalArgumentException("Invalid points amount precision");
        }
        if (amount.signum() <= 0 || amount.precision() > 10) {
            throw new IllegalArgumentException("Invalid points amount range");
        }
        return amount;
    }
}
