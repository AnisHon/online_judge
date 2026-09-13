package com.anishan.gateway.advice;

import cn.hutool.http.HttpStatus;
import com.anishan.commons.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@Slf4j
public class GatewayGlobalExceptionAdvice {

    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public R<String> handleException(Exception e) {
        log.error("网关路由处理失败", e);
        return R.error(HttpStatus.HTTP_BAD_GATEWAY, "路由故障");
    }



}
