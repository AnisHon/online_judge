package com.anishan.problem.domain.entity;

import com.anishan.problem.domain.enumeration.ContestRankState;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Version/lease header for immutable final-rank entry generations. */
@Data
@TableName("contest_rank_snapshot")
public class ContestRankSnapshot implements Serializable {
    @TableId(value = "contest_id", type = IdType.INPUT)
    private Long contestId;
    private ContestRankState state;
    private Long version;
    private Long nextVersion;
    private Integer buildAttempts;
    private LocalDateTime nextBuildAt;
    private Long sourceSeq;
    private String sourceMode;
    private String ruleVersion;
    private Long totalUsers;
    private Long pendingCount;
    private String leaseOwner;
    private LocalDateTime leaseUntil;
    private LocalDateTime generatedAt;
    private String lastError;
    private LocalDateTime updatedAt;
}
