package com.anishan.user.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("角色数据类")
public class RoleVo {

    @ApiModelProperty("角色ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roleId;

    @ApiModelProperty("角色名称")
    private String roleName;

    @ApiModelProperty("面向用户展示的角色名称")
    private String displayName;

    @ApiModelProperty("是否作为特殊身份标签公开展示")
    private Boolean specialRole;


    @ApiModelProperty("角色状态（0正常 1停用）")
    private Integer status;


    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("备注")
    private String remark;
}
