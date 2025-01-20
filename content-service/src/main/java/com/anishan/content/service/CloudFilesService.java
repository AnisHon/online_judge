package com.anishan.content.service;

import com.anishan.content.domain.dto.CloudFileDto;
import com.anishan.content.domain.dto.QueryCloudFile;
import com.anishan.content.domain.dto.SpliceChunk;
import com.anishan.content.domain.dto.ChunkUploadDto;
import com.anishan.content.domain.entity.CloudFiles;
import com.anishan.content.domain.vo.CloudFilesVo;
import com.anishan.content.domain.vo.SpliceVo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 网盘文件表 服务类
 * </p>
 *
 * @author anishan
 * @since 2025-01-14
 */
public interface CloudFilesService extends IService<CloudFiles> {

    List<CloudFilesVo> getCloudFilesVo(QueryCloudFile query);

    boolean saveCloudFile(CloudFileDto cloudFileDto, Long userId);

    boolean removeCloudFile(Long id);

    SpliceVo spliceUpload(MultipartFile file, String md5, Integer index, ChunkUploadDto chunkUpload);

    boolean rename(CloudFileDto cloudFileDto);

    SpliceVo initSpliceUpload(SpliceChunk spliceChunk);

    void merge(ChunkUploadDto md5);

    SpliceVo getSlice(String md5);

    boolean saveCloudFileBySpliceVo(SpliceVo spliceVo, Long parentId, String fileName);
}
