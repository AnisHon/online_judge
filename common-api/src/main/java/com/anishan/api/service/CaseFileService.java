package com.anishan.api.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用于存储OJ Case的service
 */
public interface CaseFileService {


    void save(MultipartFile[] cases);

    void remove(List<String> paths);
}
