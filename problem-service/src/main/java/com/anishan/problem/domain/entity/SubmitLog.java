package com.anishan.problem.domain.entity;

import com.anishan.commons.enumeration.JudgeResult;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * OJ判题提交记录
 * @TableName submit_log
 */
@TableName(value ="submit_log")
@Data
@Accessors(chain = true)
public class SubmitLog implements Serializable {
    /**
     * 提交ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long submitId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 题目id
     */
    private Long problemId;

    private Long contestId;

    /**
     * 使用语言的id
     */
    private String language;

    /** 用户提交的源代码。公开详情接口只允许本人读取。 */
    private String code;

    /**
     * 提交结果，取值范围 (AC, RE, WA, TLE, MLE)
     */
    @EnumValue
    private JudgeResult status;

    /**
     * 耗时 单位ms
     */
    private Long time;

    /**
     * 内存使用 单位kb
     */
    private Long memory;

    private String stderr;

    /** 仅供管理侧排查，不能复制到 SubmitLogVo。 */
    private String internalError;

    private String errorCode;

    private Integer totalCount;

    private Integer passCount;

    /**
     * 
     */
    private Date submitTime;


}
