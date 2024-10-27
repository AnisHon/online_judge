package com.anishan.user.domain.dto;

import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.user.domain.entity.SysUserRoleRelation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@ApiModel("带ID的PageQuery")
@Data
public class PagedUserRoleQuery extends PagedQuery<SysUserRoleRelation> {

    @ApiModelProperty("角色ID用于通过角色查找用户")
    private Long roleId;
    @ApiModelProperty("用户ID用于通过用户查找角色")
    private String userId;
    @ApiModelProperty("用户名")
    private String username;
    @ApiModelProperty("邮箱")
    private String email;
    @ApiModelProperty("昵称")
    private String nikeName;
}
