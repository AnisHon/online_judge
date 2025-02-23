package com.anishan.user.service;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.enumeration.CaptchaCodeType;
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
        AbstractCaptcha captcha = AuthUtil.generateCaptchaCode(CaptchaCodeType.MATH);
        log.debug("captcha:{}", captcha);
        log.info("captcha:{}", captcha.getImageBase64Data());
        log.info("captcha:{}", captcha.getCode());
    }


}
