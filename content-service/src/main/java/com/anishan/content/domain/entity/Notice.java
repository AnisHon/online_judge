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
 * 文件信息表
 * </p>
 *
 * @author anishan
 * @since 2025-01-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("notice")
@ApiModel(value="Notice对象", description="文件信息表")
public class Notice implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "notice_id", type = IdType.ASSIGN_ID)
    private Long noticeId;

    @ApiModelProperty(value = "公告标题")
    private String title;

    @ApiModelProperty(value = "发送者ID")
    private Long userId;

    @ApiModelProperty(value = "是否置顶")
    private Boolean topUp;

    @TableLogic
    @ApiModelProperty(value = "逻辑删除")
    private Boolean delFlag;

    private LocalDateTime createTime;

    @Version
    private LocalDateTime updateTime;


}
