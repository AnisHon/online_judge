package com.anishan.api.client.judgeserver.domain;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel("用户测试运行信息类")
public class RunTestInfo {
    private Long userId;
    private String uuid;
    private String code;
    private String language;
    private String stdin;
}
