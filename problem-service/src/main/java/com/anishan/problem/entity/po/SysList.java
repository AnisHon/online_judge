package com.anishan.problem.entity.po;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 题单表
 * @TableName sys_list
 */
@TableName(value ="sys_list")
@Data
public class SysList implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long listId;

    /**
     * 题单名字，必须唯一
     */
    private String listName;

    /**
     * 题单说明，字数不应该太多
     */
    private String description;

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