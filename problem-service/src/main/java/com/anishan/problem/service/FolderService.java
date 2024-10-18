package com.anishan.problem.service;

import com.anishan.problem.domain.dto.FolderDto;
import com.anishan.problem.domain.entity.Folder;
import com.anishan.problem.domain.vo.FolderVo;
import com.anishan.problem.domain.vo.TreedFolder;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author happy
* @description 针对表【folder(文件夹表)】的数据库操作Service
* @createDate 2024-10-16 22:39:16
*/
public interface FolderService extends IService<Folder> {

    List<FolderVo> getAllFolders();

    List<TreedFolder> getAllTreedFolders();

    boolean existFolder(Long id);

    boolean existFolder(String name);

    boolean addFolder(FolderDto folderDto);

    boolean updateFolder(FolderDto folderDto);
}
