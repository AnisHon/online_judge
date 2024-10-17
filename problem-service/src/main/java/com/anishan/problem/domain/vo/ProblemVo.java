package com.anishan.problem.domain.vo;

import com.anishan.commons.e.ProblemAuth;
import com.anishan.commons.e.ProblemType;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题目主表，OJ题目有分表，非OJ不需要继续分表
 * @TableName problem
 */
@TableName(value ="problem")
@Data
public class ProblemVo {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("主键")
    private Long problemId;

    @ApiModelProperty("题目名称")
    private String title;

    @ApiModelProperty("题目类型，(1 OJ, 2 OTHER)")
    private ProblemType type;

    @ApiModelProperty("题目来源")
    private String source;

    @ApiModelProperty("题目描述")
    private String description;

    @ApiModelProperty("备注,提醒")
    private String hint;

    @EnumValue
    @ApiModelProperty("默认为1公开，2为比赛题目")
    private ProblemAuth auth;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

}