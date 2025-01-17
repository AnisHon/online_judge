package com.anishan.content.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("云文件类")
public class CloudFilesVo {

    @ApiModelProperty("云文件ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long cloudFileId;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("file info文件ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    @ApiModelProperty("文件MD5")
    private String fileMd5;

    @ApiModelProperty("文件OSS路径")
    private String filePath;

    @ApiModelProperty("文件大小")
    private Long fileSize;

    @ApiModelProperty("上传用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @ApiModelProperty("上传用户名")
    private String nikeName;

    @ApiModelProperty("父文件ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    @ApiModelProperty("是否是文件夹")
    private boolean dir;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

}
