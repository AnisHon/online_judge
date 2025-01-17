package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.problem.domain.dto.FolderDto;
import com.anishan.problem.domain.entity.Folder;
import com.anishan.problem.domain.vo.FolderVo;
import com.anishan.problem.domain.vo.TreedFolder;
import com.anishan.problem.mapper.FolderMapper;
import com.anishan.problem.service.FolderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
* @author happy
* @description 针对表【folder(文件夹表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
public class FolderServiceImpl extends ServiceImpl<FolderMapper, Folder>
    implements FolderService{

    @Override
    public List<FolderVo> getAllFolders() {
        List<Folder> list = this.list();
        return BeanUtil.copyToList(list, FolderVo.class);
    }

    public List<FolderVo> getHeadFolders() {
        List<Folder> list = this.list(
                new LambdaQueryWrapper<Folder>()
                        .eq(Folder::getParentId, 0)
        );
        return BeanUtil.copyToList(list, FolderVo.class);
    }

    public void doBuildTreeRecursion(Set<TreedFolder> folders, TreedFolder parent) {
        if(parent == null || parent.isFile()) {
            return;
        }

        List<TreedFolder> child = folders
                .stream()
                .filter(f -> Objects.equals(parent.getFolder().getFolderId(), f.getFolder().getParentId()))
                .collect(Collectors.toList());

        parent.setChildren(child);
        child.forEach(folders::remove);

        for (TreedFolder folder : child) {
            doBuildTreeRecursion(folders, folder);
        }

    }

    public Set<TreedFolder> buildTree(Set<FolderVo> folders, Set<FolderVo> headers) {
        folders.removeAll(headers);
        Set<TreedFolder> trees = folders.stream().map(TreedFolder::new).collect(Collectors.toSet());
        Set<TreedFolder> treedHeader = headers.stream().map(TreedFolder::new).collect(Collectors.toSet());

        for (TreedFolder header : treedHeader) {
            doBuildTreeRecursion(trees, header);
        }

        return treedHeader;
    }

    @Override
    public List<TreedFolder> getAllTreedFolders() {
        Set<FolderVo> allFolders = new HashSet<>(getAllFolders());
        Set<FolderVo> headFolders = new HashSet<>(getHeadFolders());
        Set<TreedFolder> treeHeader = buildTree(allFolders, headFolders);
        return new ArrayList<>(treeHeader);

    }

    @Override
    public boolean existFolder(Long id) {
        return this.exists(new LambdaQueryWrapper<Folder>()
                .eq(Folder::getFolderId, id)
        );
    }

    @Override
    public boolean existFolder(String name) {
        return this.exists(new LambdaQueryWrapper<Folder>()
                .eq(Folder::getFolderName, name)
        );
    }

    @Override
    public boolean addFolder(FolderDto folderDto) {
        Folder folder = BeanUtil.copyProperties(folderDto, Folder.class, "folderId");
        return this.save(folder);
    }


    @Override
    public boolean updateFolder(FolderDto folderDto) {
        Folder folder = this.getById(folderDto.getFolderId());
        BeanUtil.copyProperties(folderDto, folder);
        return this.updateById(folder);
    }


}




