package com.anishan.user.service.impl;

import com.anishan.api.config.ConstConfig;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.user.config.UserConfig;
import com.anishan.user.service.SysMenuService;
import com.anishan.user.service.SysUserRoleService;
import com.anishan.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.anishan.user.service.impl.SysUserServiceImpl.getLoginUser;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserService sysUserService;
    private final SysUserRoleService sysUserRoleService;
    private final SysMenuService sysMenuService;
    private final UserConfig userConfig;


    private LoginUser loadByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserService.getUserByUsernameOrEmail(username);
        Optional.ofNullable(sysUser).orElseThrow(() -> new UsernameNotFoundException(username));

        // root uses in-memory password set by nacos
        if ("root".equals(username)) {
            return sysUserService.getRootAccount();
        }

        return getLoginUser(sysUser, sysUserRoleService, sysMenuService);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.loadByUsername(username);
    }
}
