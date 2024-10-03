package com.anishan.user.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@ApiModel("用于添加用户，有权限和密码，密码自动加密")
public class SysUserDto {
    @NotNull
    @ApiModelProperty(value = "用户名，唯一，可用于登陆", required = true)
    private String userName;
    @NotNull
    @ApiModelProperty(value = "用户组，具体值参考role", required = true)
    private String role;
    @ApiModelProperty("用户邮箱，唯一，可用于登陆，可以留空，默认为username@role.com")
    private String email;
    @ApiModelProperty("昵称，不填写会自动生成")
    private String nikeName;
    @NotNull
    @Length(min = 8, max = 16)
    @ApiModelProperty(value = "密码，明文", required = true)
    private String password;
    @ApiModelProperty("状态(1封禁, 0正常) 默认为正常")
    private Integer status = 0;
    @ApiModelProperty("备注")
    private String remark;
    @ApiModelProperty("删除标记，默认0未被删除")
    private Integer delFlag = 0;

}
