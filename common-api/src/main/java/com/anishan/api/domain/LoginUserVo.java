package com.anishan.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Data
public class LoginUserVo {
    @JsonSerialize
    private UserVo user;
    private List<RoleVo> roles;
    private List<String> auths;

    @JsonIgnore
    private List<SimpleGrantedAuthority> authorities;
}
