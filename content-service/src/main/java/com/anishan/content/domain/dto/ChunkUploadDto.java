package com.anishan.content.domain.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 分片上传记录
 * @TableName chunk_upload
 */
@Data
@Accessors(chain = true)
public class ChunkUploadDto {
    /**
     * 
     */
    private Long chunkId;

    /**
     * 分片上传的uploadId
     */
    private String uploadId;

    private String filePath;

    /**
     * 文件唯一标识（md5）
     */
    private String fileMd5;

    /**
     * 文件名
     */
    private String fileName;

    private Long fileSize;
    /**
     * 每个分片大小（byte）
     */
    private Long chunkSize;

    /**
     * 分片数量
     */
    private Integer chunkNum;
}