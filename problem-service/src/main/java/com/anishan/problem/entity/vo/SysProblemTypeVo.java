package com.anishan.problem.entity.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@ApiModel("题目类型信息")
public class SysProblemTypeVo {

    @ApiModelProperty("类型id")
    private Long typeId;

    @ApiModelProperty("类型名")
    private String typeName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

}