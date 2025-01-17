package com.anishan.problem.domain.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;

    @ApiModelProperty("标签id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tagId;


}