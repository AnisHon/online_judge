package com.anishan.api.advice;

import cn.hutool.http.HttpStatus;
import com.anishan.commons.domain.R;
import com.anishan.commons.exception.IllegalTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import org.springframework.security.access.AccessDeniedException;

import javax.validation.ConstraintViolationException;
import java.util.Objects;

@ControllerAdvice
@Slf4j
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

        if (e != null) {
            log.error(e.getMessage(), e);
        }

        return R.error(HttpStatus.HTTP_INTERNAL_ERROR, builder.toString());
    }

    @ResponseBody
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public R<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return R.error(HttpStatus.HTTP_BAD_REQUEST, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(IllegalTokenException.class)
    public R<String> handleIllegalTokenException(IllegalTokenException e) {
        return R.error(HttpStatus.HTTP_UNAUTHORIZED, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(PermissionDeniedDataAccessException.class)
    public R<String> handlePermissionDeniedDataAccessException(PermissionDeniedDataAccessException e) {
        return R.error(HttpStatus.HTTP_UNAUTHORIZED, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<String> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return R.error(HttpStatus.HTTP_BAD_REQUEST, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED)
    public R<String> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return R.error(HttpStatus.HTTP_BAD_METHOD, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    public R<String> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return R.error(HttpStatus.HTTP_NOT_FOUND, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public R<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String defaultMessage = Objects.requireNonNull(e.getFieldError()).getDefaultMessage();
        String field = e.getFieldError().getField();
        return R.error(HttpStatus.HTTP_BAD_REQUEST, field + ":" + defaultMessage);
    }

    @ResponseBody
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.CONFLICT)
    public R<String> handleDuplicateKeyException(DuplicateKeyException e) {
        e.printStackTrace();
        return R.error(HttpStatus.HTTP_CONFLICT, "字段冲突，请查对后再提交");
    }

    @ResponseBody
    @ExceptionHandler(ConstraintViolationException.class)
    public R<String> handleConstraintViolationException(ConstraintViolationException e) {
        e.printStackTrace();
        return R.error(HttpStatus.HTTP_BAD_REQUEST, "使用了不存在的对象，请检查后重试");
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

    @ResponseBody
    @ExceptionHandler(AccessDeniedException.class)
    public R<String> handleAccessDeniedException(AccessDeniedException e) {
//        System.out.println(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return R.forbidden();
    }


//
//    @ResponseBody
//    @ExceptionHandler(MismatchedInputException.class)
//    public R<String> handleMismatchedInputException(MismatchedInputException e) {
//        return R.error(HttpStatus.HTTP_BAD_REQUEST, "JSON语法错误");
//    }

}
