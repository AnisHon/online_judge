package com.anishan.content.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
@ApiModel("创建评论回复")
public class CommentReplyRequest {
    @NotNull
    @ApiModelProperty(value = "纯文本回复内容", required = true)
    private String content;

    @NotNull
    @ApiModelProperty(value = "一次逻辑提交的UUID，重试时复用", required = true)
    private String clientRequestId;
}
