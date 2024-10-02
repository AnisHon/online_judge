package com.anishan.user.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api("测试类")
public class TestController {


    @GetMapping("/test")
    @ApiOperation("测试方法，返回固定字符串test")
    public String test() {
        return "test";
    }


}
