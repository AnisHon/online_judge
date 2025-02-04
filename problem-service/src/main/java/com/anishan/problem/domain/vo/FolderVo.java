package com.anishan.problem.domain.vo;

import com.anishan.commons.enumeration.FolderType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件夹表
 * @TableName folder
 */
@TableName(value ="folder")
@Data
@ApiModel("用于树状图")
public class FolderVo implements Serializable {

    @ApiModelProperty("文件夹ID，不存在ID为0的文件夹")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long folderId;

    @ApiModelProperty("唯一文件夹名")
    private String folderName;

    @ApiModelProperty("类型(D directory 目录，F file 文件, M 菜单栏)")
    private FolderType folderType;

    private Integer order;

    @ApiModelProperty("父文件夹名，默认0表示没有父文件夹")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    @ApiModelProperty("题单，如果是D类型则应该为空")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long listId;

}