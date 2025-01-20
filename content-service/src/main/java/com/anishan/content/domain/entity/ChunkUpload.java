package com.anishan.content.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 分片上传记录
 * @TableName chunk_upload
 */
@TableName(value ="chunk_upload")
@Data
@Accessors(chain = true)
public class ChunkUpload {
    /**
     *
     */
    @TableId(value = "chunk_id")
    private Long chunkId;

    /**
     * 分片上传的uploadId
     */
    @TableField(value = "upload_id")
    private String uploadId;


    /**
     * 每个分片大小（byte）
     */
    @TableField(value = "chunk_size")
    private Long chunkSize;

    /**
     * 分片数量
     */
    @TableField(value = "chunk_num")
    private Integer chunkNum;
}