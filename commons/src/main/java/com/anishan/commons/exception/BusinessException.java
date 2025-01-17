package com.anishan.commons.exception;

import com.anishan.commons.enumeration.BusinessError;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private BusinessError error;

    public BusinessException() {}
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(BusinessError error) {
        this.error = error;
    }
}
