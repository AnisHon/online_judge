package com.anishan.user.entity.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("权限/菜单类")
public class MenuVo {

    @ApiModelProperty("菜单ID")
    private Long menuId;

    @ApiModelProperty("菜单名称")
    private String menuName;

    @ApiModelProperty("父菜单ID")
    private Long parentId;

    @ApiModelProperty("路由路径")
    private String router;

    @ApiModelProperty("菜单类型（I菜单项item M菜单栏MenuBar B按钮）")
    private String menuType;

    @ApiModelProperty("权限Security标识")
    private String perms;

    @ApiModelProperty("菜单图标")
    private String icon;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("标记")
    private String remark;
}
