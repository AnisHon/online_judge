package com.anishan.user.domain.dto;

import com.anishan.commons.enumeration.ValidationGroup;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 角色ID
     */
    @NotNull(groups = {ValidationGroup.Insert.class, ValidationGroup.Delete.class})
    @ApiModelProperty("角色ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roleId;


}