package com.anishan.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Persistence model for a directed user follow relationship. */
@Data
@TableName("user_follow")
public class UserFollow implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long followerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long followeeId;

    private LocalDateTime createdAt;
}
