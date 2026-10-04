package com.anishan.user.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Persistence model for a private, recipient-scoped notification. */
@Data
@TableName("user_notification")
public class UserNotification implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long notificationId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long recipientId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long actorId;

    private String eventId;
    private String dedupeKey;
    private String type;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long solutionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long commentId;

    private String action;
    private String reason;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
