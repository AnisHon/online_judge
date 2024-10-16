package com.anishan.problem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.Folder;
import com.anishan.problem.service.FolderService;
import com.anishan.problem.mapper.FolderMapper;
import org.springframework.stereotype.Service;

/**
* @author happy
* @description 针对表【folder(文件夹表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
public class FolderServiceImpl extends ServiceImpl<FolderMapper, Folder>
    implements FolderService{

}




