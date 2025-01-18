package com.anishan.content.service.impl;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import com.anishan.api.client.content.domain.OssFileInputStream;
import com.anishan.content.domain.entity.FileInfo;
import com.anishan.api.file.FileOperation;
import com.anishan.content.service.FileInfoService;
import com.anishan.content.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FileServiceImpl implements FileService {

    private final FileInfoService fileInfoService;

    private final FileOperation fileOperation;

    private static final Set<String> imageTypes;

    private static final Digester md5 = new Digester(DigestAlgorithm.MD5);

    static {
        imageTypes = Set.of(  "jpg", "jpeg", "png", "xbm",
                "tif", "jfif", "ico", "tiff", "gif",
                "svg", "svgz", "webp", "bmp",
                "pjp", "apng", "pjpeg", "avif"
        );
    }


    @Override
    public String uploadAvatar(MultipartFile avatar, Long userId) {
        String filePath = getAvatarPath(userId);

        try {
            String type = FileTypeUtil.getType(avatar.getInputStream());

            // 不支持的格式直接不接受
            if (!imageTypes.contains(type)) {
                return null;
            }

            fileOperation.saveFile(filePath, avatar.getInputStream(), "image/" + type);
        } catch (IOException e) {
            filePath = null;
        }

        return filePath;
    }



    @Override
    @Transactional
    public String uploadImage(MultipartFile image) {
        String filePath;
        FileInfo fileInfo = new FileInfo();
        try {
            long size = image.getSize();
            String md5Hex = md5.digestHex(image.getInputStream());

            String type = FileTypeUtil.getType(image.getInputStream());

            fileInfo.setFileSize(size);
            fileInfo.setFileMd5(md5Hex);
            fileInfo.setFileType(type);

            // 不支持的格式直接不接受
            if (!imageTypes.contains(type)) {
                return null;
            }

            FileInfo exists = fileInfoService.exists(fileInfo);

            if (exists == null) {
                String fileName = IdUtil.fastSimpleUUID();
                fileInfo.setFileName(fileName);
                filePath = getImagePath(fileName);
                fileInfo.setFilePath(filePath);

                fileOperation.saveFile(filePath, image.getInputStream(), "image/" + type);

                fileInfoService.save(fileInfo);
            } else {
                filePath = exists.getFilePath();
            }

        } catch (IOException e) {
            filePath = null;
        }

        return filePath;

    }

    @Override
    public OssFileInputStream getImage(String fileName) {
        String imagePath = getImagePath(fileName);
        return fileOperation.getFile(imagePath);
    }
}
