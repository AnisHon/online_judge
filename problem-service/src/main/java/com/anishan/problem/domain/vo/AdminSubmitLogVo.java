package com.anishan.problem.domain.vo;

import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.problem.domain.entity.SubmitLog;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 管理侧提交记录视图。
 *
 * <p>雪花 ID 必须序列化为字符串，避免浏览器 Number 精度丢失后再次查询不到记录。</p>
 */
@Data
@Accessors(chain = true)
public class AdminSubmitLogVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long contestId;

    private String language;
    private String code;
    private JudgeResult status;
    private Long time;
    private Long memory;
    private String stderr;
    private String internalError;
    private String errorCode;
    private Integer totalCount;
    private Integer passCount;
    private Date submitTime;

    public static AdminSubmitLogVo from(SubmitLog source) {
        if (source == null) {
            return null;
        }
        return new AdminSubmitLogVo()
                .setSubmitId(source.getSubmitId())
                .setUserId(source.getUserId())
                .setProblemId(source.getProblemId())
                .setContestId(source.getContestId())
                .setLanguage(source.getLanguage())
                .setCode(source.getCode())
                .setStatus(source.getStatus())
                .setTime(source.getTime())
                .setMemory(source.getMemory())
                .setStderr(source.getStderr())
                .setInternalError(source.getInternalError())
                .setErrorCode(source.getErrorCode())
                .setTotalCount(source.getTotalCount())
                .setPassCount(source.getPassCount())
                .setSubmitTime(source.getSubmitTime());
    }
}
