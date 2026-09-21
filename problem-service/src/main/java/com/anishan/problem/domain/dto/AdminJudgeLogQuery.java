package com.anishan.problem.domain.dto;

import com.anishan.commons.domain.dto.PagedQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理侧判题记录/测试用例日志查询参数。
 *
 * <p>状态使用兼容输入：既支持 JudgeResult 枚举名（如 ACCEPT），也支持对外值（如 AC）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("管理侧判题日志查询")
public class AdminJudgeLogQuery extends PagedQuery<Object> {

    @ApiModelProperty("提交ID")
    private Long submitId;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("题目ID")
    private Long problemId;

    @ApiModelProperty("比赛ID")
    private Long contestId;

    @ApiModelProperty("测试用例ID")
    private Long caseId;

    @ApiModelProperty("测试用例顺序")
    private Integer caseIndex;

    @ApiModelProperty("判题状态，可传 ACCEPT 或 AC")
    private String status;

    @ApiModelProperty("编程语言")
    private String language;
}
