package com.anishan.gateway;

import com.anishan.commons.config.SharedConfig;
import com.anishan.gateway.config.InterfaceRuleConfig;
import com.anishan.gateway.filter.SecurityFilter;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class SecurityFilterInternalPathTest {

    @Test
    void internalPathsAreBlockedAcrossServicePrefixesAndNestedVariants() {
        SecurityFilter filter = newFilter();
        for (String path : Arrays.asList(
                "/internal/content-problems/read",
                "/problem-api/internal/content-problems/read",
                "/content-api/internal/anything",
                "/user-api/v1/internal/user-summaries",
                "/api/problem-api/internal/content-problems/read")) {
            AtomicBoolean downstreamReached = new AtomicBoolean(false);
            ServerWebExchange exchange = exchange(path);
            GatewayFilterChain chain = ignored -> {
                downstreamReached.set(true);
                return Mono.empty();
            };

            filter.filter(exchange, chain).block();

            assertFalse(downstreamReached.get(), "Gateway forwarded blocked path: " + path);
            verify(exchange.getResponse()).setStatusCode(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void similarlyNamedNonInternalPathsRemainReachable() {
        SecurityFilter filter = newFilter();
        AtomicBoolean downstreamReached = new AtomicBoolean(false);
        ServerWebExchange exchange = exchange("/problem-api/problem/list");
        GatewayFilterChain chain = ignored -> {
            downstreamReached.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertTrue(downstreamReached.get());
        verify(exchange, never()).getResponse();
    }

    @Test
    void legacyJudgeResultWriteRoutesAreBlocked() {
        SecurityFilter filter = newFilter();
        for (String path : Arrays.asList(
                "/problem-api/record/judge-save",
                "/api/problem-api/record/judge-save",
                "/problem-api/log/queue",
                "/problem-api/log/log-judge",
                "/problem-api/log/update",
                "/problem-api/log/change-status")) {
            AtomicBoolean downstreamReached = new AtomicBoolean(false);
            ServerWebExchange exchange = exchange(path);
            GatewayFilterChain chain = ignored -> {
                downstreamReached.set(true);
                return Mono.empty();
            };

            filter.filter(exchange, chain).block();

            assertFalse(downstreamReached.get(), "Gateway forwarded legacy judge write route: " + path);
            verify(exchange.getResponse()).setStatusCode(HttpStatus.FORBIDDEN);
        }
    }

    private SecurityFilter newFilter() {
        InterfaceRuleConfig rules = new InterfaceRuleConfig();
        rules.setBlockRules(Arrays.asList("/**/internal/**", "/**/record/judge-save",
                "/**/log/queue", "/**/log/log-judge", "/**/log/update", "/**/log/change-status"));
        SharedConfig sharedConfig = new SharedConfig();
        sharedConfig.setProduct(false);
        return new SecurityFilter(rules, sharedConfig);
    }

    private ServerWebExchange exchange(String path) {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        when(request.getURI()).thenReturn(URI.create("http://localhost" + path));
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(response.setComplete()).thenReturn(Mono.empty());
        return exchange;
    }
}
