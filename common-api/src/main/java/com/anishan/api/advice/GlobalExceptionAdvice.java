package com.anishan.api.advice;

import cn.hutool.http.HttpStatus;
import com.anishan.commons.config.SharedConfig;
import com.anishan.commons.domain.R;
import com.anishan.commons.exception.BusinessException;
import com.anishan.commons.exception.IllegalTokenException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
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
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GlobalExceptionAdvice {

    private final SharedConfig sharedConfig;

    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public R<String> handleException(Exception e) {
        log.error(e.getMessage(), e);
        if (sharedConfig.isProduct()) {
            return R.error(HttpStatus.HTTP_BAD_REQUEST, "出现错误，请联系管理员");
        } else {
            Throwable temp = e;
            StringBuilder builder = new StringBuilder();
            while (temp != null) {
                builder.append(temp.getMessage()).append("\n");
                temp = temp.getCause();
            }

            return R.error(HttpStatus.HTTP_INTERNAL_ERROR, builder.toString());
        }


    }

    @ResponseBody
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public R<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.debug("IllegalArgumentException", e);
        if (sharedConfig.isProduct()) {
            return R.error(HttpStatus.HTTP_BAD_REQUEST, e.getMessage());
        } else {
            return R.error(HttpStatus.HTTP_BAD_REQUEST, "出现错误");
        }
    }

    @ResponseBody
    @ExceptionHandler(IllegalTokenException.class)
    public R<String> handleIllegalTokenException(IllegalTokenException e) {
        log.debug("IllegalTokenException", e);
        return R.error(HttpStatus.HTTP_UNAUTHORIZED, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(PermissionDeniedDataAccessException.class)
    public R<String> handlePermissionDeniedDataAccessException(PermissionDeniedDataAccessException e) {
        log.debug("PermissionDeniedDataAccessException", e);
        return R.error(HttpStatus.HTTP_UNAUTHORIZED, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(MissingRequestHeaderException.class)
    public R<String> handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        log.debug("MissingRequestHeaderException", e);
        return R.error(HttpStatus.HTTP_UNAUTHORIZED, null);
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<String> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.debug("MethodArgumentTypeMismatchException", e);
        if (sharedConfig.isProduct()) {
            return R.error(HttpStatus.HTTP_BAD_REQUEST, e.getMessage());
        } else {
            return R.error(HttpStatus.HTTP_UNAUTHORIZED, "出现错误");
        }
    }

    @ResponseBody
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED)
    public R<String> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.debug("HttpRequestMethodNotSupportedException", e);
        return R.error(HttpStatus.HTTP_BAD_METHOD, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    public R<String> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.debug("NoHandlerFoundException", e);
        return R.error(HttpStatus.HTTP_NOT_FOUND, e.getMessage());
    }



    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public R<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String defaultMessage = Objects.requireNonNull(e.getFieldError()).getDefaultMessage();
        log.debug("MethodArgumentNotValidException", e);
        return R.error(HttpStatus.HTTP_BAD_REQUEST, defaultMessage);
    }

    @ResponseBody
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.CONFLICT)
    public R<String> handleDuplicateKeyException(DuplicateKeyException e) {
        log.debug("DuplicateKeyException", e);
        return R.error(HttpStatus.HTTP_CONFLICT, "字段冲突，请查对后再提交");
    }

    @ResponseBody
    @ExceptionHandler(ConstraintViolationException.class)
    public R<String> handleConstraintViolationException(ConstraintViolationException e) {
        log.debug("ConstraintViolationException", e);
        return R.error(HttpStatus.HTTP_BAD_REQUEST, "使用了不存在的对象，请检查后重试");
    }

    @ResponseBody
    @ExceptionHandler(RuntimeException.class)
    public R<String> handleRuntimeException(RuntimeException e) {

        log.debug("RuntimeException", e);
        if (sharedConfig.isProduct()) {
            return R.error(HttpStatus.HTTP_BAD_REQUEST, "出现错误");
        } else {
            return R.error(HttpStatus.HTTP_BAD_REQUEST, e.getMessage());
        }
    }

    @ResponseBody
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.debug(e.getMessage(), e);
        if (sharedConfig.isProduct()) {
            return R.error(HttpStatus.HTTP_BAD_REQUEST, "请求错误");

        } else {
            return R.error(HttpStatus.HTTP_BAD_REQUEST, "JSON语法错误" + e.getMessage());

        }
    }

    @ResponseBody
    @ExceptionHandler(BusinessException.class)
    public R<String> handleBusinessException(BusinessException e) {
        log.debug("BusinessException", e);
        return R.error(HttpStatus.HTTP_BAD_REQUEST, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(AccessDeniedException.class)
    public R<String> handleAccessDeniedException(AccessDeniedException e) {
        log.debug("AccessDeniedException", e);
//        System.out.println(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return R.forbidden();
    }

    @ResponseBody
    @ExceptionHandler(JsonParseException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public R<String> handleJsonParseException(JsonParseException e) {
        log.debug("JsonParseException", e);
        return R.error(HttpStatus.HTTP_BAD_REQUEST, e.getMessage());
    }


    @ResponseBody
    @ExceptionHandler(MismatchedInputException.class)
    public R<String> handleMismatchedInputException(MismatchedInputException e) {
        log.debug("MismatchedInputException", e);
        return R.error(HttpStatus.HTTP_BAD_REQUEST, "出现错误");
    }

}
