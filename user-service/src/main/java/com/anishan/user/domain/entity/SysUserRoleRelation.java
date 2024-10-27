package com.anishan.user.domain.entity;

import com.anishan.user.domain.dto.UserRoleRelationDto;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户和角色关联表
 * @TableName sys_user_role
 */
@TableName(value ="sys_user_role")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SysUserRoleRelation {

    public SysUserRoleRelation(UserRoleRelationDto relation) {
        this.userId = relation.getUserId();
        this.roleId = relation.getRoleId();
    }

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色ID
     */
    private Long roleId;

}