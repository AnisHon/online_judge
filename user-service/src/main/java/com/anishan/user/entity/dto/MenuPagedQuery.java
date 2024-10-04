package com.anishan.user.entity.dto;

import com.anishan.commons.annotation.ConditionColumn;
import com.anishan.commons.annotation.SortedColumn;
import com.anishan.commons.entity.dto.SortedPagedQuery;
import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.user.e.MenuType;
import com.anishan.user.entity.po.SysMenu;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("菜单分页 条件 查询参数类")
public class MenuPagedQuery extends SortedPagedQuery<SysMenu> {

    private static final Map<String, String> KEY_MAPPING;
    static {
        KEY_MAPPING = MysqlMappingUtils.mapColumn(MenuPagedQuery.class);
    }

    @Override
    protected Class<SysMenu> extendedClass() {
        return SysMenu.class;
    }

    @Override
    protected Object extendedObject() {
        return this;
    }

    @Override
    protected Map<String, String> extendedKeyMapping() {
        return KEY_MAPPING;
    }

    @ConditionColumn("eq")
    @SortedColumn
    @ApiModelProperty("菜单ID")
    private Long menuId;

    @ConditionColumn
    @SortedColumn
    @ApiModelProperty("菜单名称")
    private String menuName;

    @ConditionColumn("eq")
    @SortedColumn
    @ApiModelProperty("父菜单ID")
    private Long parentId;

    @ConditionColumn
    @SortedColumn
    @ApiModelProperty("路由路径")
    private String router;

    @ConditionColumn
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
