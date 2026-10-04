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

/** Read-only projection only; it is not an authorization cache or cross-service FK. */
@Data
@Accessors(chain = true)
@TableName("solution_problem_reference")
public class SolutionProblemReference implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "problem_id", type = IdType.INPUT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;
    private Boolean existsFlag;
    private Boolean delFlag;
    private Integer auth;
    private String title;
    private LocalDateTime checkedAt;
    private LocalDateTime requestedAt;
}
