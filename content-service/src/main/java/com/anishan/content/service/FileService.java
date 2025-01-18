package com.anishan.content.service;

import cn.hutool.core.date.DateUtil;
import com.anishan.api.client.content.domain.OssFileInputStream;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

public interface FileService {

    String imagePath = "images/";

    /**
     * images/年/月/日/fileName
     */
    default String getImagePath(String fileName) {
        String dateTime = DateUtil.format(DateUtil.date(), "yyyy/MM/dd/");
        return imagePath + dateTime + fileName;
    }

    default String getAvatarPath(Long userId) {
        return "avatar/" + userId + "/default_avatar";
    }

    String uploadAvatar(MultipartFile avatar, Long userId);

    String uploadImage(MultipartFile image);

    OssFileInputStream getImage(@NotNull String path);
}
