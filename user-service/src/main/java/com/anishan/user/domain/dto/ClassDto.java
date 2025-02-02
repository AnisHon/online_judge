package com.anishan.user.domain.dto;

import com.anishan.commons.enumeration.ValidationGroup;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("班级参数类")
public class ClassDto {

    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("班级Id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long classId;

    @NotEmpty(groups = ValidationGroup.Insert.class)
    @ApiModelProperty("班级名")
    private String className;

    @ApiModelProperty("备注")
    private String remark;

}
