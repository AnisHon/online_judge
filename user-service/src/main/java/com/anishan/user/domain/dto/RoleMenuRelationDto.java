package com.anishan.user.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 角色和菜单关联表
 * @TableName sys_role_menu
 */
@TableName(value ="sys_role_menu")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleMenuRelationDto implements Serializable {
    /**
     * 角色ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roleId;

    /**
     * 菜单ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long menuId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}