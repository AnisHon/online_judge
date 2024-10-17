package com.anishan.problem.domain.vo;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 题目标签表
 * @TableName tag
 */
@TableName(value ="tag")
@Data
public class TagVo {

    @ApiModelProperty("主键")
    private Long tagId;

    @ApiModelProperty("题目标签")
    private String tagName;

    @ApiModelProperty("颜色RGB值，带#")
    private String tagColor;

    @ApiModelProperty("创建时间")
    private Date createTime;



}