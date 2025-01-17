package com.anishan.user.domain.dto;

import com.anishan.commons.annotation.ConditionColumn;
import com.anishan.commons.annotation.SortedColumn;
import com.anishan.commons.domain.dto.SortedPagedQuery;
import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.commons.enumeration.MenuType;
import com.anishan.user.domain.entity.SysMenu;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("菜单分页 条件 查询参数类")
public class MenuPagedQuery extends SortedPagedQuery<SysMenu> {

    @ConditionColumn("eq")
    @SortedColumn
    @ApiModelProperty("菜单ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long menuId;

    @ConditionColumn
    @SortedColumn
    @ApiModelProperty("菜单名称")
    private String menuName;

    @ConditionColumn("eq")
    @SortedColumn
    @ApiModelProperty("父菜单ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    @ConditionColumn
    @SortedColumn
    @ApiModelProperty("路由路径")
    private String router;

    @ConditionColumn("eq")
    @SortedColumn
    @ApiModelProperty("菜单类型（I菜单项item M菜单栏MenuBar B按钮）")
    private MenuType menuType;

    @ConditionColumn
    @SortedColumn
    @ApiModelProperty("权限Security标识")
    private String perms;

    @ConditionColumn
    @SortedColumn
    @ApiModelProperty("菜单图标")
    private String icon;

    @ApiModelProperty("标记")
    private String remark;


}
