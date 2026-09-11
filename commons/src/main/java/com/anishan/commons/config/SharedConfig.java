package com.anishan.commons.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.ConfigurableEnvironment;

@Data
@ConfigurationProperties("oj")
public class SharedConfig {

    @Autowired
    private ConfigurableEnvironment environment;

    // Fail closed if the configuration centre is unavailable or incomplete.
    private boolean product = true;

}
