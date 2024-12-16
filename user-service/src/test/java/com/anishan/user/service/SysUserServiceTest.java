package com.anishan.user.service;

import com.anishan.user.util.EmailSender;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@Slf4j
@SpringBootTest
public class SysUserServiceTest {


    @Resource
    private EmailSender emailSender;

    @Test
    @SneakyThrows
    public void test() {
        String s = emailSender.sendEmailCode("3137687133@qq.com");
        log.info("code: {}", s);
        Thread.sleep(10*1000);
    }


}
