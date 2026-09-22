package com.anishan.user.domain.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class SysUserInfoDto {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String nikeName;

    @Length(max = 160, message = "个性签名不能超过160个字符")
    private String signature;

}
