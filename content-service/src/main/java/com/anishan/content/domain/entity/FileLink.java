package com.anishan.content.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 文件目录链接表
 * </p>
 *
 * @author anishan
 * @since 2025-01-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("file_link")
@ApiModel(value="FileLink对象", description="文件目录链接表")
public class FileLink implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "link_id", type = IdType.ASSIGN_ID)
    private Long linkId;

    @ApiModelProperty(value = "文件id")
    private Long fileId;

    @ApiModelProperty(value = "引用计数")
    private Long reference;


}
