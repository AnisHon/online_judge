package com.anishan.commons.domain.vo;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.commons.domain.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("分页查询参数")
public class PagedResult<T> {

    @ApiModelProperty("分页大小")
    private Long pageSize;

    @ApiModelProperty("当前页")
    private Long currentPage;

    @ApiModelProperty("总页数")
    private Long totalRecords;

    @ApiModelProperty("最终查询数据")
    private List<T> data;

    public static <T, K> PagedResult<T> fromPage(Page<K> page, Long total) {

        PagedResult<T> tPagedResult = new PagedResult<>();
        tPagedResult.setCurrentPage(page.getCurrent());
        tPagedResult.setPageSize(page.getSize());
        tPagedResult.setTotalRecords(total);
        return tPagedResult;
    }

    public static <T, K> PagedResult<T> fromPage(Page<K> page, List<T> data, Long total) {
        PagedResult<T> tPagedResult = fromPage(page, total);
        tPagedResult.setData(data);
        return tPagedResult;
    }


    public R<PagedResult<T>> toR() {
        return R.success(this);
    }

    public static <T> PagedResult<T> build(Page<T> page) {
        PagedResult<T> tPagedResult = new PagedResult<>();
        tPagedResult.setCurrentPage(page.getCurrent());
        tPagedResult.setPageSize(page.getSize());
        tPagedResult.setData(page.getRecords());
        tPagedResult.setTotalRecords(page.getTotal());
        return tPagedResult;
    }

    public static <T, K> PagedResult<K> build(Page<T> page, Class<K> clazz) {
        PagedResult<K> tPagedResult = new PagedResult<>();
        tPagedResult.setCurrentPage(page.getCurrent());
        tPagedResult.setPageSize(page.getSize());
        tPagedResult.setTotalRecords(page.getTotal());
        List<T> records = page.getRecords();
        List<K> ts = BeanUtil.copyToList(records, clazz);
        tPagedResult.setData(ts);
        return tPagedResult;
    }

}
