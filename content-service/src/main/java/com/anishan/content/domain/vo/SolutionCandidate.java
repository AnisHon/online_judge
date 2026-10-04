package com.anishan.content.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** Internal pagination cursor; never returned from a controller. */
@Data
public class SolutionCandidate {
    private Long solutionId;
    private Long problemId;
    private Long userId;
    private Long version;
    private Boolean topUp;
    private LocalDateTime createTime;
}
