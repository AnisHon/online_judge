package com.anishan.api.advice;

import cn.hutool.http.HttpStatus;
import com.anishan.commons.entity.R;
import com.anishan.api.exception.IllegalTokenException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionAdvice {

    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public R<String> handleException(Exception e) {
        Throwable temp = e;
        StringBuilder builder = new StringBuilder();

        while (temp != null) {
            builder.append(temp.getMessage()).append("\n");
            temp = temp.getCause();
        }

        e.printStackTrace();

        return R.error(HttpStatus.HTTP_INTERNAL_ERROR, builder.toString());
    }

    @ResponseBody
    @ExceptionHandler(IllegalTokenException.class)
    public R<String> handleIllegalTokenException(IllegalTokenException e) {
        return R.error(HttpStatus.HTTP_UNAUTHORIZED, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<String> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return R.error(HttpStatus.HTTP_BAD_REQUEST, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<String> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return R.error(HttpStatus.HTTP_BAD_METHOD, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(NoHandlerFoundException.class)
    public R<String> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return R.error(HttpStatus.HTTP_NOT_FOUND, e.getMessage());
    }


}
