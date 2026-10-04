package com.anishan.api.client.user.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/** Minimal public identity used by community and ranking features. */
@Data
@ApiModel("安全用户摘要")
public class UserSummaryVo {

    @ApiModelProperty("用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("昵称")
    private String nikeName;

    @ApiModelProperty("公开展示的特殊角色标签")
    private List<String> specialRoles;
}
