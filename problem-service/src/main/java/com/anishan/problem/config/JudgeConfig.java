package com.anishan.problem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("judge.config")
public class JudgeConfig {

    private Integer judgeInterval = 10;

}
