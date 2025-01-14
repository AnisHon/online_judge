package com.anishan.problem.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@ApiModel("题解内容")
@Accessors(chain = true)
public class SolutionVo {
    @ApiModelProperty(value = "主键")
    private Long solutionId;

    @ApiModelProperty(value = "题解标题")
    private String title;

    @ApiModelProperty(value = "对应题目")
    private Long problemId;

    @ApiModelProperty(value = "题目标题")
    private String problemTitle;

    @ApiModelProperty(value = "发送者ID")
    private Long userId;

    @ApiModelProperty(value = "发送者昵称")
    private String nikeName;

    @ApiModelProperty(value = "是否置顶")
    private Boolean topUp;

    @ApiModelProperty(value = "是否尽自己可见")
    private Boolean private_;

    @ApiModelProperty(value = "内容")
    private String content;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
