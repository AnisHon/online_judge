package com.anishan.content.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import com.anishan.api.client.content.domain.OSSFileInfo;
import com.anishan.api.client.content.domain.OssFileInputStream;
import com.anishan.api.file.FileOperation;
import com.anishan.commons.domain.R;
import com.anishan.commons.exception.BusinessException;
import com.anishan.content.domain.dto.ChunkUploadDto;
import com.anishan.content.domain.dto.CloudFileDto;
import com.anishan.content.domain.dto.QueryCloudFile;
import com.anishan.content.domain.dto.SpliceChunk;
import com.anishan.content.domain.vo.CloudFilesVo;
import com.anishan.content.domain.vo.SpliceVo;
import com.anishan.content.service.ChunkUploadService;
import com.anishan.content.service.CloudFilesService;
import com.anishan.content.service.FileService;
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
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    @GetMapping("/file/progress/{md5}")
    @PreAuthorize("hasAuthority('content:file:add')")
    public R<SpliceVo> getFileProgress(@PathVariable String md5) {
        SpliceVo spliceVo = cloudFilesService.getSlice(md5);

        // 不为null就直接添加
        return R.success(spliceVo);
    }

    @ApiOperation("初始化分片上传文件")
    @PostMapping("/file/init")
    @PreAuthorize("hasAuthority('content:file:add')")
    public R<SpliceVo> initUpload(@Validated @RequestBody SpliceChunk spliceChunk) {
        SpliceVo spliceVo = cloudFilesService.initSpliceUpload(spliceChunk);
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
    @PreAuthorize("hasAuthority('content:file:add')")
    public R<Object> mergeFile(@PathVariable("md5") String md5) {
        ChunkUploadDto chunkUpload = chunkUploadService.getByMd5(md5);
        if (chunkUpload == null) {
            return R.badRequest("分片任务不存在");
        }
        cloudFilesService.merge(chunkUpload);
        return R.success(null);
    }


    @ApiOperation("添加文件")
    @PostMapping("/file/cloud/{md5}/{fileName}/{parentId}")
    @PreAuthorize("hasAuthority('content:file:add')")
    public R<Object> addCloudFile(@PathVariable String md5, @PathVariable String fileName, @PathVariable Long parentId) {
        SpliceVo spliceVo = cloudFilesService.getSlice(md5);
        try {
            return R.success(cloudFilesService.saveCloudFileBySpliceVo(spliceVo, parentId, fileName));
        }  catch (DuplicateKeyException e) {
            return R.badRequest("文件名重复");
        }
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

    @GetMapping("/file/download")
    public void download(String fileName, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (fileName == null) {
            return;
        }

        OutputStream os = null;
        OssFileInputStream stream = null;

        try {

            OSSFileInfo fileInfo = fileOperation.getFileInfo(fileName);

            // 分片下载
            long fSize = fileInfo.getSize();// 获取长度
            response.setContentType("application/octet-stream");
            String urlFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            response.addHeader("Content-Disposition", "attachment;filename=" + urlFileName);
            //根据前端传来的Range 判断支不支持分片下载
            response.setHeader("Accept-Range", "bytes");
            //文件大小
            response.setHeader("fSize", String.valueOf(fSize));
            //文件名称
            response.setHeader("fName", fileName);
            response.setCharacterEncoding("UTF-8");
            // 定义下载的开始和结束位置
            long startPos = 0;
            long lastPos = fSize - 1;
            //判断前端需不需要使用分片下载
            if (null != request.getHeader("Range")) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                String numRange = request.getHeader("Range").replaceAll("bytes=", "");
                System.out.println("请求头：" + request.getHeader("Range"));
                String[] strRange = numRange.split("-");
                if (strRange.length == 2) {
                    startPos = Long.parseLong(strRange[0].trim());
                    lastPos = Long.parseLong(strRange[1].trim());
                    // 若结束字节超出文件大小 取文件大小
                    if (lastPos >= fSize - 1) {
                        lastPos = fSize - 1;
                        System.out.println("请求头last："+ lastPos);
                    }
                } else {
                    // 若只给一个长度 开始位置一直到结束
                    startPos = Long.parseLong(numRange.replaceAll("-", "").trim());
                }
            }

            //要下载的长度
            long rangeLength = lastPos - startPos + 1;
            //组装断点下载基本信息
            String contentRange = "bytes" + startPos + "-" + lastPos + "/" + fSize;
            response.setHeader("Content-Range", contentRange);
            response.setHeader("Content-Length", String.valueOf(rangeLength));
            os = new BufferedOutputStream(response.getOutputStream());

            //minio上获取文件信息
            stream = fileOperation.getFile(fileName, startPos, rangeLength);

            os = new BufferedOutputStream(response.getOutputStream());

            //将读取的文件写入到OutputStream中
            byte[] buffer = new byte[1024];
            long bytesWritten = 0;
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                //已经读取的长度和本次读取的长度之和是否大于需要读取的长度（实质就是判断是否最后一行）
                if (bytesWritten + bytesRead > rangeLength) {
                    os.write(buffer, 0, (int) (rangeLength - bytesWritten));
                    break;
                } else {
                    os.write(buffer, 0, bytesRead);
                    bytesWritten += bytesRead;
                }
            }
            os.flush();
            response.flushBuffer();
        } finally {
            if (os != null) {
                os.close();
            }
            if(stream != null){
                stream.close();
            }
        }
    }


}
