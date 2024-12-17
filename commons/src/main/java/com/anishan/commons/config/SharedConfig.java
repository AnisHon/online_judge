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

    private boolean product = false;

    public void setProduct(boolean product) {
        this.product = product;
        environment.getSystemProperties().put("knife4j.production", product);
        environment.getSystemProperties().put("knife4j.enabled", !product);
        environment.getSystemProperties().put("knife4j.gateway.enabled", !product);
    }
}
