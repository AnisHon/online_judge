package com.anishan.api.advice;

import cn.hutool.http.HttpStatus;
import com.anishan.commons.domain.R;
import com.anishan.api.exception.IllegalTokenException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import java.util.Objects;

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

        // todo
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

    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String defaultMessage = Objects.requireNonNull(e.getFieldError()).getDefaultMessage();
        String field = e.getFieldError().getField();
        return R.error(HttpStatus.HTTP_BAD_REQUEST, field + ":" + defaultMessage);
    }

    @ResponseBody
    @ExceptionHandler(DuplicateKeyException.class)
    public R<String> handleDuplicateKeyException(DuplicateKeyException e) {
        return R.error(HttpStatus.HTTP_CONFLICT, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(RuntimeException.class)
    public R<String> handleRuntimeException(RuntimeException e) {
        // todo
        e.printStackTrace();
        return R.error(HttpStatus.HTTP_BAD_REQUEST, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        e.printStackTrace();
        return R.error(HttpStatus.HTTP_BAD_REQUEST, "JSON语法错误" + e.getMessage());
    }
//
//    @ResponseBody
//    @ExceptionHandler(MismatchedInputException.class)
//    public R<String> handleMismatchedInputException(MismatchedInputException e) {
//        return R.error(HttpStatus.HTTP_BAD_REQUEST, "JSON语法错误");
//    }

}
