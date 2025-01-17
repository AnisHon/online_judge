package com.anishan.problem.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel("题目信息统计")
public class ProblemStatistic {
    @ApiModelProperty("问题Id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;

    @ApiModelProperty("题目设定分数")
    private String title;

    @ApiModelProperty("题目分数")
    private BigDecimal score;

    @ApiModelProperty("正确人数")
    private Integer rightNum;

    @ApiModelProperty("错误人数")
    private Integer wrongNum;

    @ApiModelProperty("未交人数")
    private Integer absentNum;

    @ApiModelProperty("平均分")
    private BigDecimal average;

}
