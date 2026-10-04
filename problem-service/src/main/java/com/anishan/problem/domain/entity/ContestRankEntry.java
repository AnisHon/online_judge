package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One user's aggregate in an immutable contest rank version. */
@Data
@TableName("contest_rank_entry")
public class ContestRankEntry implements Serializable {
    // The table has a composite key; mapper writes use explicit SQL and never use this as a logical identity.
    @TableId(value = "contest_id", type = IdType.INPUT)
    private Long contestId;
    private Long version;
    private Long userId;
    private Long rankNo;
    private Long rowPosition;
    private BigDecimal score;
    private Integer correctCount;
    private Integer answeredCount;
    private Boolean handedIn;
    private LocalDateTime createdAt;
}
