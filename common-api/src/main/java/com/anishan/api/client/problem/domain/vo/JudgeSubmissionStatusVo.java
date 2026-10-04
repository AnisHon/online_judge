package com.anishan.api.client.problem.domain.vo;

import com.anishan.commons.enumeration.JudgeResult;
import lombok.Data;

/** Minimal internal status used by judge-server to skip already-applied Rabbit deliveries. */
@Data
public class JudgeSubmissionStatusVo {

    private Boolean resultApplied;
    private JudgeResult status;
}
