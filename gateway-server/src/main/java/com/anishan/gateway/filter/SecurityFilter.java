package com.anishan.gateway.filter;

import cn.hutool.core.text.AntPathMatcher;
import com.anishan.commons.config.SharedConfig;
import com.anishan.gateway.config.InterfaceRuleConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SecurityFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher matcher = new AntPathMatcher();

    private final InterfaceRuleConfig interfaceRuleConfig;
    private final SharedConfig sharedConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        for (String blockRule : interfaceRuleConfig.getBlockRules()) {
            if (matcher.match(blockRule, path)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }

        if (sharedConfig.isProduct()) {
            for (String blockRule : interfaceRuleConfig.getProductEnvBlockRules()) {
                if (matcher.match(blockRule, path)) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
            }
        }


        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
