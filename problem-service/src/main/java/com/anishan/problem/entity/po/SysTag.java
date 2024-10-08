package com.anishan.problem.entity.po;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 题目标签表
 * @TableName sys_tag
 */
@TableName(value ="sys_tag")
@Data
public class SysTag implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long tagId;

    /**
     * 题目标签
     */
    private String tagName;

    /**
     * 颜色RGB值，带#
     */
    private String tagColor;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    @Version
    private Date updateTime;

    /**
     * 删除标记
     */
    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}