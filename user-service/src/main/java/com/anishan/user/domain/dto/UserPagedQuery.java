package com.anishan.user.domain.dto;

import com.anishan.commons.annotation.ConditionColumn;
import com.anishan.commons.annotation.SortedColumn;
import com.anishan.commons.domain.dto.SortedPagedQuery;
import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.commons.enumeration.UserState;
import com.anishan.api.domain.entity.SysUser;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.*;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("用户查询,String类型支持模糊查询")
public class UserPagedQuery extends SortedPagedQuery<SysUser> {

    private static final Map<String, String> KEY_MAPPING;

    static {

        KEY_MAPPING = MysqlMappingUtils.mapColumn(UserPagedQuery.class);
//        KEY_MAPPING = new HashMap<>();
//        KEY_MAPPING.put("userId", "user_id");
//        KEY_MAPPING.put("userName", "user_name");
//        KEY_MAPPING.put("email", "email");
//        KEY_MAPPING.put("nikeName", "nike_name");
    }

    @SortedColumn
    @ApiModelProperty("用户id")
    @ConditionColumn("eq")
    private Long userId;

    @SortedColumn
    @ConditionColumn
    @ApiModelProperty("用户名")
    private String userName;

    @SortedColumn
    @ConditionColumn
    @ApiModelProperty("用户邮箱")
    private String email;

    @SortedColumn
    @ConditionColumn
    @ApiModelProperty("用户自定义名")
    private String nikeName;

    @ConditionColumn("eq")
    @ApiModelProperty("用户状态(0 1封禁)")
    private UserState status;


    @Override
    protected Class<SysUser> extendedClass() {
        return SysUser.class;
    }

    @Override
    protected Object extendedObject() {
        return this;
    }

    @Override
    protected Map<String, String> extendedKeyMapping() {
        return KEY_MAPPING;
    }
}
