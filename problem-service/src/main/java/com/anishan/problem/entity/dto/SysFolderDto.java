package com.anishan.problem.entity.dto;

import com.anishan.commons.e.FolderType;
import com.anishan.commons.e.ValidationGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 *
 * @TableName sys_folder
 */
@Data
@ApiModel("文件夹，文件")
public class SysFolderDto {

    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("文件夹ID，不存在ID为0的wjj")
    private Long folderId;

    @ApiModelProperty("唯一文件夹名")
    private String folderName;

    @ApiModelProperty("类型(D directory 目录，F file 文件)")
    private FolderType folderType;

    @ApiModelProperty("父文件夹名，默认0表示没有父文件夹")
    private Long parentId;

    @ApiModelProperty("题单，如果是D类型则应该为空")
    private Long listId;

}