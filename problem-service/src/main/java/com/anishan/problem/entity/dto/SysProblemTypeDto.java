package com.anishan.problem.entity.dto;

import com.anishan.commons.e.ValidationGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;


@Data
@ApiModel("题目类型信息")
public class SysProblemTypeDto {

    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("类型id")
    private Long typeId;

    @ApiModelProperty("类型名")
    private String typeName;

}