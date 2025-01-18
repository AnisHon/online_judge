package com.anishan.api.file;

import com.anishan.api.client.content.domain.OSSFileInfo;
import com.anishan.api.client.content.domain.OssFileInputStream;

import java.io.InputStream;
import java.util.List;

public interface FileOperation {

    List<OSSFileInfo> listFiles(String folder);

    OssFileInputStream getFile(String path);

    void saveFile(String path, InputStream inputStream);

    void saveFile(String path, InputStream inputStream, String contentType);

    void saveFile(String path, InputStream inputStream, Long chunkSize);

    void deleteFile(String path);

    void deleteFiles(List<String> path);

    void downloadFile(String bucketName, String objectName, String path, boolean overwrite);

    void uploadFile(String bucketName, String objectName, String path, Long partSize);

    OSSFileInfo getFileInfo(String objectName);
}
