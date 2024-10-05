package com.anishan.api.exception;


import org.springframework.security.core.AuthenticationException;

public class IllegalTokenException extends AuthenticationException {

    public IllegalTokenException(String message) {
        super(message);
    }

}
