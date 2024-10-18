package com.anishan.problem.domain.dto;

import com.anishan.commons.e.ValidationGroup;
import com.anishan.problem.domain.vo.ProblemVo;
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
    private Long listId;

    @NotNull(groups = {ValidationGroup.Insert.class, ValidationGroup.Delete.class, ValidationGroup.Update.class})
    @ApiModelProperty("题目ID")
    private Long problemId;

    @NotNull(groups = ValidationGroup.Insert.class)
    @ApiModelProperty(value = "题目顺序", allowEmptyValue = true)
    private Integer problemOrder;

    @NotNull(groups = ValidationGroup.Insert.class)
    @ApiModelProperty(value = "对应分数", allowEmptyValue = true)
    private BigDecimal score;

}