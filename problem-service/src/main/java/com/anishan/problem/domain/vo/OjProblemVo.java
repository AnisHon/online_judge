package com.anishan.problem.domain.vo;

import com.anishan.commons.enumeration.Difficulty;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OJ题目分表
 * @TableName oj_problem
 */
@TableName(value ="oj_problem")
@Data
@ApiModel("OJ题目")
public class OjProblemVo {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("主键")
    private Long problemId;

    @ApiModelProperty("时间限制单位ms")
    private Long timeLimit;

    @ApiModelProperty("难度 (0 未分类, 1 简单, 2 中等, 3 困难)")
    private Difficulty difficulty;

    @ApiModelProperty("内存限制单位kb")
    private Long memoryLimit;

    @ApiModelProperty("堆栈内存限制单位mb")
    private Integer stackLimit;

    @ApiModelProperty("输入描述")
    private String input;

    @ApiModelProperty("输出描述")
    private String output;

    @ApiModelProperty("输入样例")
    private String inputExample;

    @ApiModelProperty("输出样例")
    private String outputExample;

    @TableLogic
    @ApiModelProperty("删除标记(0未删除 1删除)")
    private Integer delFlag;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

}