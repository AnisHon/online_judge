package com.anishan.problem.entity.vo;

import com.anishan.commons.e.JudgeResult;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("用户提交记录")
public class SubmitRecordVo {

    @ApiModelProperty("提交ID")
    private Long submitId;

    @ApiModelProperty("用户ID")
    private String userId;

    @ApiModelProperty("题目ID")
    private Long problemId;

    @ApiModelProperty("使用语言的id")
    private Long languageId;

    @ApiModelProperty("结果，取值范围 (AC, RE, WA, TLE, MLE)")
    private JudgeResult result;

    @ApiModelProperty("耗时 单位ms")
    private Integer time;

    @ApiModelProperty("内存使用 单位kb")
    private Integer memory;

    @ApiModelProperty("提交时间")
    private LocalDateTime submitTime;

}