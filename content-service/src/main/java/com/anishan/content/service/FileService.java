package com.anishan.content.service;

import com.anishan.api.client.content.domain.OssFileInputStream;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

public interface FileService {

    String imagePath = "images/";

    default String getImagePath(String fileName) {
        return imagePath + fileName;
    }

    default String getAvatarPath(Long userId) {
        return "avatar/" + userId + "/default_avatar";
    }

    String uploadAvatar(MultipartFile avatar, Long userId);

    String uploadImage(MultipartFile image);

    OssFileInputStream getImage(@NotNull String path);
}
