package com.anishan.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("关注关系列表分页参数")
public class FollowPageQuery {

    @NotNull
    @Min(value = 1, message = "页码必须大于0")
    @ApiModelProperty(value = "当前页", example = "1")
    private Long currentPage = 1L;

    @NotNull
    @Min(value = 1, message = "每页数量必须为1到50")
    @Max(value = 50, message = "每页数量必须为1到50")
    @ApiModelProperty(value = "每页数量，最大50", example = "20")
    private Long pageSize = 20L;
}
