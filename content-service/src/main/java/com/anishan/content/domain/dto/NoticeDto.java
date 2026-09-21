package com.anishan.content.domain.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
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
@Accessors(chain = true)
@ApiModel(value="Notice对象", description="文件信息表")
public class NoticeDto {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long noticeId;

    @ApiModelProperty(value = "公告标题")
    private String title;

    @ApiModelProperty(value = "发送者ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @ApiModelProperty(value = "是否置顶")
    private Boolean topUp;

    @ApiModelProperty(value = "公告内容")
    private String content;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
