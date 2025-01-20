package com.anishan.content.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("分片")
public class SpliceChunk {

    @NotNull
    @ApiModelProperty("索引 0 开始")
    private Integer index;

    @NotNull
    @ApiModelProperty("分片大小")
    private Long chunkSize;

    @NotNull
    @ApiModelProperty("总大小")
    private Long totalSize;

    @NotNull
    @ApiModelProperty("文件MD5")
    private String md5;

    @NotEmpty
    @ApiModelProperty("文件名")
    private String fileName;

    @NotNull
    @ApiModelProperty("父文件夹Id")
    private Long parentId;

}
