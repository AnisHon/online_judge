package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Durable JSON event awaiting relay; its ID is a UUID rather than a numeric ASSIGN_ID. */
@Data
@Accessors(chain = true)
@TableName(value = "problem_event_outbox", autoResultMap = true)
public class ProblemEventOutbox implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private String eventId;

    private String dedupeKey;
    private String eventType;
    private String exchangeName;
    private String routingKey;
    /** Serialized JSON envelope; MySQL validates the JSON column on insert. */
    private String payload;
    private String status;
    private Integer attempts;
    private LocalDateTime nextAttemptAt;
    private String leaseOwner;
    private LocalDateTime leaseUntil;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
