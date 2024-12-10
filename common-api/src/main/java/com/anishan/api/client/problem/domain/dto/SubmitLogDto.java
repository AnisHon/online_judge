package com.anishan.api.client.problem.domain.dto;

import com.anishan.commons.enumeration.JudgeResult;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class SubmitLogDto {
    /**
     * 提交ID
     */
    @TableId(type = IdType.AUTO)
    private Long submitId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 题目id
     */
    private Long problemId;

    /**
     * 使用语言的id
     */
    private String language;

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

    /**
     *
     */
    private Date submitTime;
}
