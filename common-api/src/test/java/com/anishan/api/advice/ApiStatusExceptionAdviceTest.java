package com.anishan.api.advice;

import com.anishan.commons.config.SharedConfig;
import com.anishan.commons.domain.R;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.commons.exception.IllegalTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiStatusExceptionAdviceTest {

    @Test
    void supportedStatusesMatchTheHttpStatusAndResponseCode() {
        GlobalExceptionAdvice advice = advice(true);
        List<Integer> allowedStatuses = Arrays.asList(400, 401, 403, 404, 409, 429, 503);

        for (Integer status : allowedStatuses) {
            ResponseEntity<R<String>> response = advice.handleApiStatusException(
                    new ApiStatusException(status, "安全提示"));
            assertEquals(status.intValue(), response.getStatusCodeValue());
            assertEquals(status.intValue(), response.getBody().getCode());
            assertEquals("安全提示", response.getBody().getMessage());
        }
    }

    @Test
    void unsupportedStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ApiStatusException(418, "message"));
    }

    @Test
    void productionResponseDoesNotExposeTechnicalBusinessMessages() {
        ResponseEntity<R<String>> response = advice(true).handleApiStatusException(
                new ApiStatusException(409, "select * from sys_user"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getCode());
        assertFalse(response.getBody().getMessage().contains("sys_user"));
    }

    @Test
    void accessDeniedAndInvalidTokenHaveMatchingHttpStatuses() {
        GlobalExceptionAdvice advice = advice(true);

        ResponseEntity<R<String>> forbidden = advice.handleAccessDeniedException(
                new AccessDeniedException("denied"));
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN.value(), forbidden.getBody().getCode());

        ResponseEntity<R<String>> unauthorized = advice.handleIllegalTokenException(
                new IllegalTokenException("invalid"));
        assertEquals(HttpStatus.UNAUTHORIZED, unauthorized.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), unauthorized.getBody().getCode());
    }

    private GlobalExceptionAdvice advice(boolean production) {
        SharedConfig config = new SharedConfig();
        config.setProduct(production);
        return new GlobalExceptionAdvice(config);
    }
}
