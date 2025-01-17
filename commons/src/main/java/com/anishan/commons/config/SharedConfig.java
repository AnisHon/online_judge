package com.anishan.commons.config;

import lombok.Data;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.ConfigurableEnvironment;

@Data
@ConfigurationProperties("oj")
public class SharedConfig {

    @Autowired
    private ConfigurableEnvironment environment;

    private boolean product = false;

}
