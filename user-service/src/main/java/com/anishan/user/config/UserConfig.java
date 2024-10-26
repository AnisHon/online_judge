package com.anishan.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oj.config")
public class UserConfig {

    private Long checkInAward;
    private Long checkMaxAward;
    private float acProblemAwardRate;
    private Long defaultRoleId;

    private String fileBaseUrl;

}
