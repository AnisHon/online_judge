package com.anishan.problem.entity.dto;

import com.anishan.commons.e.ValidationGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@ApiModel("编程语言")
public class SysLanguageDto {

    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("主键")
    private Long languageId;

    @ApiModelProperty("语言名字")
    private String languageName;

    @ApiModelProperty("编译指令")
    private String compileCommand;

    @ApiModelProperty("语言排序")
    private Integer seq;


}