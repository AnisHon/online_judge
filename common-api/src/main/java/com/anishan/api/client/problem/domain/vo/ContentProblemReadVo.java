package com.anishan.api.client.problem.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Safe problem metadata and content-domain eligibility; never includes a statement or answer. */
@Data
@AllArgsConstructor
public class ContentProblemReadVo {
    private String problemId;
    private boolean exists;
    private boolean deleted;
    private Integer auth;
    private boolean publicReadable;
    private boolean privateWritable;
    private String title;
}
