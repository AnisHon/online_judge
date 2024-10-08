package com.anishan.problem.entity.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("题单表")
public class SysListDto {

    @ApiModelProperty("主键")
    private Long listId;

    @ApiModelProperty("题单名字，必须唯一")
    private String listName;

    @ApiModelProperty("题单说明，字数不应该太多")
    private String description;

    @ApiModelProperty("提交次数限制")
    private Integer changes;

    @ApiModelProperty("创建时间")
    private Date createTime;

}