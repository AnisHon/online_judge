package com.anishan.content.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
@ApiModel("题解评论区状态")
public class CommentsStateRequest {
    @NotNull
    @ApiModelProperty(value = "是否允许新增评论与回复", required = true)
    private Boolean open;
}
