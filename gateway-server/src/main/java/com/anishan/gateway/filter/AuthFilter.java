package com.anishan.gateway.filter;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
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

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();

        // refresh 接口只接收请求体中的 refreshToken，不能把过期 access token 当成网关身份令牌解析。
        if (request.getURI().getPath().endsWith("/auth/refresh")) {
            return chain.filter(exchange);
        }

        // token
        String token = null;
        List<String> headers = request.getHeaders().get("token");

        if (!CollectionUtil.isEmpty(headers)) {
            token = headers.get(0);
        }

        ServerHttpRequest.Builder builder = request.mutate()
                .headers(h -> h.remove("user-id"));

        if (StrUtil.isEmpty(token)) {
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        Long userId = JwtUtil.parseAccessJwt(token);

        ServerWebExchange ex = exchange.mutate()
                .request(builder.header("user-id", userId.toString()).build())
                .build();
        // 6.放行
        return chain.filter(ex);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
