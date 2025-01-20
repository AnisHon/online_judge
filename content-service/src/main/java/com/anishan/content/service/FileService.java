package com.anishan.content.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.web.multipart.MultipartFile;



public interface FileService {

    String imagePath = "images/";

    /**
     * images/年/月/日/fileName
     */
    default String getImagePath(String fileName, String suffix) {
        String dateTime = DateUtil.format(DateUtil.date(), "yyyy/MM/dd/");
        return StrUtil.format("{}{}{}.{}", imagePath, dateTime, fileName, suffix);
    }

    default String getAvatarPath(Long userId) {
        return "avatar/" + userId + "/default_avatar";
    }

    String uploadAvatar(MultipartFile avatar, Long userId);

    String uploadImage(MultipartFile image);

}
