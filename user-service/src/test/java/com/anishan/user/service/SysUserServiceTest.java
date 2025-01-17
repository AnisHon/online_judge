package com.anishan.user.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@Slf4j
@SpringBootTest
public class SysUserServiceTest {


    @Resource
    private EmailService emailService;

    @Test
    @SneakyThrows
    public void test() {
        String s = emailService.sendEmailCode("3137687133@qq.com");
        log.info("code: {}", s);
        Thread.sleep(10*1000);
    }


}
