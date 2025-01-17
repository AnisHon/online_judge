package com.anishan.problem.domain.entity;

import com.anishan.commons.enumeration.FolderType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 文件夹表
 * @TableName folder
 */
@TableName(value ="folder")
@Data
public class Folder implements Serializable {
    /**
     * 文件夹ID，不存在ID为0的文件夹
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long folderId;

    /**
     * 唯一文件夹名
     */
    private String folderName;

    /**
     * 类型(D directory 目录，F file 文件, M 菜单栏)
     */
    private FolderType folderType;

    /**
     * 父文件夹名，默认0表示没有父文件夹
     */
    private Long parentId;

    /**
     * 题单，如果是D类型则应该为空
     */
    private Long listId;

    /**
     * 逻辑删除
     */
    private Integer delFlag;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}