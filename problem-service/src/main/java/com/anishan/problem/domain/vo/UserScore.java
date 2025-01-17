package com.anishan.problem.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel("单个用户每道题分数")
public class UserScore {
    @ApiModelProperty("题目ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;

    @ApiModelProperty("题目")
    private String title;

    @ApiModelProperty("是否正确, null代表没交")
    private Boolean correct;

    @ApiModelProperty("得分,  null代表没交")
    private BigDecimal score;
}
