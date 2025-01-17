package com.anishan.user.domain.dto;

import com.anishan.commons.annotation.ConditionColumn;
import com.anishan.commons.annotation.SortedColumn;
import com.anishan.commons.domain.dto.SortedPagedQuery;
import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.api.domain.entity.SysRole;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("角色分页类")
public class RolePagedQuery extends SortedPagedQuery<SysRole> {

    @ConditionColumn("eq")
    @SortedColumn
    @ApiModelProperty("角色ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roleId;

    @ConditionColumn
    @SortedColumn
    @ApiModelProperty("角色名称")
    private String roleName;

    @ConditionColumn("eq")
    @ApiModelProperty("角色状态（0正常 1停用）")
    private Integer status;


    @ApiModelProperty("备注")
    private String remark;




}
