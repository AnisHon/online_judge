package com.anishan.problem.domain.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 标签 题目关系表
 * @TableName problem_tag
 */
@TableName(value ="problem_tag")
@Data
@ApiModel("题目与Tag的关联类")
public class ProblemTagDto implements Serializable {

    @ApiModelProperty("题目id")
    private Long problemId;

    @ApiModelProperty("标签id")
    private Long tagId;


}