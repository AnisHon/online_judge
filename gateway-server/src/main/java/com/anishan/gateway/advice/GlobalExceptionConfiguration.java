package com.anishan.gateway.advice;

import com.anishan.commons.domain.R;
import com.anishan.commons.exception.IllegalTokenException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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

import java.util.Objects;


@Order(-1)
@RequiredArgsConstructor
@Component
public class GlobalExceptionConfiguration implements ErrorWebExceptionHandler {
    private final ObjectMapper objectMapper;

    @NotNull
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, @NotNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // header set
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (ex instanceof ResponseStatusException) {
            response.setStatusCode(((ResponseStatusException) ex).getStatus());
        }

        if (ex instanceof RuntimeException) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
        }


        if (ex instanceof IllegalTokenException) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
        }


        return response
                .writeWith(Mono.fromSupplier(() -> {
                    DataBufferFactory bufferFactory = response.bufferFactory();
                    try {
                        return bufferFactory.wrap(objectMapper.writeValueAsBytes(R.error(Objects.requireNonNull(response.getStatusCode()).value(), ex.getMessage())));
                    } catch (JsonProcessingException e) {
                        return bufferFactory.wrap(new byte[0]);
                    }
                }));
    }
}