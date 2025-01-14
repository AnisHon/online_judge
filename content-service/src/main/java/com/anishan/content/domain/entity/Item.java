package com.anishan.content.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 轮播图表
 * </p>
 *
 * @author anishan
 * @since 2025-01-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("item")
@ApiModel(value="Item对象", description="轮播图表")
public class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "item_id", type = IdType.ASSIGN_ID)
    private Long itemId;

    @ApiModelProperty(value = "标题")
    private String title;

    @ApiModelProperty(value = "图片链接")
    private String image;

    @ApiModelProperty(value = "跳转链接")
    private String link;

    @TableLogic
    @ApiModelProperty(value = "删除标记")
    private Boolean delFlag;

    private LocalDateTime createTime;

    @Version
    private LocalDateTime updateTime;


}
