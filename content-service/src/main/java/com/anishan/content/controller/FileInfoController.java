package com.anishan.content.controller;

import cn.hutool.core.collection.CollUtil;
import com.anishan.api.file.FileOperation;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.content.domain.entity.FileInfo;
import com.anishan.content.service.FileInfoService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public R<PagedResult<FileInfo>> list(PagedQuery<FileInfo> query) {
        Page<FileInfo> page = fileInfoService.page(query.page());
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
