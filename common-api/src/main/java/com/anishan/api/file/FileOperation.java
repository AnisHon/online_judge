package com.anishan.api.file;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.anishan.api.client.content.domain.OSSFileInfo;
import com.anishan.api.client.content.domain.OssFileInputStream;
import com.anishan.api.domain.PartHash;

import java.io.InputStream;
import java.util.List;

public interface FileOperation {

    /**
     * 分块大小 10mb
     */
    long chunkSize = 10 * 1024 * 1024;

    /**
     * 分配一个路径
     */
    static String assignPath(String basePath) {
        String format = DateUtil.format(DateUtil.date(), "/yyyy/MM/dd/");
        return basePath + format + IdUtil.fastSimpleUUID();

    }

    static String assignPath(String basePath, String suffix) {
        return StrUtil.format("{}.{}", assignPath(basePath), suffix);

    }

    List<OSSFileInfo> listFiles(String folder);

    OssFileInputStream getFile(String path);

    OssFileInputStream getFile(String path, Long offset, Long length);

    void saveFile(String path, InputStream inputStream);

    void saveFile(String path, InputStream inputStream, String contentType);

    void saveFile(String path, InputStream inputStream, Long chunkSize);

    void deleteFile(String path);

    void deleteFiles(List<String> path);

    void downloadFile(String bucketName, String objectName, String path, boolean overwrite);

    void uploadFile(String bucketName, String objectName, String path, Long partSize);

    OSSFileInfo getFileInfo(String objectName);

    String createMultipartUpload(String fileName, String contentType);

    String uploadPart(String path, String uploadId, InputStream inputStream, int chunkNum, long chunkSize);

    void abortMultipartUpload(String path, String uploadId);

    String mergePart(String path, String uploadId, List<PartHash> partHashes);

    List<PartHash> listParts(String path, String uploadId);

    boolean fileExists(String filePath);
}
