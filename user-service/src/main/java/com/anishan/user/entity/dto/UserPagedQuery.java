package com.anishan.user.entity.dto;

import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.exception.UnknownKeyException;
import com.anishan.user.entity.po.SysUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.*;

@Data
@ApiModel("用户查询,String类型支持模糊查询")
public class UserPagedQuery extends PagedQuery<SysUser> {

    private static final Map<String, String> KEY_MAPPING;

    static {
        KEY_MAPPING = new HashMap<>();
        KEY_MAPPING.put("userId", "user_id");
        KEY_MAPPING.put("userName", "user_name");
        KEY_MAPPING.put("email", "email");
        KEY_MAPPING.put("nikeName", "nike_name");
    }

    @ApiModelProperty("用户id")
    private Long userId;
    @ApiModelProperty("用户名")
    private String userName;
    @ApiModelProperty("用户邮箱")
    private String email;
    @ApiModelProperty("用户自定义名")
    private String nikeName;
    @ApiModelProperty("用户状态(0 1封禁)")
    private Integer status;

    @ApiModelProperty("排序列，只能是以上列，名字也要严格按照以上列写")
    private String sortColumn;
    @ApiModelProperty("是否是生序，默认true")
    private boolean asc = true;




    public LambdaQueryWrapper<SysUser> wrapper() {
        return new LambdaQueryWrapper<SysUser>()
                .eq(userId != null, SysUser::getUserId, userId)
                .like(userName != null, SysUser::getUserName, userName)
                .like(email != null, SysUser::getEmail, email)
                .like(nikeName != null, SysUser::getNikeName, nikeName)
                .eq(status != null, SysUser::getStatus, status);
    }

    @Override
    public Page<SysUser> page() {
        Page<SysUser> page = super.page();

        Optional
                .ofNullable(sortColumn)
                .ifPresent( key -> {
                    OrderItem orderItem = new OrderItem();
                    // if key doesn't exist
                    if (!KEY_MAPPING.containsKey(sortColumn)) {
                        throw new UnknownKeyException(sortColumn);
                    }

                    orderItem.setAsc(asc);
                    orderItem.setColumn(KEY_MAPPING.get(sortColumn));
                    page.addOrder(orderItem);
                });


        return page;
    }
}
