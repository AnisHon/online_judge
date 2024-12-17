package com.anishan.api.client.judgeserver.domain;


import io.swagger.annotations.ApiModel;
import lombok.Data;


@Data
@ApiModel("用户判题信息类")
public class JudgeInfo {

    private Long userId;
    private String uuid;
    private Long problemId;
    private Long contestId;
    private Long languageId;
    private String code;
    private String language;
    private Long timeLimit;
    private Long memoryLimit;
    private Integer stackLimit;


}
