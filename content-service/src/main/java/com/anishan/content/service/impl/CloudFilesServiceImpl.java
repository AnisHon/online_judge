package com.anishan.content.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.api.domain.PartHash;
import com.anishan.api.file.FileOperation;
import com.anishan.api.util.AuthUtil;
import com.anishan.api.util.CacheUtil;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.content.domain.dto.ChunkUploadDto;
import com.anishan.content.domain.dto.CloudFileDto;
import com.anishan.content.domain.dto.QueryCloudFile;
import com.anishan.content.domain.dto.SpliceChunk;
import com.anishan.content.domain.entity.CloudFiles;
import com.anishan.content.domain.entity.FileInfo;
import com.anishan.content.domain.vo.CloudFilesVo;
import com.anishan.content.domain.vo.SpliceVo;
import com.anishan.content.mapper.CloudFilesMapper;
import com.anishan.content.service.ChunkUploadService;
import com.anishan.content.service.CloudFilesService;
import com.anishan.content.service.FileInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 网盘文件表 服务实现类
 * </p>
 *
 * @author anishan
 * @since 2025-01-14
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CacheConfig(cacheNames = "content:file:")
public class CloudFilesServiceImpl extends ServiceImpl<CloudFilesMapper, CloudFiles> implements CloudFilesService {

    private final CloudFilesMapper cloudFilesMapper;

    private final UserInternalClient userClient;

    private final FileInfoService fileInfoService;

    private final FileOperation fileOperation;

    private final ChunkUploadService chunkUploadService;


    private static String assignPath(String suffix) {
        return FileOperation.assignPath("file", suffix);
    }

    @Override
    @Cacheable(key = "#query.parentId", condition = "T(cn.hutool.core.util.StrUtil).isBlank(#query.fileName) ")
    public List<CloudFilesVo> getCloudFilesVo(QueryCloudFile query) {

        MPJLambdaWrapper<CloudFiles> wrapper = new MPJLambdaWrapper<CloudFiles>()
                .selectAll(CloudFiles.class)
                .select(FileInfo::getFileId, FileInfo::getFileMd5, FileInfo::getFilePath, FileInfo::getFileSize)
                .leftJoin(FileInfo.class, FileInfo::getFileId, CloudFiles::getFileId)
                .likeRight(CloudFiles::getFileName, query.getFileName())
                .eq(CloudFiles::getParentId, query.getParentId());


        List<CloudFilesVo> cloudFiles = cloudFilesMapper.selectJoinList(CloudFilesVo.class, wrapper);


        // 填充用户名字段
        List<Long> userIds = cloudFiles
                .stream()
                .map(CloudFilesVo::getUserId)
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(userIds)) {
            return cloudFiles;
        }
        Map<Long, String> map = userClient.nikeName(userIds).getData();

        cloudFiles.forEach(cloudFile -> cloudFile.setNikeName(map.get(cloudFile.getUserId())));

        return cloudFiles;
    }

    @Override
    @CacheEvict(key = "#cloudFileDto.parentId")
    @Transactional(rollbackFor = Exception.class)
    public boolean saveCloudFile(CloudFileDto cloudFileDto, Long userId) {

        CloudFiles cloudFiles = new CloudFiles()
                .setFileId(cloudFileDto.getFileId())
                .setFileName(cloudFileDto.getFileName())
                .setParentId(cloudFileDto.getParentId())
                .setUserId(userId)
                .setDir(cloudFileDto.getDir());

        if (cloudFileDto.getFileId() != null) {
            fileInfoService.setReference(cloudFileDto.getFileId(), 1L);
        }

        return this.save(cloudFiles);
    }

    @Override
    @CacheEvict(allEntries = true)
    public boolean removeCloudFile(Long id) {
        CloudFiles cloudFiles = this.getById(id);
        boolean exists = this.exists(
                new LambdaQueryWrapper<CloudFiles>()
                        .eq(CloudFiles::getParentId, id)
        );

        // 不存在
        if (cloudFiles == null) {
            return false;
        }

        ThrowUtil.businessError(cloudFiles.getDir() && exists, "文件夹不是空的");

        // 减少引用计数
        if (!cloudFiles.getDir()) {
            Long fileId = cloudFiles.getFileId();
            fileInfoService.setReference(fileId, -1L);
        }

        return this.removeById(id);
    }

    @SneakyThrows
    @Override
    public SpliceVo spliceUpload(MultipartFile file, String md5, Integer index, ChunkUploadDto chunkUpload) {
        InputStream inputStream = file.getInputStream();
        String uploadId = chunkUpload.getUploadId();
        String filePath = chunkUpload.getFilePath();
        fileOperation.uploadPart(filePath, uploadId, inputStream, index, inputStream.available());
        return null;
    }

    @Override
    @Transactional
    public boolean rename(CloudFileDto cloudFileDto) {
        String fileName = cloudFileDto.getFileName();

        CloudFiles cloudFiles = this.getById(cloudFileDto.getCloudFileId());
        if (cloudFiles == null) {
           return false;
        }

        cloudFiles.setFileName(fileName);

        boolean b = this.updateById(cloudFiles);

        Long parentId = cloudFiles.getParentId();
        CacheUtil.evict("content:file:", parentId);
        return b;
    }

    @Override
    public SpliceVo initSpliceUpload(SpliceChunk spliceChunk) {
        String fileName = spliceChunk.getFileName();

        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        String path = assignPath(suffix);
        String contentType = MediaTypeFactory
                .getMediaType(path)
                .orElse(MediaType.APPLICATION_OCTET_STREAM)
                .toString();

        String uploadId = fileOperation.createMultipartUpload(path, contentType);

        int chunkNum = (int) Math.ceil(spliceChunk.getTotalSize() * 1.0 / spliceChunk.getChunkSize());

        ChunkUploadDto chunkUpload = new ChunkUploadDto()
                .setChunkNum(chunkNum)
                .setChunkSize(spliceChunk.getChunkSize())
                .setFilePath(path)
                .setChunkSize(spliceChunk.getChunkSize())
                .setFileMd5(spliceChunk.getMd5())
                .setFileName(fileName)
                .setFileSize(spliceChunk.getTotalSize())
                .setUploadId(uploadId);

        Long id = chunkUploadService.saveChunkUpload(chunkUpload);

        return SpliceVo
                .fromDto(chunkUpload)
                .setFileId(id)
                .setFinished(false)
                .setPath(path)
                .setPartHashes(ListUtil.empty());
    }

    @Override
    public void merge(ChunkUploadDto chunkUpload) {
        List<PartHash> partHashes = fileOperation.listParts(chunkUpload.getFilePath(), chunkUpload.getUploadId());
        fileOperation.mergePart(chunkUpload.getFilePath(), chunkUpload.getUploadId(), partHashes);
        chunkUploadService.saveChunkUpload(chunkUpload);

    }

    @Override
    public SpliceVo getSlice(String md5) {
        ChunkUploadDto chunkUpload = chunkUploadService.getByMd5(md5);
        if (chunkUpload == null) {
            return null;
        }

        SpliceVo result = SpliceVo
                .fromDto(chunkUpload)
                .setFileId(chunkUpload.getChunkId())
                .setFinished(true)
                .setPath(chunkUpload.getFilePath());

        boolean exist = fileOperation.fileExists(chunkUpload.getFilePath());

        if (!exist) {
            // 未上传完，返回已上传的分片
            List<PartHash> partHashes = fileOperation.listParts(chunkUpload.getFilePath(), chunkUpload.getUploadId());
            result.setFinished(false).setPartHashes(partHashes);
        }
        return result;
    }

    @Override
    @CacheEvict(key = "#parentId")
    @Transactional
    public boolean saveCloudFileBySpliceVo(SpliceVo spliceVo, Long parentId, String fileName) {
        if (spliceVo == null) {
            return false;
        }

        Long userId = AuthUtil.getUserId();
        CloudFileDto cloudFileDto = new CloudFileDto()
                .setFileName(fileName)
                .setParentId(parentId)
                .setFileId(spliceVo.getFileId())
                .setDir(false);

        return saveCloudFile(cloudFileDto, userId);
    }

}
