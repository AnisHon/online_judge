package com.anishan.problem.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.util.List;
import java.util.Map;

/** Viewer-independent, rebuildable projection; never contains content-domain data. */
@Data
public class ProfileActivityBaseVo {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private long solvedCount;
    private long attemptedCount;
    private ProfileActivityHeatmapVo heatmap;
    private Map<String, List<String>> solvedProblems;
    private List<ProfileContestVo> contests;
    private boolean solvedProblemsTruncated;
    private boolean contestsTruncated;
}
