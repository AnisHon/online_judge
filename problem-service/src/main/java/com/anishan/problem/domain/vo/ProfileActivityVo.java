package com.anishan.problem.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 个人主页的学习活动摘要。
 *
 * <p>这是有意设计成摘要而不是分页明细：主页只需要有限数量的入口，完整记录仍由原有业务接口负责。</p>
 */
@Data
public class ProfileActivityVo {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private boolean owner;
    private long solvedCount;
    private long attemptedCount;
    private Map<String, List<String>> solvedProblems;
    private List<ProfileContestVo> contests;
    private List<ProfileSolutionVo> solutions;
    private boolean solvedProblemsTruncated;
    private boolean contestsTruncated;
    private boolean solutionsTruncated;
}
