package com.anishan.problem.domain.entity;

import com.anishan.commons.enumeration.ContestAuth;
import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 比赛表
 * @TableName contest
 */
@TableName(value ="contest")
@Data
public class Contest implements Serializable {
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
     * 题单id
     */
    private Long listId;

    /**
     * 比赛说明
     */
    private String description;

    /**
     * 0 公开赛，1为私有赛（访问需要密码）2为白名单模式
     */
    private ContestAuth auth;

    /**
     * 比赛密码
     */
    private String pwd;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 删除标记
     */
    @TableLogic
    private Integer delFlag;

    private LocalDateTime createTime;

    /**
     * 
     */
    @Version
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}