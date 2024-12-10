package com.anishan.commons.domain.dto;

import com.anishan.commons.enumeration.UserState;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel("传入参数User的实体类")
public class UserDto {
    @NotNull
    @ApiModelProperty("用户ID")
    private Long userId;
    @ApiModelProperty("用户邮箱，唯一，可用于登陆")
    private String email;
    @ApiModelProperty("昵称")
    private String nikeName;
    @ApiModelProperty("状态(1封禁, 0正常)")
    private UserState status;
    @ApiModelProperty("备注")
    private String remark;
}
