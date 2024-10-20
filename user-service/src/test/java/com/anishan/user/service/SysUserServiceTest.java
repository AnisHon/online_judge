package com.anishan.user.service;

import com.anishan.api.domain.SysUser;
import com.anishan.user.util.EmailSender;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
        SysUser user = sysUserService.getUserByUsernameOrEmail("anishan");
        System.out.println(user);
    }

    @Test
    public void jwtTest() {
//        String send = MailUtil.send("3137687133@qq.com", "sub", "hello", false);
        System.out.println(EmailSender.sendEmailCodeAsync("3137687133@qq.com"));

    }


}
