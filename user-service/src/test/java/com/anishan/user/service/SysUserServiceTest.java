package com.anishan.user.service;

import com.anishan.api.client.gojudge.GoJudgeClient;
import com.anishan.user.config.UserConfig;
import com.anishan.user.mapper.SysRoleMenuMapper;
import com.anishan.user.mapper.SysUserMapper;
import com.anishan.user.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.Resource;

@SpringBootTest
public class SysUserServiceTest {


    @Resource
    GoJudgeClient goJudge;
    @Resource
    SysUserService sysUserService;

    @Resource
    SysUserMapper sysUserMapper;
    @Resource
    PasswordEncoder passwordEncoder;
    @Resource
    AuthenticationService authenticationService;
    @Resource
    UserConfig userConfig;
    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Test
    public void getUserByIdTest() {

    }
    @Resource
    SysUserRoleService sysUserRoleService;

    @Resource
    SysUserRoleMapper sysUserRoleMapper;

    @Resource
    SysRoleMenuService sysRoleMenuService;

    @Test
    public void jwtTest() {
    }


}
