package com.anishan.user.entity.vo;

import com.anishan.commons.e.UserState;
import com.anishan.commons.e.ValidationGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@ApiModel("用户VO类，展示数据")
public class UserVo {

    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("用户ID")
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

}
