package com.anishan.problem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties("judge.config")
public class JudgeConfig {

    /**
     * 是否使用固定奖励分
     */
    private Boolean isFixedAwardPoint = true;

    /**
     * 使用固定奖励分时的奖励分具体数值，默认2
     */
    private BigDecimal awardPoint = new BigDecimal(2);

    public void setAwardPoint(String awardPoint) {
        this.awardPoint = new BigDecimal(awardPoint);
    }
}
