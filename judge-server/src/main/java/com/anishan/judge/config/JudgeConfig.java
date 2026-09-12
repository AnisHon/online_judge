package com.anishan.judge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties("judge.config")
public class JudgeConfig {

    /** 单台判题机同时运行的测试用例数。 */
    private Integer maxConcurrency = 4;

    /** 单台判题机同时处理的提交数。 */
    private Integer maxSubmissions = 1;

    private Integer queueCapacity = 200;

    /**
     * Minio本地挂载路径
     */
    private String mountPath = "/mnt/minio";

}
