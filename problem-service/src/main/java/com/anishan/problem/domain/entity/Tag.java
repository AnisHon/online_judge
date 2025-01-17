package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 题目标签表
 * @TableName tag
 */
@TableName(value ="tag")
@Data
public class Tag implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
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
    private LocalDateTime createTime;

    /**
     * 
     */
    @Version
    private LocalDateTime updateTime;

    /**
     * 删除标记
     */
    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}