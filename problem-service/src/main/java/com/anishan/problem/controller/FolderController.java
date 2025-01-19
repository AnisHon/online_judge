package com.anishan.problem.controller;

import com.anishan.commons.enumeration.ValidationGroup;
import com.anishan.commons.domain.R;
import com.anishan.problem.domain.dto.FolderDto;
import com.anishan.problem.domain.vo.FolderVo;
import com.anishan.problem.domain.vo.TreedFolder;
import com.anishan.problem.service.FolderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/folder")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Api("文件夹操作接口")
@CacheConfig(cacheNames = "problem:folder")
public class FolderController {

    private final FolderService folderService;

    @GetMapping("/tree")
    @ApiOperation("获取树状Folder")
    @Cacheable(key = "'treedFolder'")
    public R<List<TreedFolder>> getTreedFolder() {
        List<TreedFolder> allFolders = folderService.getAllTreedFolders();
        return R.success(allFolders);
    }

    @PostMapping
    @ApiOperation("添加Folder")
    @PreAuthorize("hasAuthority('problem:folder:add')")
    @Caching(evict = {
            @CacheEvict(key = "'treedFolder'", allEntries = true),
            @CacheEvict(key = "'folder'", allEntries = true),
    })
    public R<Boolean> addFolder(@RequestBody @Validated(ValidationGroup.Insert.class) FolderDto folderDto) {
        boolean b = folderService.addFolder(folderDto);
        return R.success(b);
    }

    @PutMapping
    @ApiOperation("更改Folder")
    @PreAuthorize("hasAuthority('problem:folder:edit')")
    @Caching(evict = {
            @CacheEvict(key = "'treedFolder'", allEntries = true),
            @CacheEvict(key = "'folder'", allEntries = true),
    })
    public R<Boolean> updateFolder(@RequestBody @Validated(ValidationGroup.Update.class) FolderDto folderDto) {
        boolean b = folderService.updateFolder(folderDto);
        return R.success(b);
    }

    @GetMapping
    @ApiOperation("获取普通非树状Folder")
    @Cacheable(key = "'folder'")
    public R<List<FolderVo>> getFolders() {
        List<FolderVo> allFolders = folderService.getAllFolders();
        return R.success(allFolders);
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('problem:folder:remove')")
    @ApiOperation("删除folder")
    @Caching(evict = {
            @CacheEvict(key = "'treedFolder'", allEntries = true),
            @CacheEvict(key = "'folder'", allEntries = true),
    })
    public R<Boolean> batchDelFolder(@PathVariable List<Long> ids) {
        boolean b = folderService.removeByIds(ids);
        return R.success(b);
    }

}
