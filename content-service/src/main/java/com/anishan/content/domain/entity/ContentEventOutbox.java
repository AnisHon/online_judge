package com.anishan.content.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Content-domain outbox; it must never claim problem-service point/judge events. */
@Data
@Accessors(chain = true)
@TableName(value = "content_event_outbox", autoResultMap = true)
public class ContentEventOutbox implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    private String eventId;
    private String dedupeKey;
    private String eventType;
    private String exchangeName;
    private String routingKey;
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
