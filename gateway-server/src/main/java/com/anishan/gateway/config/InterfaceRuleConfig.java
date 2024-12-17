package com.anishan.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "oj.rule")
public class InterfaceRuleConfig {

    private List<String> blockRules = new ArrayList<>();

    private List<String> productEnvBlockRules = new ArrayList<>();

}
