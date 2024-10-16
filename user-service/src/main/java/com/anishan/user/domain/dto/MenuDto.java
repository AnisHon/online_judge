package com.anishan.user.domain.dto;

import com.anishan.commons.e.MenuType;
import com.anishan.commons.e.ValidationGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel("菜单参数")
public class MenuDto {

    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("菜单ID，插入时不管用，不用设置")
    private Long menuId;

    @ApiModelProperty("菜单名称")
    private String menuName;

    @ApiModelProperty("父菜单ID, 0表示没有父菜单")
    private Long parentId;

    @ApiModelProperty("路由路径")
    private String router;

    @ApiModelProperty("菜单类型（I菜单项item M菜单栏MenuBar B按钮）")
    private MenuType menuType;

    @ApiModelProperty("权限Security标识")
    private String perms;

    @ApiModelProperty("菜单图标")
    private String icon;

    @ApiModelProperty("标记")
    private String remark;
}
