package com.anishan.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Idempotency receipt for the first completion point award per user and problem. */
@Data
@TableName("user_point_award_receipt")
public class UserPointAwardReceipt implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;

    private String eventId;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
