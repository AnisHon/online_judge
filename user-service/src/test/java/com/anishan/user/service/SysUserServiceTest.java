package com.anishan.user.service;

import org.junit.jupiter.api.Test;
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


    @Test
    public void getUserByIdTest() {
        System.out.println(passwordEncoder.encode("www.github.com"));
        String pass = "$2a$10$cu.mwqY2JT1pGcIQM.h0R.GVi.yx8P4KC3UANgP7ypxsFaGxUR17m";
        System.out.println(passwordEncoder.matches("www.github.com", pass));
        pass = "$2a$10$6K/unS1cQMnPBprGkb4IweNehr.Us6Z0P5c27mwKL7EhvKJoG7C3y";
        System.out.println(passwordEncoder.matches( "change-this-default-password", pass));
    }

    @Test
    public void jwtTest() {
//        String send = MailUtil.send("3137687133@qq.com", "sub", "hello", false);
//        System.out.println(EmailSender.sendEmailCodeAsync("3137687133@qq.com"));

    }


}
