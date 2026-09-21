package com.anishan.problem.domain.vo;

import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.problem.domain.entity.JudgeCaseLog;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/** 管理侧测试用例判题日志视图，所有雪花 ID 以字符串返回。 */
@Data
@Accessors(chain = true)
public class AdminJudgeCaseLogVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long caseId;

    private Integer caseIndex;
    private JudgeResult status;
    private BigDecimal score;
    private Long time;
    private Long memory;
    private String internalError;
    private Date createTime;
    private Date updateTime;

    public static AdminJudgeCaseLogVo from(JudgeCaseLog source) {
        if (source == null) {
            return null;
        }
        return new AdminJudgeCaseLogVo()
                .setId(source.getId())
                .setSubmitId(source.getSubmitId())
                .setProblemId(source.getProblemId())
                .setCaseId(source.getCaseId())
                .setCaseIndex(source.getCaseIndex())
                .setStatus(source.getStatus())
                .setScore(source.getScore())
                .setTime(source.getTime())
                .setMemory(source.getMemory())
                .setInternalError(source.getInternalError())
                .setCreateTime(source.getCreateTime())
                .setUpdateTime(source.getUpdateTime());
    }
}
