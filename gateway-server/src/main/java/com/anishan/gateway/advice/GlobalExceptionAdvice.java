package com.anishan.gateway.advice;

import cn.hutool.http.HttpStatus;
import com.anishan.commons.entity.R;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionAdvice {

    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public R<String> handleException(Exception e) {
        return R.error(HttpStatus.HTTP_BAD_GATEWAY, "路由故障");
    }



}
