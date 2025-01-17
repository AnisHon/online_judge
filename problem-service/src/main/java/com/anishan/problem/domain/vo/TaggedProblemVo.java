package com.anishan.problem.domain.vo;

import com.anishan.commons.enumeration.ProblemAuth;
import com.anishan.commons.enumeration.ProblemType;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaggedProblemVo {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("主键")
    @JsonSerialize(using = ToStringSerializer.class)
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


    private List<TagVo> tags;

}
