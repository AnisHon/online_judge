package com.anishan.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oj.minio")
public class MinioConfigProperties {
    private String url = "http://localhost:9000";

    private String accessKey;

    private String secretKey;

    private String bucketName;
}
