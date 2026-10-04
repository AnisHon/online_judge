package com.anishan.content.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
@ApiModel("创建一级评论")
public class CommentCreateRequest {
    @NotNull
    @ApiModelProperty(value = "纯文本评论，最多2000 Unicode code points、8KiB UTF-8", required = true)
    private String content;

    @NotNull
    @ApiModelProperty(value = "由客户端为一次逻辑提交生成并在重试时复用的UUID", required = true)
    private String clientRequestId;
}
