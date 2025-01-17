package com.anishan.api.client.judgeserver.domain;


import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;


@Data
@ApiModel("用户判题信息类")
@Accessors(chain = true)
public class JudgeInfo {


    private Long userId;

    /**
     * 用户sse uuid
     */
    private String uuid;

    private Long problemId;

    private Long contestId;
    private Long languageId;

    /**
     * 代码
     */
    private String code;
    private String language;

    private Long timeLimit;
    private Long memoryLimit;
    private Integer stackLimit;


}
