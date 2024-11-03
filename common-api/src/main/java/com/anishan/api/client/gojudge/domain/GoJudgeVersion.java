package com.anishan.api.client.gojudge.domain;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel("Go Judge的version实体类")
public class GoJudgeVersion {
    private boolean addressSpaceLimit;
    private String buildVersion;
    private boolean copyOutOptional;
    private String goVersion;
    private String os;
    private boolean pipeProxy;
    private String platform;
    private boolean stream;
    private boolean symlink;
}
