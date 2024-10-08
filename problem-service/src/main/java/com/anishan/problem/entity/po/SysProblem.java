package com.anishan.problem.entity.po;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 题目信息
 * @TableName sys_problem
 */
@TableName(value ="sys_problem")
@Data
public class SysProblem implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long problemId;

    /**
     * 题目名称
     */
    private String title;

    /**
     * 作者
     */
    private String author;

    /**
     * 单位ms
     */
    private Integer timeLimit;

    /**
     * 单位kb
     */
    private Integer memoryLimit;

    /**
     * 单位mb
     */
    private Integer stackLimit;

    /**
     * 题目描述
     */
    private String description;

    /**
     * 输入描述
     */
    private String input;

    /**
     * 输出描述
     */
    private String output;

    /**
     * 输入样例
     */
    private String inputExample;

    /**
     * 输出样例
     */
    private String outputExample;

    /**
     * 题目来源
     */
    private String source;

    /**
     * 备注,提醒
     */
    private String hint;

    /**
     * 默认为1公开，2为私有，3为比赛题目
     */
    private Integer auth;

    /**
     * 当该题目为OI题目时的分数
     */
    private Integer ioScore;

    /**
     * 题目评测模式,default、spj、interactive
     */
    private String judgeMode;

    /**
     * 题目样例评测模式,default,subtask_lowest,subtask_average
     */
    private String judgeCaseMode;

    /**
     * 题目评测时用户程序的额外文件 json key:name value:content
     */
    private String userExtraFile;

    /**
     * 题目评测时交互或特殊程序的额外文件 json key:name value:content
     */
    private String judgeExtraFile;

    /**
     * 特判程序或交互程序代码
     */
    private String spjCode;

    /**
     * 特判程序或交互程序代码的语言
     */
    private String spjLanguage;

    /**
     * 删除标记(0未删除 1删除)
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间，用于乐观锁
     */
    @Version
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}