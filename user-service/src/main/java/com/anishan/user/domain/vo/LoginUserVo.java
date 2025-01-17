package com.anishan.user.domain.vo;

import com.anishan.commons.enumeration.UserState;
import com.anishan.commons.enumeration.ValidationGroup;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LoginUserVo {
    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @ApiModelProperty("用户名，唯一，可用于登陆")
    private String userName;

    @ApiModelProperty("用户邮箱，唯一，可用于登陆")
    private String email;

    @ApiModelProperty("昵称")
    private String nikeName;

    @ApiModelProperty("状态(1封禁, 0正常)")
    private UserState status;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("权限")
    private List<String> auths;
}
