package com.anishan.problem.domain.entity;

import com.anishan.problem.domain.enumeration.ContestAttemptState;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Durable accepted attempt metadata; source code and non-OJ answers remain in their existing tables. */
@Data
@TableName("contest_attempt")
public class ContestAttempt implements Serializable {
    @TableId(value = "attempt_id", type = IdType.ASSIGN_ID)
    private Long attemptId;
    private Long contestId;
    private Long userId;
    private Long problemId;
    private Long attemptSeq;
    private Long submitId;
    private String kind;
    private LocalDateTime acceptedAt;
    private ContestAttemptState state;
    private String judgeStatus;
    private BigDecimal score;
    private Boolean correct;
    private LocalDateTime completedAt;
}
