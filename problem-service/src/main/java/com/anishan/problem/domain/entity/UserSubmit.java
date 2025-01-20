package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 用户提交表
 * @TableName user_submit
 */
@TableName(value ="user_submit")
@Data
public class UserSubmit {
    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 比赛ID
     */
    @TableField(value = "contest_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long contestId;

    /**
     * 提交时间
     */
    @TableField(value = "submit_time")
    private LocalDateTime submitTime;

}