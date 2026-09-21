package com.anishan.problem.domain.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 题单中单道题目的排序信息。
 */
@Data
@ApiModel("题单题目排序项")
public class ProblemListOrderItemDto {

    @NotNull
    @ApiModelProperty("题目 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;

    @NotNull
    @Min(1)
    @ApiModelProperty("题目顺序，从 1 开始")
    private Integer problemOrder;
}
