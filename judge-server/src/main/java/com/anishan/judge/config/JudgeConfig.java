package com.anishan.judge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties("judge.config")
public class JudgeConfig {

    /**
     * 判题间隔，单位s
     */
    private Integer judgeInterval = 10;
}
