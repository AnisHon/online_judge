package com.anishan.content.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import com.anishan.api.client.content.domain.OssFileInputStream;
import com.anishan.commons.domain.R;
import com.anishan.content.domain.dto.CloudFileDto;
import com.anishan.content.domain.dto.QueryCloudFile;
import com.anishan.content.domain.vo.CloudFilesVo;
import com.anishan.api.file.FileOperation;
import com.anishan.content.service.FileService;
import com.anishan.content.service.CloudFilesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
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

    private void responseFile(String filePath, HttpServletResponse response) {
        try (OssFileInputStream is = fileOperation.getFile(filePath)) {
            // content-type
            String contentType = is.getHeaders().get("Content-Type");
            response.setContentType(contentType);

            ServletOutputStream os = response.getOutputStream();

            IoUtil.copy(is, os);
        } catch (Exception ignore) {
            response.setStatus(HttpStatus.HTTP_NOT_FOUND);
        }
    }

    @ApiOperation("头像上传")
    @PostMapping("/avatar")
    @Transactional
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
        log.debug(uri);
        responseFile(uri, response);
    }


    @ApiOperation("列出文件夹下的子文件夹")
    @GetMapping("/file/list")
    public R<List<CloudFilesVo>> listFiles(@Validated QueryCloudFile queryCloudFile) {
        List<CloudFilesVo> files = cloudFilesService.getCloudFilesVo(queryCloudFile);
        return R.success(files);
    }

    @ApiOperation("下载文件")
    @GetMapping("/file/{path}")
    public void getFile(@NotNull @PathVariable("path") String path, HttpServletResponse response) {
        responseFile(path, response);
    }

    @ApiOperation("上传文件")
    @PostMapping("/file")
    @PreAuthorize("hasAuthority('content:file:add')")
    public void uploadFile(@RequestParam("file") List<MultipartFile> files, Long parentId, HttpServletResponse response) {

    }

    @ApiOperation("添加文件夹")
    @PostMapping("/file/dir")
    @PreAuthorize("hasAuthority('content:file:add')")
    public R<Boolean> uploadDir(@RequestBody CloudFileDto cloudFileDto, @RequestHeader("user-id") Long userId) {
        cloudFileDto.setDir(true);
        boolean b = cloudFilesService.saveCloudFile(cloudFileDto, userId);
        return R.success(b);
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
