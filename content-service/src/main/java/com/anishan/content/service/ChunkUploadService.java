package com.anishan.content.service;

import com.anishan.content.domain.dto.ChunkUploadDto;
import com.anishan.content.domain.entity.ChunkUpload;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author happy
* @description 针对表【chunk_upload(分片上传记录)】的数据库操作Service
* @createDate 2025-01-19 23:00:23
*/
public interface ChunkUploadService extends IService<ChunkUpload> {

    ChunkUploadDto getByMd5(String md5);

    Long saveChunkUpload(ChunkUploadDto chunkUpload);
}
