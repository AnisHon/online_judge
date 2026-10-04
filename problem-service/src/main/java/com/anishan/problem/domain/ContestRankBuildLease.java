package com.anishan.problem.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Fencing token for one immutable candidate version of a contest rank. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestRankBuildLease {
    private Long contestId;
    private Long candidateVersion;
    private Long sourceSeq;
    private String leaseOwner;
    private String sourceMode;
    private String ruleVersion;
}
