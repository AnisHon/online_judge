package com.anishan.user.service;

import com.anishan.api.config.ConstConfig;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.user.config.UserConfig;
import com.anishan.user.domain.dto.PagedUserRoleQuery;
import com.anishan.user.domain.dto.RoleMenuRelationDto;
import com.anishan.user.domain.dto.UserRoleRelationDto;
import com.anishan.user.domain.entity.SysUserRoleRelation;
import com.anishan.user.domain.vo.UserVo;
import com.anishan.user.mapper.SysRoleMenuMapper;
import com.anishan.user.mapper.SysUserMapper;
import com.anishan.user.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class SysUserServiceTest {


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
        System.out.println(userConfig);
    }
    @Resource
    SysUserRoleService sysUserRoleService;

    @Resource
    SysUserRoleMapper sysUserRoleMapper;

    @Resource
    SysRoleMenuService sysRoleMenuService;

    @Test
    public void jwtTest() {
        System.out.println(sysRoleMenuService.removeBatch(List.of(
                new RoleMenuRelationDto(1000L, 2000L),
                new RoleMenuRelationDto(1000L, 2000L),
                new RoleMenuRelationDto(1000L, 2000L),
                new RoleMenuRelationDto(1000L, 2000L)
        )));
    }


}
