package com.anishan.api.file.impl;

import cn.hutool.core.collection.CollUtil;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.anishan.api.client.content.domain.OSSFileInfo;
import com.anishan.api.client.content.domain.OssFileInputStream;
import com.anishan.api.domain.PartHash;
import com.anishan.api.file.FileOperation;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Getter
@RequiredArgsConstructor
public class MinioFileOperationImpl implements FileOperation {

    private final MinioClient minioClient;

    private final String bucketName;

    private final AmazonS3 s3Client;


    @Override
    public List<OSSFileInfo> listFiles(String folder) {
        List<OSSFileInfo> files = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs
                        .builder()
                        .bucket(bucketName)
                        .prefix(folder)
                        .build()
        );

        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                OSSFileInfo ossFileInfo = new OSSFileInfo()
                        .setHash(item.etag())
                        .setFilename(item.objectName())
                        .setOwnerId(item.owner().id())
                        .setOwnerName(item.owner().displayName())
                        .setDir(item.isDir())
                        .setLastModified(item.lastModified().toLocalDateTime())
                        .setSize(item.size())
                        .setStorageClass(item.storageClass())
                        .setLatest(item.isLatest())
                        .setVersionId(item.versionId())
                        .setMeta(item.userMetadata());
                files.add(ossFileInfo);
            }
        } catch (InternalException | NoSuchAlgorithmException | ServerException | InvalidKeyException | XmlParserException e) {
            log.error("Minio内部出现错误：",e);
        } catch (InsufficientDataException | IOException | InvalidResponseException | ErrorResponseException e) {
            throw new RuntimeException(e);
        }


        return files;
    }

    @Override
    public OssFileInputStream getFile(String path) {
        OssFileInputStream inputStream = null;
        try {
            GetObjectResponse stream = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(path).build());
            inputStream = new OssFileInputStream(stream, stream.headers(), stream.bucket(), stream.region(), stream.bucket());

        } catch (InternalException | NoSuchAlgorithmException | ServerException | InvalidKeyException | XmlParserException e) {
            log.error("Minio内部出现错误：",e);
        } catch (InsufficientDataException | IOException | InvalidResponseException | ErrorResponseException e) {
            throw new RuntimeException(e);
        }
        return inputStream;
    }

    @Override
    public void saveFile(String path, InputStream inputStream) {
        try {
            minioClient.putObject(
                    PutObjectArgs
                            .builder()
                            .bucket(bucketName)
                            .object(path)
                            .stream(inputStream, inputStream.available(), -1)
                            .build()
            );
        } catch (InternalException | NoSuchAlgorithmException | ServerException | InvalidKeyException | XmlParserException e) {
            log.error("Minio内部出现错误：",e);
        } catch (InsufficientDataException | IOException | InvalidResponseException | ErrorResponseException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void saveFile(String path, InputStream inputStream, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs
                            .builder()
                            .bucket(bucketName)
                            .object(path)
                            .stream(inputStream, inputStream.available(), -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (InternalException | NoSuchAlgorithmException | ServerException | InvalidKeyException | XmlParserException e) {
            log.error("Minio内部出现错误：",e);
        } catch (InsufficientDataException | IOException | InvalidResponseException | ErrorResponseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveFile(String path, InputStream inputStream, Long chunkSize) {
        try {
            minioClient.putObject(PutObjectArgs
                    .builder()
                    .bucket(bucketName)
                    .object(path)
                    .stream(inputStream, -1, chunkSize)
                    .build()
            );
        }catch (InternalException | NoSuchAlgorithmException | ServerException | InvalidKeyException | XmlParserException e) {
            log.error("Minio内部出现错误：",e);
        } catch (InsufficientDataException | IOException | InvalidResponseException | ErrorResponseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteFile(String path) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(path).build());
        } catch (InternalException | NoSuchAlgorithmException | ServerException | InvalidKeyException | XmlParserException e) {
            log.error("Minio内部出现错误：",e);
        } catch (InsufficientDataException | IOException | InvalidResponseException | ErrorResponseException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public void deleteFiles(List<String> path) {
        if (CollUtil.isEmpty(path)) {
            return;
        }

        List<DeleteObject> deleteObjects = path
                .stream()
                .map(DeleteObject::new)
                .collect(Collectors.toList());

        try {
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs
                            .builder()
                            .bucket(bucketName)
                            .objects(deleteObjects)
                            .build()
            );

            for (Result<DeleteError> result : results) {
                DeleteError deleteError = result.get();
                log.error("{} 删除失败, 错误码 {}, 原因 {}", deleteError.objectName(), deleteError.code(), deleteError.message());
            }

        } catch (InternalException | NoSuchAlgorithmException | ServerException | InvalidKeyException | XmlParserException e) {
            log.error("Minio内部出现错误：",e);
        } catch (InsufficientDataException | IOException | InvalidResponseException | ErrorResponseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void downloadFile(String bucketName, String objectName, String path, boolean overwrite) {
        try {
            minioClient.downloadObject(
                    DownloadObjectArgs
                            .builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .filename(path)
                            .overwrite(overwrite)
                            .build()
            );
        } catch (InternalException | NoSuchAlgorithmException | ServerException | InvalidKeyException | XmlParserException e) {
            log.error("Minio内部出现错误：",e);
        } catch (InsufficientDataException | IOException | InvalidResponseException | ErrorResponseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void uploadFile(String bucketName, String objectName, String path, Long partSize) {
        try {
            minioClient.uploadObject(UploadObjectArgs.builder().bucket(bucketName).object(objectName).filename(path, partSize).build());
        } catch (InternalException | NoSuchAlgorithmException | ServerException | InvalidKeyException | XmlParserException e) {
            log.error("Minio内部出现错误：",e);
        } catch (InsufficientDataException | IOException | InvalidResponseException | ErrorResponseException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public OSSFileInfo getFileInfo(String objectName) {

        OSSFileInfo ossFileInfo = null;
        try {
            StatObjectResponse response = minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());

            ossFileInfo = new OSSFileInfo()
                    .setHash(response.etag())
                    .setFilename(response.object())
                    .setDir(false)
                    .setLastModified(response.lastModified().toLocalDateTime())
                    .setVersionId(response.versionId())
                    .setSize(response.size())
                    .setMeta(response.userMetadata());
        } catch (InternalException | NoSuchAlgorithmException | ServerException | InvalidKeyException | XmlParserException e) {
            log.error("Minio内部出现错误：",e);
        } catch (InsufficientDataException | IOException | InvalidResponseException | ErrorResponseException e) {
            throw new RuntimeException(e);
        }
        return ossFileInfo;
    }

    @Override
    public String createMultipartUpload(String fileName, String contentType) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        InitiateMultipartUploadRequest initiateMultipartUploadRequest =
                new InitiateMultipartUploadRequest(bucketName, fileName)
                        .withObjectMetadata(objectMetadata);
        InitiateMultipartUploadResult initiateMultipartUploadResult =
                s3Client.initiateMultipartUpload(initiateMultipartUploadRequest);
        return initiateMultipartUploadResult.getUploadId();
    }

    @Override
    public String uploadPart(String path, String uploadId, InputStream inputStream, int chunkNum, long chunkSize) {
        UploadPartRequest uploadPartRequest = new UploadPartRequest()
                .withBucketName(bucketName)
                .withKey(path)
                .withUploadId(uploadId)
                .withPartNumber(chunkNum)
                .withInputStream(inputStream)
                .withPartSize(chunkSize);
        UploadPartResult uploadPartResult = s3Client.uploadPart(uploadPartRequest);
        return uploadPartResult.getETag();
    }

    @Override
    public void abortMultipartUpload(String path, String uploadId) {
        s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, path, uploadId));
    }

    @Override
    public String mergePart(String path, String uploadId, List<PartHash> partHashes) {
        List<PartETag> partETags = partHashes
                .stream()
                .map(x -> new PartETag(x.getChuckIndex(), x.getPartHash()))
                .collect(Collectors.toList());

        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucketName, path, uploadId, partETags);

        CompleteMultipartUploadResult completeMultipartUploadResult =
                s3Client.completeMultipartUpload(completeMultipartUploadRequest);
        return completeMultipartUploadResult.getETag();
    }

    @Override
    public List<PartHash> listParts(String path, String uploadId) {
        ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, path, uploadId);
        PartListing partListing = s3Client.listParts(listPartsRequest);
        List<PartSummary> parts = partListing.getParts();

        return parts
                .stream()
                .map(part -> new PartHash(part.getPartNumber(), part.getETag()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean fileExists(String filePath) {
        return s3Client.doesObjectExist(bucketName, filePath);
    }


}
