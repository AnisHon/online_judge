package com.anishan.content.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SolutionCandidatePage {
    private long total;
    private List<SolutionCandidate> candidates;
}
