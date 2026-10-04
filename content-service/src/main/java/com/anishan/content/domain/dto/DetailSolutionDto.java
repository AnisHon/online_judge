package com.anishan.content.domain.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
@ApiModel("题解内容")
public class DetailSolutionDto {
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("题解ID")
    private Long solutionId;

    @NotNull(message = "标题不能为空")
    @NotEmpty(message = "标题不能为空")
    @Length(min = 1, max = 50, message = "标题只能在1到50之间")
    private String title;

    @NotNull(message = "请选择题目")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;

    private Boolean topUp;
    private Boolean private_;

    @NotNull(message = "题解内容不能为空")
    @NotEmpty(message = "题解内容不能为空")
    private String content;
}
