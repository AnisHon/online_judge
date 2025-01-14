package com.anishan.problem.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("题解内容")
@Accessors(chain = true)
public class DetailSolutionDto {
    @ApiModelProperty(value = "主键")
    private Long solutionId;

    @NotNull(message = "标题不能为空")
    @NotEmpty(message = "标题不能为空")
    @Length(min = 1, max = 50, message = "标题只能在1到50之间")
    @ApiModelProperty(value = "题解标题")
    private String title;

    @NotNull
    @ApiModelProperty(value = "对应题目")
    private Long problemId;

    @ApiModelProperty(value = "是否置顶, 对于普通用户没用")
    private Boolean topUp;

    @ApiModelProperty(value = "是否自己可见")
    private Boolean private_;

    @NotNull
    @NotEmpty(message = "题解内容不能为空")
    @ApiModelProperty(value = "内容")
    private String content;
}
