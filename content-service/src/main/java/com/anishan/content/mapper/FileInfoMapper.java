package com.anishan.content.mapper;


import com.anishan.api.client.content.domain.LinkReferenceCount;
import com.anishan.content.domain.entity.FileInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * 文件信息表 Mapper 接口
 * </p>
 *
 * @author anishan
 * @since 2025-01-11
 */
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    List<FileInfo> selectByFileMd5AndFileSizeAndFileType(@Param("fileInfos") List<FileInfo> fileInfos);

    void updateReferenceByFilePath(List<LinkReferenceCount> rc);

    @Update("update file_info set reference = reference + #{count} where file_id = #{fileId}")
    void updateReferenceByFileId(@Param("fileId") Long fileId, @Param("count") Long count);
}
