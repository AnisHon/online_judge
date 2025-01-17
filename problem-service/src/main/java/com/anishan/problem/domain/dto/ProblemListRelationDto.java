package com.anishan.problem.domain.dto;

import com.anishan.commons.enumeration.ValidationGroup;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 题单 题目关系表
 * @TableName problem_problem_list
 */

@Data
@ApiModel("用于添加题目")
@AllArgsConstructor
@NoArgsConstructor
public class ProblemListRelationDto {

    @NotNull(groups = {ValidationGroup.Insert.class, ValidationGroup.Delete.class, ValidationGroup.Update.class})
    @ApiModelProperty("题单ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long listId;

    @NotNull(groups = {ValidationGroup.Insert.class, ValidationGroup.Delete.class, ValidationGroup.Update.class})
    @ApiModelProperty("题目ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;

    @NotNull(groups = ValidationGroup.Insert.class)
    @ApiModelProperty(value = "题目顺序", allowEmptyValue = true)
    private Integer problemOrder;

    @NotNull(groups = ValidationGroup.Insert.class)
    @ApiModelProperty(value = "对应分数", allowEmptyValue = true)
    private BigDecimal score;

}