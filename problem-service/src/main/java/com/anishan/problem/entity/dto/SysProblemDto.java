package com.anishan.problem.entity.dto;

import com.anishan.commons.e.ContestAuth;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel("题目信息")
public class SysProblemDto {

    @ApiModelProperty("主键")
    private Long problemId;

    @ApiModelProperty("题目名称")
    private String title;

    @ApiModelProperty("作者")
    private String author;

    @ApiModelProperty("类型ID，如果不是0(0是OJ题目)，就是其他题目")
    private Long typeId;

    @ApiModelProperty("单位ms")
    private Integer timeLimit;

    @ApiModelProperty("单位kb")
    private Integer memoryLimit;

    @ApiModelProperty("单位mb")
    private Integer stackLimit;

    @ApiModelProperty("题目描述")
    private String description;

    @ApiModelProperty("输入描述")
    private String input;

    @ApiModelProperty("输出描述")
    private String output;

    @ApiModelProperty("输入样例")
    private String inputExample;

    @ApiModelProperty("输出样例")
    private String outputExample;

    @ApiModelProperty("题目来源")
    private String source;

    @ApiModelProperty("备注,提醒")
    private String hint;

    @ApiModelProperty("默认为1公开，2为比赛题目")
    private ContestAuth auth;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}