package com.anishan.problem.entity.dto;

import com.anishan.commons.e.ValidationGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@ApiModel("题目标签表")
public class SysTagDto {

    @NotNull(groups = ValidationGroup.Update.class)

    @ApiModelProperty("主键")
    private Long tagId;

    @Length(min = 1, max = 32, message = "不能为空，不能太长")
    @ApiModelProperty("题目标签名称")
    private String tagName;

    @Length(min = 1, max = 10, message = "不能为空不能太长")
    @ApiModelProperty("颜色RGB值，带#")
    private String tagColor;


}