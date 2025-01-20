package com.anishan.user.domain.entity;

import com.anishan.commons.enumeration.MenuType;
import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 菜单权限表
 * @TableName sys_menu
 */
@TableName(value ="sys_menu")
@Data
public class SysMenu implements Serializable {
    /**
     * 菜单ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long menuId;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 路由排序
     */
    private Integer orderNum;

    /**
     * 父菜单ID
     */
    private Long parentId;

    /**
     * 路由路径
     */
    private String router;

    /**
     * 菜单类型（I菜单项item M菜单栏MenuBar B按钮）
     */
    private MenuType menuType;

    /**
     * 权限Security标识
     */
    private String perms;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Version
    private LocalDateTime updateTime;

    /**
     * 备注
     */
    private String remark;

    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;


}