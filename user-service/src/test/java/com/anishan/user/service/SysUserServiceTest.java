package com.anishan.user.service;

import cn.hutool.extra.mail.MailUtil;
import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.user.entity.dto.UserPagedQuery;
import com.anishan.user.util.EmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Map;

@SpringBootTest
public class SysUserServiceTest {


    @Resource
    SysUserService sysUserService;

    @Test
    public void getUserByIdTest() {
        Map<String, String> stringStringMap = MysqlMappingUtils.mapColumn(UserPagedQuery.class);
        System.out.println(stringStringMap);
    }

    @Test
    public void jwtTest() {
//        String send = MailUtil.send("3137687133@qq.com", "sub", "hello", false);
        System.out.println(EmailSender.sendEmailCodeAsync("3137687133@qq.com"));

    }


}
