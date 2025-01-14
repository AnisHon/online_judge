package com.anishan.content.service;

import com.anishan.content.domain.entity.FileInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 文件信息表 服务类
 * </p>
 *
 * @author anishan
 * @since 2025-01-11
 */
public interface FileInfoService extends IService<FileInfo> {


    FileInfo exists(FileInfo fileInfo);

}
