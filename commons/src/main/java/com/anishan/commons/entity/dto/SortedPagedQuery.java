package com.anishan.commons.entity.dto;

import cn.hutool.core.util.StrUtil;
import com.anishan.commons.exception.UnknownKeyException;
import com.anishan.commons.util.MysqlMappingUtils;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("排序范型类")
public abstract class SortedPagedQuery<T> extends PagedQuery<T> {
    @ApiModelProperty("排序列，只能是以上列，名字也要严格按照以上列写")
    protected String sortColumn;
    @ApiModelProperty("是否是生序，默认true")
    protected boolean asc = true;

    protected abstract Class<T> extendedClass();

    protected abstract Object extendedObject();

    protected abstract Map<String, String> extendedKeyMapping();

    public Wrapper<T> wrapper() {
        return MysqlMappingUtils.buildWrapper(extendedObject(), extendedClass());
//        return new LambdaQueryWrapper<SysUser>()
//                .eq(userId != null, SysUser::getUserId, userId)
//                .like(userName != null, SysUser::getUserName, userName)
//                .like(email != null, SysUser::getEmail, email)
//                .like(nikeName != null, SysUser::getNikeName, nikeName)
//                .eq(status != null, SysUser::getStatus, status);
    }

    public QueryWrapper<T> queryWrapper() {
        return (QueryWrapper<T>) wrapper();
    }

    public LambdaQueryWrapper<T> lambdaQueryWrapper() {
        return queryWrapper().lambda();
    }

    @Override
    public Page<T> page() {
        Page<T> page = super.page();
        final Map<String, String> KEY_MAPPING = extendedKeyMapping();
        if (StrUtil.isAllBlank(sortColumn)) {
            return page;
        }


        OrderItem orderItem = new OrderItem();
        // if key doesn't exist

        if (!KEY_MAPPING.containsKey(sortColumn)) {
            throw new UnknownKeyException(sortColumn);
        }

        orderItem.setAsc(asc);
        orderItem.setColumn(KEY_MAPPING.get(sortColumn));
        page.addOrder(orderItem);



        return page;
    }
}
