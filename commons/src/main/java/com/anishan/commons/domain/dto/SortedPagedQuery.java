package com.anishan.commons.domain.dto;

import cn.hutool.core.util.StrUtil;
import com.anishan.commons.exception.UnknownKeyException;
import com.anishan.commons.util.MysqlMappingUtils;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("排序范型类")
public abstract class SortedPagedQuery<T> extends PagedQuery<T> {
    @ApiModelProperty("排序列，只能是以上列，名字也要严格按照以上列写")
    protected String sortColumn;
    @ApiModelProperty("是否是生序，默认true")
    protected boolean asc = true;

    private final ConcurrentHashMap<String, Map<String, String>> orderMap = new ConcurrentHashMap<>();

    @SuppressWarnings("all")
    protected Class<T> extendedClass() {
        return (Class<T>) this.getClass();
    }

    protected Object extendedObject() {
        return this;
    }

    protected synchronized Map<String, String> extendedKeyMapping() {
        Class<T> clazz = extendedClass();
        Map<String, String> keyMapping;
        String fullClassName = clazz.getName();
        if (orderMap.containsKey(fullClassName)) {
            keyMapping = MysqlMappingUtils.mapColumn(clazz);
            orderMap.put(fullClassName, keyMapping);
        } else {
            keyMapping = orderMap.get(fullClassName);
        }
        return keyMapping;
    }

    public Wrapper<T> wrapper() {
       return wrapper(null);
    }

    public Wrapper<T> wrapper(QueryWrapper<T> queryWrapper) {
        return MysqlMappingUtils.buildWrapper(extendedObject(), queryWrapper);
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
