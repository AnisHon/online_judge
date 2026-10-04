package com.anishan.user.domain.entity;

import com.anishan.user.domain.enumeration.NotificationFanoutStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Durable cursor and lease for notifying followers of a published solution. */
@Data
@TableName("notification_fanout_job")
public class NotificationFanoutJob implements Serializable {

    @TableId(type = IdType.INPUT)
    private String eventId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long actorId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long solutionId;

    private LocalDateTime occurredAt;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long cursorUserId;

    private NotificationFanoutStatus status;
    private String leaseOwner;
    private LocalDateTime leaseUntil;
    private Integer attempts;
    private LocalDateTime nextAttemptAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
