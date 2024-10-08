package com.anishan.problem.entity.po;

import com.anishan.commons.e.FolderType;
import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import lombok.Data;

/**
 * 文件夹表
 * @TableName sys_folder
 */
@TableName(value ="sys_folder")
@Data
public class SysFolder implements Serializable {
    /**
     * 文件夹ID，不存在ID为0的wjj
     */
    @TableId(type = IdType.AUTO)
    private Long folderId;

    /**
     * 唯一文件夹名
     */
    private String folderName;

    /**
     * 类型(D directory 目录，F file 文件)
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
     * logic delete
     */
    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}