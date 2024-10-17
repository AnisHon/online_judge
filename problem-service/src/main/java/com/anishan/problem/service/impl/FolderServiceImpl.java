package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.problem.domain.dto.FolderDto;
import com.anishan.problem.domain.vo.FolderVo;
import com.anishan.problem.domain.vo.TreedFolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.Folder;
import com.anishan.problem.service.FolderService;
import com.anishan.problem.mapper.FolderMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        if(parent == null || !parent.isFolder()) {
            return;
        }

        List<TreedFolder> child = folders.stream().filter(parent::isParent).collect(Collectors.toList());

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

        return trees;
    }

    @Override
    public List<TreedFolder> getAllTreedFolders() {
        Set<FolderVo> allFolders = new HashSet<>(getAllFolders());
        Set<FolderVo> headFolders = new HashSet<>(getHeadFolders());
        Set<TreedFolder> treeHeader = buildTree(allFolders, headFolders);
        return new ArrayList<>(treeHeader);

    }


    void doAddCheck() {

    }

    @Override
    public boolean addFolder(FolderDto folderDto) {
        Folder folder = BeanUtil.copyProperties(folderDto, Folder.class);
        return this.save(folder);
    }


}




