package com.anishan.problem.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户提交记录
 * @TableName submit_record
 */
@TableName(value ="submit_record")
@Data
public class SubmitRecord implements Serializable {
    /**
     * 提交ID
     */
    @TableId(type = IdType.AUTO)
    private Long submitId;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 题目id
     */
    private Long problemId;

    /**
     * 使用语言的id
     */
    private Long languageId;

    /**
     * 结果，取值范围 (AC, RE, WA, TLE, MLE)
     */
    private String result;

    /**
     * 耗时 单位ms
     */
    private Integer time;

    /**
     * 内存使用 单位kb
     */
    private Integer memory;

    /**
     * 
     */
    private LocalDateTime submitTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}