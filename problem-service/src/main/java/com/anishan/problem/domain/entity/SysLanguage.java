package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 编程语言表
 * @TableName sys_language
 */
@TableName(value ="sys_language")
@Data
public class SysLanguage implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long languageId;

    /**
     * 语言名字
     */
    private String languageName;

    /**
     * 编译指令
     */
    private String compileCommand;

    /**
     * 语言排序
     */
    private Integer seq;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 
     */
    @Version
    private Date gmtModified;

    /**
     * 删除标记
     */
    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}