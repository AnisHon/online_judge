package com.anishan.content.service;

import com.anishan.content.domain.dto.CloudFileDto;
import com.anishan.content.domain.dto.QueryCloudFile;
import com.anishan.content.domain.entity.CloudFiles;
import com.anishan.content.domain.vo.CloudFilesVo;
import com.baomidou.mybatisplus.extension.service.IService;

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
}
