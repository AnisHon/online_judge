package com.anishan.user.domain.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class SysUserInfoDto {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String nikeName;

}
