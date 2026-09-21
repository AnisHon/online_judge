package com.anishan.api.client.problem.domain.vo;

import com.anishan.commons.enumeration.JudgeResult;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
public class SubmitLogVo {
    /**
     * 提交ID
     */
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitId;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 题目id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long contestId;

    /**
     * 使用语言的id
     */
    private String language;

    /** 提交源代码。接口层会先做归属校验。 */
    private String code;

    /**
     * 提交结果，取值范围 (AC, RE, WA, TLE, MLE)
     */
    private JudgeResult status;

    private String stderr;

    /**
     * 耗时 单位ms
     */
    private Long time;

    /**
     * 内存使用 单位kb
     */
    private Long memory;

    private Integer totalCount;

    private Integer passCount;

    /**
     *
     */
    private Date submitTime;
}
