package com.anishan.user.service;

import com.anishan.api.config.ConstConfig;
import com.anishan.user.config.UserConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.Resource;

@SpringBootTest
public class SysUserServiceTest {


    @Resource
    SysUserService sysUserService;
    @Resource
    PasswordEncoder passwordEncoder;
    @Resource
    AuthenticationService authenticationService;
    @Resource
    UserConfig userConfig;
    @Test
    public void getUserByIdTest() {
        System.out.println(userConfig);
    }

    @Test
    public void jwtTest() {
//        String send = MailUtil.send("3137687133@qq.com", "sub", "hello", false);
//        System.out.println(EmailSender.sendEmailCodeAsync("3137687133@qq.com"));

    }


}
