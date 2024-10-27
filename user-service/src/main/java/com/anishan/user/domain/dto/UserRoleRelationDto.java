package com.anishan.user.domain.dto;

import com.anishan.commons.e.ValidationGroup;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("用户角色关联")
public class UserRoleRelationDto {
    /**
     * 用户ID
     */
    @NotNull(groups = {ValidationGroup.Insert.class, ValidationGroup.Delete.class})
    @ApiModelProperty("用户ID")
    private Long userId;

    /**
     * 角色ID
     */
    @NotNull(groups = {ValidationGroup.Insert.class, ValidationGroup.Delete.class})
    @ApiModelProperty("角色ID")
    private Long roleId;


}