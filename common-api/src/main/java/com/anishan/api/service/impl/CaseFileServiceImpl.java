package com.anishan.api.service.impl;

import com.anishan.api.file.FileOperation;
import com.anishan.api.service.CaseFileService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseFileServiceImpl implements CaseFileService {

    private final FileOperation fileOperation;

    @Override
    @SneakyThrows
    public void save(MultipartFile[] cases) {
        for (MultipartFile item : cases) {
            String path = item.getOriginalFilename();
            InputStream inputStream = item.getInputStream();
            fileOperation.saveFile(path, inputStream);
        }
    }

    @Override
    public void remove(List<String> paths) {
        for (String path : paths) {
            try {
                fileOperation.deleteFile(path);
            } catch (Exception e) {
                log.error("文件删除失败：{}", e.getMessage());
            }
        }
    }


}
