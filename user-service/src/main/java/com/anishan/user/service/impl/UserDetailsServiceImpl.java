package com.anishan.user.service.impl;

import com.anishan.user.domain.LoginUser;
import com.anishan.user.domain.entity.SysRole;
import com.anishan.user.domain.entity.SysUser;
import com.anishan.user.service.SysMenuService;
import com.anishan.user.service.SysUserRoleService;
import com.anishan.user.service.SysUserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserService sysUserService;
    private final SysUserRoleService sysUserRoleService;
    private final SysMenuService sysMenuService;

    public UserDetailsServiceImpl(
            SysUserService sysUserService,
            SysUserRoleService sysUserRoleService,
            SysMenuService sysMenuService
    ) {
        this.sysUserService = sysUserService;
        this.sysUserRoleService = sysUserRoleService;
        this.sysMenuService = sysMenuService;
    }

    private LoginUser loadByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserService.getUserByUsernameOrEmail(username);

        Optional.ofNullable(sysUser).orElseThrow(() -> new UsernameNotFoundException(username));

        List<SysRole> roles;
        List<String> authorities;
        try {
            roles = sysUserRoleService.getRolesByUserId(sysUser.getUserId());
        } catch (Exception e) {
            roles = new ArrayList<>();
        }

        try {
            authorities = sysMenuService.getAuthorities_(roles);
        } catch (Exception e) {
            authorities = new ArrayList<>();
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUser(sysUser);
        loginUser.setRoles(roles);
        loginUser.setAuths(authorities);

        return loginUser;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.loadByUsername(username);
    }
}
