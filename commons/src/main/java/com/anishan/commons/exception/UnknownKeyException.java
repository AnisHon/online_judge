package com.anishan.commons.exception;

public class UnknownKeyException extends RuntimeException {

    public UnknownKeyException(String key) {
        super("Unknown Key:" + key);
    }





}
