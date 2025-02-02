package com.anishan.problem.domain.dto;

import com.anishan.commons.enumeration.ValidationGroup;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 题单表
 * @TableName problem_list
 */

@Data
@ApiModel("题单")
public class ProblemListDto {

    @ApiModelProperty("主键")
    @NotNull(groups = ValidationGroup.Update.class)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long listId;

    @ApiModelProperty("题单名字，必须唯一")
    @NotEmpty(groups = ValidationGroup.Insert.class)
    private String listName;

    @ApiModelProperty("题单说明，字数不应该太多")
    private String description;

}