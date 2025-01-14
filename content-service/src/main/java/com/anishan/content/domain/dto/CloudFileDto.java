package com.anishan.content.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
@Data
public class CloudFileDto {

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "父文件ID")
    private Long parentId;

    @ApiModelProperty(value = "存储文件ID")
    private Long fileId;

    @ApiModelProperty(value = "是否是文件夹")
    private Boolean dir;

}
