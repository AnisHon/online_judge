package com.anishan.content.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.commons.exception.BusinessException;
import com.anishan.content.domain.dto.CloudFileDto;
import com.anishan.content.domain.dto.QueryCloudFile;
import com.anishan.content.domain.entity.CloudFiles;
import com.anishan.content.domain.entity.FileInfo;
import com.anishan.content.domain.vo.CloudFilesVo;
import com.anishan.content.mapper.CloudFilesMapper;
import com.anishan.content.service.CloudFilesService;
import com.anishan.content.service.FileInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
public class CloudFilesServiceImpl extends ServiceImpl<CloudFilesMapper, CloudFiles> implements CloudFilesService {

    private final CloudFilesMapper cloudFilesMapper;

    private final UserInternalClient userClient;

    @Override
    public List<CloudFilesVo> getCloudFilesVo(QueryCloudFile query) {

        MPJLambdaWrapper<CloudFiles> wrapper = new MPJLambdaWrapper<CloudFiles>()
                .selectAll(CloudFiles.class)
                .select(FileInfo::getFileMd5, FileInfo::getFilePath, FileInfo::getFileSize)
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
    public boolean saveCloudFile(CloudFileDto cloudFileDto, Long userId) {

        CloudFiles cloudFiles = new CloudFiles()
                .setFileName(cloudFileDto.getFileName())
                .setParentId(cloudFileDto.getParentId())
                .setFileId(cloudFileDto.getFileId())
                .setUserId(userId)
                .setDir(cloudFileDto.getDir());

        return this.save(cloudFiles);
    }

    @Override
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

        // 文件夹且里面有东西不能删
        if (cloudFiles.getDir() && exists) {
           throw new BusinessException("文件夹不是空的");
        }


        return this.removeById(id);
    }
}
