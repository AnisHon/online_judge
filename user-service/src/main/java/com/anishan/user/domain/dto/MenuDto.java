package com.anishan.user.domain.dto;

import com.anishan.commons.enumeration.MenuType;
import com.anishan.commons.enumeration.ValidationGroup;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("菜单参数")
public class MenuDto {

    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("菜单ID，插入时不管用，不用设置")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long menuId;

    @NotEmpty
    @ApiModelProperty("菜单名称")
    private String menuName;

    @NotNull(groups = ValidationGroup.Insert.class)
    @ApiModelProperty("路由排序")
    private Integer orderNum;

    @ApiModelProperty("父菜单ID, 0表示没有父菜单")
    @JsonSerialize(using = ToStringSerializer.class)

    private Long parentId;

    @ApiModelProperty("路由路径")
    private String router;

    @ApiModelProperty("组件路径")
    private String component;

    @NotNull(groups = ValidationGroup.Insert.class)
    @ApiModelProperty("菜单类型（I菜单项item M菜单栏MenuBar B按钮）")
    private MenuType menuType;

    @ApiModelProperty("权限Security标识")
    private String perms;

    @ApiModelProperty("菜单图标")
    private String icon;

    @ApiModelProperty("标记")
    private String remark;
}
