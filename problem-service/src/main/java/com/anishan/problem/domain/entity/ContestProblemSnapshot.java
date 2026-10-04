package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable contest-time roster metadata. Problem bodies and judge cases stay in their source tables. */
@Data
@TableName("contest_problem_snapshot")
public class ContestProblemSnapshot implements Serializable {
    @TableId(value = "contest_id", type = IdType.INPUT)
    private Long contestId;
    private Long problemId;
    private Integer problemOrder;
    private String title;
    private Integer problemType;
    private BigDecimal maxScore;
    private LocalDateTime createdAt;
}
