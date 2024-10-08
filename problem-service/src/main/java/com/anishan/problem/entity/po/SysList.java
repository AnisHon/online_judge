package com.anishan.problem.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
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
     * 提交次数限制
     */
    private Integer changes;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    /**
     * 删除标记
     */
    private Integer delFlag;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}