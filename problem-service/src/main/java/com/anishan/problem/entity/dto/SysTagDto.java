package com.anishan.problem.entity.dto;

import com.anishan.commons.e.ValidationGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@ApiModel("题目标签表")
public class SysTagDto {

    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("主键")
    private Long tagId;

    @ApiModelProperty("题目标签名称")
    private String tagName;

    @ApiModelProperty("颜色RGB值，带#")
    private String tagColor;


}