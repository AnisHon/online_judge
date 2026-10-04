package com.anishan.content.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oj.content.solution")
public class SolutionDomainProperties {
    /** Enabled explicitly only after solution-domain-v1 reaches CUTOVER. */
    private boolean referenceRefreshEnabled = false;
}
