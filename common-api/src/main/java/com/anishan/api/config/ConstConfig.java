package com.anishan.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "common.config")
public class ConstConfig {

    private Long captchaCodeLifespan;
    private Long emailCodeLifespan;
    private Long checkInAward;
    private Long checkMaxAward;
    private float acProblemAwardRate;
    private Long defaultRoleId;
    // todo 上线后或者用windows的时候这里需要更改
    private String fileBaseUrl = "/Users/anishan/Project/Web/temp";


}
