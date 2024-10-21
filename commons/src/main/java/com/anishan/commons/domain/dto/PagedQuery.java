package com.anishan.commons.domain.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("分页查询参数")
@Validated
public class PagedQuery<T> {

    @NotNull(message = "分页大小不合规")
    @Min(value = 0, message = "分页大小不合规")
    @ApiModelProperty(value = "分页大小", required = true)
    private Long pageSize;

    @NotNull(message = "当前页面不合规")

    @Min(value = 1, message = "错误的页")
    @ApiModelProperty(value = "当前页", required = true)
    private Long currentPage;


    public Page<T> page() {
        return new Page<>(currentPage, pageSize);
    }

//    @ApiIgnore
//    public List<T> record() {
//        return new Page<T>(currentPage, pageSize).getRecords();
//    }

}
