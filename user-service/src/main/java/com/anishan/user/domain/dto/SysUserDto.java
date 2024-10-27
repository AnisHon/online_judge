package com.anishan.user.domain.dto;

import com.anishan.commons.e.UserState;
import com.anishan.commons.e.ValidationGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@ApiModel("用于添加用户，拥有几乎所有属性，有权限和密码，密码自动加密。不同于UserDto")
public class SysUserDto {
    @NotNull(groups = ValidationGroup.Update.class)
    @Pattern(regexp = "^[a-zA-Z0-9_-]{4,16}$", groups = ValidationGroup.Insert.class)
    @ApiModelProperty(value = "用户名，唯一，可用于登陆", required = true)
    private String userName;
    @NotNull()
    @ApiModelProperty(value = "roleId", required = true)
    private Long role;

    @Email(groups = ValidationGroup.Insert.class)
    @ApiModelProperty("用户邮箱，唯一，可用于登陆，可以留空，默认为username@role.com")
    private String email;
    @ApiModelProperty("昵称，不填写会自动生成")
    private String nikeName;
    @NotNull
    @Length(min = 8, max = 16)
    @ApiModelProperty(value = "密码，明文", required = true)
    private String password;
    @ApiModelProperty("状态(1封禁, 0正常) 默认为正常")
    private UserState status = UserState.NORMAL;
    @ApiModelProperty("备注")
    private String remark;
    @ApiModelProperty("删除标记，默认0未被删除")
    private Integer delFlag = 0;

}
