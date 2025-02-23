package com.anishan.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "common.config")
public class ConstConfig {

    private Long captchaCodeLifespan = 120L;
    private Long emailCodeLifespan = 120L;
    private Long checkInAward = 10L;
    private Long checkMaxAward = 40L;
    private float acProblemAwardRate = 0.5f;
    private Long defaultRoleId = 1L;
    // todo 无用待删除
    private String fileBaseUrl = "/opt/oj_files";

    private String casesFolder = "problem_case";

    private BigDecimal listDefaultScore = new BigDecimal("100");

}
