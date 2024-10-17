package com.anishan.problem.controller;

import com.anishan.commons.entity.R;
import com.anishan.problem.domain.dto.FolderDto;
import com.anishan.problem.domain.vo.TreedFolder;
import com.anishan.problem.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/folder")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @GetMapping("/list-tree")
    public R<List<TreedFolder>> getTreedFolder() {
        List<TreedFolder> allFolders = folderService.getAllTreedFolders();
        return R.success(allFolders);
    }

    @PostMapping("/add")
    public R<Boolean> addFolder(@RequestBody FolderDto folderDto) {
        boolean b = folderService.addFolder(folderDto);
        return R.success(b);
    }






}
