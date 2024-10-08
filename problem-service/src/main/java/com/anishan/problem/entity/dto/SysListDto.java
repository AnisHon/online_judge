package com.anishan.problem.entity.dto;

import com.anishan.commons.e.ValidationGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@ApiModel("题单表")
public class SysListDto {

    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("主键")
    private Long listId;

    @ApiModelProperty("题单名字，必须唯一")
    private String listName;

    @ApiModelProperty("题单说明，字数不应该太多")
    private String description;

    @ApiModelProperty("提交次数限制")
    private Integer changes;

}