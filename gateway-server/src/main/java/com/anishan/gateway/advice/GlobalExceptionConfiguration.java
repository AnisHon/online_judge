package com.anishan.gateway.advice;

import com.anishan.commons.domain.R;
import com.anishan.commons.exception.IllegalTokenException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Order(-1)
@RequiredArgsConstructor
@Component
@Slf4j
public class GlobalExceptionConfiguration implements ErrorWebExceptionHandler {
    private final ObjectMapper objectMapper;

    @NotNull
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, @NotNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        log.error("网关请求处理失败，path={}, status={}",
                exchange.getRequest().getPath().value(), status.value(), ex);

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.setStatusCode(status);

        return response
                .writeWith(Mono.fromSupplier(() -> {
                    DataBufferFactory bufferFactory = response.bufferFactory();
                    try {
                        return bufferFactory.wrap(objectMapper.writeValueAsBytes(
                                R.error(status.value(), clientMessage(status))));
                    } catch (JsonProcessingException e) {
                        return bufferFactory.wrap(new byte[0]);
                    }
                }));
    }

    private HttpStatus resolveStatus(Throwable exception) {
        if (exception instanceof ResponseStatusException) {
            return ((ResponseStatusException) exception).getStatus();
        }
        if (exception instanceof IllegalTokenException) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (exception instanceof org.springframework.security.access.AccessDeniedException) {
            return HttpStatus.FORBIDDEN;
        }
        if (exception instanceof org.springframework.web.server.ServerWebInputException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String clientMessage(HttpStatus status) {
        switch (status) {
            case BAD_REQUEST:
                return "请求参数有误，请检查后重试";
            case UNAUTHORIZED:
                return "登录状态无效或已过期";
            case FORBIDDEN:
                return "无权限访问该资源";
            case NOT_FOUND:
                return "请求地址不存在";
            case METHOD_NOT_ALLOWED:
                return "不支持的请求方式";
            default:
                return "服务暂时无法处理请求，请稍后重试";
        }
    }
}
