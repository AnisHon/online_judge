package com.anishan.problem.entity.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("编程语言")
public class SysLanguageDto {

    @ApiModelProperty("主键")
    private Long languageId;

    @ApiModelProperty("语言名字")
    private String languageName;

    @ApiModelProperty("编译指令")
    private String compileCommand;

    @ApiModelProperty("语言排序")
    private Integer seq;

    @ApiModelProperty("创建时间")
    private LocalDateTime gmtCreate;

}