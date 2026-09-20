package com.anishan.api.client.judgeserver.domain;


import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;


@Data
@ApiModel("用户判题信息类")
@Accessors(chain = true)
public class JudgeInfo {

    /**
     * problem-service 创建的提交记录 ID。判题机不再自行创建公开提交记录。
     */
    private Long submitId;

    /** problem-service 用于释放用户判题锁的内部令牌，不向用户公开。 */
    private String submissionLockToken;

    private Long userId;

    private Long problemId;

    private Long contestId;
    private Long languageId;

    /**
     * 题单设置的score
     */
    private BigDecimal listScore;

    /**
     * 代码
     */
    private String code;
    private String language;

    private Long timeLimit;
    private Long memoryLimit;
    private Integer stackLimit;


}
