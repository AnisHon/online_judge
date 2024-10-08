package com.anishan.problem.entity.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("题目标签表")
public class SysTagDto {

    @ApiModelProperty("主键")
    private Long tagId;

    @ApiModelProperty("题目标签名称")
    private String tagName;

    @ApiModelProperty("颜色RGB值，带#")
    private String tagColor;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

}