package com.anishan.problem.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("最终榜单分页参数")
public class FinalRankPageQuery {

    @NotNull(message = "当前页不能为空")
    @Min(value = 1, message = "当前页必须大于0")
    @ApiModelProperty(value = "当前页，从1开始", required = true)
    private Long currentPage;

    @NotNull(message = "分页大小不能为空")
    @Min(value = 1, message = "分页大小必须大于0")
    @Max(value = 50, message = "每页最多50条")
    @ApiModelProperty(value = "每页条数，最大50", required = true)
    private Long pageSize;
}
