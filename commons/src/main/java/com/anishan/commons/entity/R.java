package com.anishan.commons.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

import static cn.hutool.http.HttpStatus.*;


@Data
@AllArgsConstructor
@ApiModel("响应Restful的实体类")
public class R<T> {


    @ApiModelProperty("Http Statue Code")
    private int code;
    @ApiModelProperty("消息，一般用于非200的响应")
    private String message;
    @ApiModelProperty("返回数据的")
    private T data;

    public static <T> R<T> success(T data) {
        return new R<>(HTTP_OK, "success", data);
    }

    public static R<Map<String, Object>> withMap() {
        return success(new HashMap<>());
    }

    public static R<String> success() {
        return success("ok");
    }

    public static R<String> error(int code, String message) {
        return new R<>(code, message, null);
    }


    public static R<String> error404() {
        return error(HTTP_NOT_FOUND, "Not Found");
    }


    /**
     * 没有登陆
     * @return 401
     */
    public static R<String> unauthorized() {
        return error(HTTP_UNAUTHORIZED, "unauthorized");
    }

    public static R<String> unauthorized(String msg) {
        return error(HTTP_UNAUTHORIZED, msg);
    }


    /**
     * 没权限
     * @return 403
     */
    public static R<String> forbidden() {
        return error(HTTP_FORBIDDEN, "Forbidden");
    }

    /**
     * 专属于网络爬虫的返回体
     * @return 返回403，拒绝网络爬虫
     */
    public static R<String> noWebCrawler() {
        return error(HTTP_FORBIDDEN, "Get lost with your snake and bugs");
    }




}
