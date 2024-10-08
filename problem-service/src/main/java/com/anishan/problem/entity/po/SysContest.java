package com.anishan.problem.entity.po;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * 比赛表
 * @TableName sys_contest
 */
@TableName(value ="sys_contest")
@Data
public class SysContest implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long contestId;

    /**
     * 比赛创建者id
     */
    private Long userId;

    /**
     * 比赛标题
     */
    private String title;

    /**
     * 0为acm赛制，1为比分赛制
     */
    private Integer type;

    /**
     * 题单
     */
    private Long listId;

    /**
     * 比赛说明
     */
    private String description;

    /**
     * 0公开赛，1为私有赛（访问需要密码）
     */
    private Integer auth;

    /**
     * 比赛密码
     */
    private String pwd;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 删除标记
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 
     */
    private LocalDateTime createTime;

    /**
     * 
     */
    @Version
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}