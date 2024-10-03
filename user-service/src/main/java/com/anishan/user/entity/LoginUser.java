package com.anishan.user.entity;

import com.anishan.user.entity.po.SysAuthority;
import com.anishan.user.entity.po.SysMenu;
import com.anishan.user.entity.po.SysRole;
import com.anishan.user.entity.po.SysUser;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
public class LoginUser implements UserDetails {

    private SysUser user;
    private List<SysRole> roles;
    private List<SysAuthority> auths;
    private boolean isEmail;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return isEmail ? user.getEmail() : user.getPassword();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() == 0;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
