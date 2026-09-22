package com.anishan.content.controller;

import cn.hutool.core.collection.CollUtil;
import com.anishan.api.file.FileOperation;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.content.domain.entity.FileInfo;
import com.anishan.content.service.FileInfoService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/fileInfo")
@RequiredArgsConstructor
public class FileInfoController {


    private final FileInfoService fileInfoService;
    private final FileOperation fileOperation;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('content:file:list')")
    public R<PagedResult<FileInfo>> list(PagedQuery<FileInfo> query,
                                         @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String normalized = keyword.trim();
            wrapper.and(item -> item.like(FileInfo::getFileName, normalized)
                    .or().like(FileInfo::getFilePath, normalized)
                    .or().like(FileInfo::getFileMd5, normalized)
                    .or().like(FileInfo::getFileType, normalized));
        }
        Page<FileInfo> page = fileInfoService.page(query.page(), wrapper);
        return R.success(PagedResult.build(page));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('content:file:remove')")
    public R<Boolean> delete(@PathVariable List<Long> ids) {
        boolean b;
        if (CollUtil.isEmpty(ids)) {
            b = false;
        } else {
            b = fileInfoService.removeByIds(ids);
        }

        List<FileInfo> fileInfos = fileInfoService.listByIds(ids);
        List<String> paths = fileInfos.stream().map(FileInfo::getFilePath).collect(Collectors.toList());

        fileOperation.deleteFiles(paths);
        return R.success(b);
    }

}
