package com.anishan.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.anishan.commons.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();

        ServerHttpRequest.Builder builder = request.mutate()
                .headers(h -> h.remove("user-id"));

        String path = request.getURI().getPath();
        // Refresh/logout are authenticated by the HttpOnly refresh cookie. Do not try
        // to parse an expired access token before the request reaches user-service.
        if (path.endsWith("/auth/refresh") || path.endsWith("/auth/logout")) {
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        // New clients use the standard Authorization header. Keep the legacy custom
        // header as a temporary compatibility path for non-browser clients.
        String token = extractAccessToken(request);

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

    private String extractAccessToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst("Authorization");
        if (StrUtil.isNotBlank(authorization) && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        return request.getHeaders().getFirst("token");
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
