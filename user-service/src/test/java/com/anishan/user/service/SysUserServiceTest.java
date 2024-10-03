package com.anishan.user.service;

import com.anishan.user.entity.vo.UserVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class SysUserServiceTest {


    @Resource
    SysUserService sysUserService;

    @Test
    public void getUserByIdTest() {
        UserVo userById = sysUserService.getUserById(1L);
//        log.debug(userById.toString());
        System.out.println(userById);
    }


}
