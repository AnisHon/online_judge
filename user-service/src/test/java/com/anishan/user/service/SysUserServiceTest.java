package com.anishan.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@Slf4j
@SpringBootTest
public class SysUserServiceTest {


    @Resource
    private EmailService emailService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @SneakyThrows
    public void test() {
        String str = "hello world \r\n hello \n world \r";
        String s = str.replaceAll("\r\n?", "\n");
        System.out.println(s);
        System.out.println(objectMapper.writeValueAsString(s));
        Thread.sleep(10*1000);
    }


}
