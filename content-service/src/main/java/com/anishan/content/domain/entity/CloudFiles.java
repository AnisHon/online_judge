package com.anishan.content.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 网盘文件表
 * </p>
 *
 * @author anishan
 * @since 2025-01-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("cloud_files")
@ApiModel(value="CloudFiles对象", description="网盘文件表")
public class CloudFiles implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "cloud_file_id", type = IdType.ASSIGN_ID)
    private Long cloudFileId;

    @ApiModelProperty(value = "文件名")
    @TableField("file_name")
    private String fileName;

    @ApiModelProperty(value = "父文件ID")
    @TableField("parent_id")
    private Long parentId;

    @ApiModelProperty(value = "存储文件ID")
    @TableField("file_id")
    private Long fileId;

    @ApiModelProperty(value = "用户ID")
    @TableField("user_id")
    private Long userId;

    @ApiModelProperty(value = "是否是文件夹")
    @TableField("dir")
    private Boolean dir;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;


}
