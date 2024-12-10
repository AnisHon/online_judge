package com.anishan.user.domain.vo;

import com.anishan.commons.enumeration.MenuType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("权限/菜单类")
public class MenuVo {

    @ApiModelProperty("菜单ID")
    private Long menuId;

    @ApiModelProperty("菜单名称")
    private String menuName;

    @ApiModelProperty("路由排序")
    private Integer orderNum;

    @ApiModelProperty("父菜单ID")
    private Long parentId;

    @ApiModelProperty("路由路径")
    private String router;

    @ApiModelProperty("菜单类型（I菜单项item M菜单栏MenuBar B按钮）")
    private MenuType menuType;

    @ApiModelProperty("权限Security标识")
    private String perms;

    @ApiModelProperty("菜单图标")
    private String icon;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("标记")
    private String remark;

    @JsonIgnore
    public boolean isRoot() {
        return parentId == null || parentId == 0 ;
    }

}
