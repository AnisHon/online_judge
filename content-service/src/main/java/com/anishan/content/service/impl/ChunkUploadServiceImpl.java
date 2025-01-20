package com.anishan.content.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.anishan.content.domain.dto.ChunkUploadDto;
import com.anishan.content.domain.entity.ChunkUpload;
import com.anishan.content.domain.entity.FileInfo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.content.service.ChunkUploadService;
import com.anishan.content.mapper.ChunkUploadMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* @author happy
* @description 针对表【chunk_upload(分片上传记录)】的数据库操作Service实现
* @createDate 2025-01-19 23:00:23
*/
@Service
@RequiredArgsConstructor
public class ChunkUploadServiceImpl extends ServiceImpl<ChunkUploadMapper, ChunkUpload>
    implements ChunkUploadService{

    private final ChunkUploadMapper chunkUploadMapper;

    @Override
    public ChunkUploadDto getByMd5(String md5) {

        MPJLambdaWrapper<ChunkUpload> wrapper = new MPJLambdaWrapper<ChunkUpload>()
                .selectAll(ChunkUpload.class)
                .select(FileInfo::getFilePath, FileInfo::getFileName, FileInfo::getFileSize, FileInfo::getFileMd5)
                .leftJoin(FileInfo.class, FileInfo::getFileId, ChunkUpload::getChunkId)
                .eq(FileInfo::getFileMd5, md5)
                .last("limit 1");

        return chunkUploadMapper.selectJoinOne(ChunkUploadDto.class, wrapper);
    }

    @Override
    @Transactional
    public Long saveChunkUpload(ChunkUploadDto chunkUploadDto) {

        long id = IdWorker.getId();
        String filePath = chunkUploadDto.getFilePath();

        String suffix = FileNameUtil.getSuffix(filePath);

        FileInfo fileInfo = new FileInfo()
                .setFileId(id)
                .setFileName(chunkUploadDto.getFileName())
                .setFilePath(chunkUploadDto.getFilePath())
                .setFileSize(chunkUploadDto.getFileSize())
                .setFileMd5(chunkUploadDto.getFileMd5())
                .setReference(0L)
                .setFileType(suffix)
                .setUploadTime(LocalDateTimeUtil.now());


        ChunkUpload chunkUpload = new ChunkUpload()
                .setChunkId(id)
                .setUploadId(chunkUploadDto.getUploadId())
                .setChunkSize(chunkUploadDto.getChunkSize())
                .setChunkNum(chunkUploadDto.getChunkNum());

        Db.save(fileInfo);
        this.save(chunkUpload);
        return id;
    }
}




