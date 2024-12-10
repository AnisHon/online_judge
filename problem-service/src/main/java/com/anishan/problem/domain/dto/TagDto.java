package com.anishan.problem.domain.dto;

import com.anishan.commons.enumeration.ValidationGroup;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 题目标签表
 * @TableName tag
 */
@TableName(value ="tag")
@Data
public class TagDto {

    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("主键")
    private Long tagId;

    @ApiModelProperty("题目标签")
    private String tagName;

    @Pattern(regexp = "^#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})$",
            message = "必须是有效的RGB值",
            groups = {
                ValidationGroup.Update.class,
                ValidationGroup.Insert.class
            }
    )
    @ApiModelProperty("颜色RGB值，带#")
    private String tagColor;

}