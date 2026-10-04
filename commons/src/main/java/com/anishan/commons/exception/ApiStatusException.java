package com.anishan.commons.exception;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Business error whose HTTP status must match the response body's R.code.
 * Only statuses used by the public API are accepted.
 */
public class ApiStatusException extends BusinessException {

    private static final Set<Integer> SUPPORTED_STATUS_CODES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(400, 401, 403, 404, 409, 429, 503)));

    private final int statusCode;

    public ApiStatusException(int statusCode, String safeMessage) {
        super(safeMessage);
        if (!SUPPORTED_STATUS_CODES.contains(statusCode)) {
            throw new IllegalArgumentException("Unsupported API status code");
        }
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
