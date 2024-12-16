package com.anishan.commons.exception;

public class BusinessException extends RuntimeException {
    public BusinessException() {}
    public BusinessException(String message) {
        super(message);
    }
}
