package com.anishan.gateway.filter;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.anishan.commons.config.SharedConfig;
import com.anishan.commons.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Resource
    private SharedConfig sharedConfig;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        if (!sharedConfig.isProduct()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();

        // token
        String token = null;
        List<String> headers = request.getHeaders().get("token");

        if (!CollectionUtil.isEmpty(headers)) {
            token = headers.get(0);
        }

        if (StrUtil.isEmpty(token)) {
            return chain.filter(exchange);
        }

        Long userId = JwtUtil.parseJwt(token);


        ServerWebExchange ex = exchange.mutate()
                .request(builder -> builder.header("user-id", userId.toString()))
                .build();
        // 6.放行
        return chain.filter(ex);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}