package com.anishan.content.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import com.anishan.api.client.content.domain.OssFileInputStream;
import com.anishan.commons.domain.R;
import com.anishan.commons.exception.BusinessException;
import com.anishan.content.domain.dto.CloudFileDto;
import com.anishan.content.domain.dto.QueryCloudFile;
import com.anishan.content.domain.dto.SpliceChunk;
import com.anishan.content.domain.dto.ChunkUploadDto;
import com.anishan.content.domain.entity.FileInfo;
import com.anishan.content.domain.vo.CloudFilesVo;
import com.anishan.api.file.FileOperation;
import com.anishan.content.domain.vo.SpliceVo;
import com.anishan.content.service.ChunkUploadService;
import com.anishan.content.service.FileService;
import com.anishan.content.service.CloudFilesService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping
@Api("文件上传下载API")
@RequiredArgsConstructor
public class FileController {


    private final FileService fileService;
    private final FileOperation fileOperation;
    private final CloudFilesService cloudFilesService;
    private final ChunkUploadService chunkUploadService;

    private void responseFile(String filePath, HttpServletResponse response) {
        try (OssFileInputStream is = fileOperation.getFile(filePath)) {


            String contentType = FileUtil.getMimeType(filePath);

            String filename = StrUtil.subAfter(filePath, "/", true);
            response.setContentType(contentType);
            response.addHeader("download-filename", filename);

            ServletOutputStream os = response.getOutputStream();

            IoUtil.copy(is, os);
        } catch (Exception ignore) {
            response.setStatus(HttpStatus.HTTP_NOT_FOUND);
        }
    }

    @ApiOperation("头像上传")
    @PostMapping("/avatar")
    public R<String> uploadAvatar(@RequestHeader("user-id") Long userId, @RequestParam("avatar") MultipartFile avatar) {
        if (avatar.isEmpty()) {
            return R.success(null);
        }

        String b = fileService.uploadAvatar(avatar, userId);

        return R.success(b);


    }


    @ApiOperation("获取头像")
    @GetMapping("/avatar/{userId}")
    public void getAvatar(@PathVariable Long userId, HttpServletResponse response) {
        String avatarPath = fileService.getAvatarPath(userId);
        responseFile(avatarPath, response);
    }


    @ApiOperation("上传图片")
    @PostMapping("/image")
    @Transactional
    public R<List<String>> uploadImage(@RequestParam("images") @Length(min = 1, max = 8) List<MultipartFile> images) {
        if (images.isEmpty()) {
            return R.success(null);
        }

        ArrayList<String> paths = new ArrayList<>(images.size());
        for (MultipartFile image : images) {
            // 这是url链接路径，不是OSS路径
            String path = fileService.uploadImage(image);
            paths.add(path);
        }

        return R.success(paths);
    }

    @ApiOperation("获取图片")
    @GetMapping("/image/**")
    public void getImage(HttpServletRequest req, @NotNull HttpServletResponse response) {
        String uri = StrUtil.subSuf(req.getRequestURI(), "/image/".length());
        responseFile(uri, response);
    }


    @ApiOperation("列出文件夹下的子文件夹")
    @GetMapping("/file/list")
    public R<List<CloudFilesVo>> listFiles(@Validated QueryCloudFile queryCloudFile) {
        List<CloudFilesVo> files = cloudFilesService.getCloudFilesVo(queryCloudFile);
        return R.success(files);
    }

    @ApiOperation("下载文件")
    @GetMapping("/file")
    public void getFile(@NotNull String path, HttpServletResponse response) {
        responseFile(path, response);
    }

    @PutMapping("/file")
    @ApiModelProperty("更新名称")
    @PreAuthorize("hasAuthority('content:file:edit')")
    public R<Object> rename(@RequestBody @Validated CloudFileDto cloudFileDto) {
        try {
            boolean rename = cloudFilesService.rename(cloudFileDto);
            return R.success(rename);
        } catch (DuplicateKeyException e) {
            return R.badRequest("文件(夹)名重复");
        }
    }

    @ApiModelProperty("查看是否存在，用于断点续传，秒传")
    @GetMapping("/file/progress/{md5}/{parentId}")
    @PreAuthorize("hasAuthority('content:file:add')")
    public R<SpliceVo> getFileProgress(@PathVariable String md5, @PathVariable Long parentId) {
        SpliceVo spliceVo = cloudFilesService.getSlice(md5);

        try {
            cloudFilesService.saveCloudFileBySpliceVo(spliceVo, parentId);
        }  catch (DuplicateKeyException e) {
            return R.badRequest("文件名重复");
        }

        // 不为null就直接添加
        return R.success(spliceVo);
    }

    @ApiOperation("初始化分片上传文件")
    @PostMapping("/file/init")
    @PreAuthorize("hasAuthority('content:file:add')")
    public R<SpliceVo> initUpload(@Validated @RequestBody SpliceChunk spliceChunk) {
        SpliceVo spliceVo = cloudFilesService.initSpliceUpload(spliceChunk);

        try {
            cloudFilesService.saveCloudFileBySpliceVo(spliceVo, spliceChunk.getParentId());
        }  catch (DuplicateKeyException e) {
            return R.badRequest("文件名重复");
        }
        return R.success(spliceVo);
    }

    @ApiOperation("分片上传文件")
    @PostMapping("/file/{md5}/{index}")
    @PreAuthorize("hasAuthority('content:file:add')")
    public R<Object> uploadFile(
            @RequestParam @NotNull MultipartFile file,
            @ApiParam("文件md5") @PathVariable String md5,
            @ApiParam("分片索引") @PathVariable Integer index

    ) {
        ChunkUploadDto chunkUpload = chunkUploadService.getByMd5(md5);
        if (chunkUpload == null) {
            return R.badRequest("分片不存在");
        }
        cloudFilesService.spliceUpload(file, md5, index, chunkUpload);
        return R.success(null);
    }

    @ApiOperation("合并文件")
    @PostMapping("/file/merge/{md5}")
    public R<Object> mergeFile(@PathVariable("md5") String md5) {
        ChunkUploadDto chunkUpload = chunkUploadService.getByMd5(md5);
        if (chunkUpload == null) {
            return R.badRequest("分片任务不存在");
        }
        cloudFilesService.merge(chunkUpload);
        return R.success(null);
    }

    @ApiOperation("添加文件夹")
    @PostMapping("/file/dir")
    @PreAuthorize("hasAuthority('content:file:add')")
    public R<Boolean> uploadDir(@RequestBody CloudFileDto cloudFileDto, @RequestHeader("user-id") Long userId) {
        cloudFileDto.setDir(true);
        try {
            boolean b = cloudFilesService.saveCloudFile(cloudFileDto, userId);
            return R.success(b);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("文件夹名重复");
        }
    }

    @ApiOperation("删除")
    @DeleteMapping("/file/{id}")
    @PreAuthorize("hasAuthority('content:file:remove')")
    @Transactional
    public R<Boolean> deleteFile(@PathVariable("id") Long id) {
        boolean b = cloudFilesService.removeCloudFile(id);
        return R.success(b);
    }



}
