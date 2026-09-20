package com.anishan.judge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

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

    /** 用户自由输入运行的 CPU 时间上限。与正式题目判题限制隔离。 */
    private Duration testCpuTime = Duration.ofSeconds(2);

    /** 用户自由输入运行的墙钟时间上限。 */
    private Duration testWallTime = Duration.ofSeconds(3);

    /** 用户自由输入运行的内存上限。 */
    private DataSize testMemory = DataSize.ofMegabytes(256);

    /** 用户自由输入运行的标准输出上限。 */
    private DataSize testOutput = DataSize.ofMegabytes(2);

    /** 用户自由输入运行允许创建的进程数。 */
    private Integer testPids = 4;

    /**
     * Minio本地挂载路径
     */
    private String mountPath = "/mnt/minio";

}
