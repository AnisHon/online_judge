package com.anishan.user.entity.dto;

import com.anishan.user.e.ValidationGroup;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@ApiModel("班级参数类")
public class ClassDto {

    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("班级Id")
    private Long classId;

    @ApiModelProperty("班级名")
    private String className;

    @ApiModelProperty("备注")
    private String remark;

}
