package com.anishan.content.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.anishan.api.client.content.domain.LinkReferenceCount;
import com.anishan.content.domain.entity.FileInfo;
import com.anishan.content.mapper.FileInfoMapper;
import com.anishan.content.service.FileInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 文件信息表 服务实现类
 * </p>
 *
 * @author anishan
 * @since 2025-01-11
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoService {




    private final FileInfoMapper fileInfoMapper;

    /**
     * 判断相同
     *
     * @param fileInfo 要保存的文件，需要计算好md5 size type
     * @return 返回找到的对象，没找到返回null
     */
    @Override
    public FileInfo exists(FileInfo fileInfo) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<FileInfo>()
                .select(FileInfo::getFileName, FileInfo::getFilePath)
                .eq(FileInfo::getFileMd5, fileInfo.getFileMd5())
                .eq(FileInfo::getFileType, fileInfo.getFileType())
                .eq(FileInfo::getFileSize, fileInfo.getFileSize());
        List<FileInfo> list = this.list(wrapper);

        return list.stream().findFirst().orElse(null);
    }

    @Override
    public void updateReference(Map<String, Long> map) {
        List<LinkReferenceCount> referenceCount = new ArrayList<>(map.size());
        map.forEach((k, v) -> {
            if (!k.startsWith("/api/")) {
                return;
            }
            String path = k.substring("/api/".length());
            referenceCount.add(
                    new LinkReferenceCount()
                            .setPath(path)
                            .setCount(v)
            );
        });


        if (CollUtil.isEmpty(referenceCount)) {
            return;
        }
        fileInfoMapper.updateReferenceByFilePath(referenceCount);


    }
}
