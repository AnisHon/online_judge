package com.anishan.content.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("solution_moderation_action")
public class SolutionModerationAction implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "action_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long actionId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long solutionId;
    private String targetType;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long authorId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operatorId;
    private String action;
    private String reason;
    private LocalDateTime createdAt;
}
