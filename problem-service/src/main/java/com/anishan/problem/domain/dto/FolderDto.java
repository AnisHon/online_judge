package com.anishan.problem.domain.dto;

import com.anishan.commons.enumeration.FolderType;
import com.anishan.commons.enumeration.ValidationGroup;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 文件夹表
 * @TableName folder
 */
@TableName(value ="folder")
@Data
@ApiModel("用于树状图")
public class FolderDto implements Serializable {

    @ApiModelProperty("文件夹ID，不存在ID为0的文件夹")
    @NotNull(groups = ValidationGroup.Update.class)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long folderId;

    @ApiModelProperty("唯一文件夹名")
    @Length(min = 1, max = 32, message = "最长32，不可为空", groups = ValidationGroup.Insert.class)
    private String folderName;

    @ApiModelProperty("类型(D directory 目录，F file 文件, M 菜单栏)")
    private FolderType folderType;

    @ApiModelProperty("顺序")
    private Integer order;

    @NotNull(groups = ValidationGroup.Insert.class, message = "不可以为空")
    @ApiModelProperty("父文件夹名，默认0表示没有父文件夹")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    @ApiModelProperty("题单，如果是D类型则应该为空")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long listId;

}