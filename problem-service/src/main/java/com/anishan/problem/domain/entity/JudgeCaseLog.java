package com.anishan.problem.domain.entity;

import com.anishan.commons.enumeration.JudgeResult;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 判题机内部测试用例日志。该表不作为用户提交记录返回。
 */
@Data
@Accessors(chain = true)
@TableName("judge_case_log")
public class JudgeCaseLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long submitId;
    private Long problemId;
    private Long caseId;
    private Integer caseIndex;

    @EnumValue
    private JudgeResult status;

    private BigDecimal score;
    private Long time;
    private Long memory;

    /** 判题机内部异常详情，不对普通用户输出。 */
    private String internalError;

    private Date createTime;
    private Date updateTime;
}
