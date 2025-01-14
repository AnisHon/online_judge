package com.anishan.content.config;

import com.anishan.content.file.FileOperation;
import com.anishan.content.file.impl.MinioFileOperationImpl;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class MinioConfig {
    @Value("${oj.minio.url}")
    private String minioUrl;

    @Value("${oj.minio.access-key}")
    private String accessKey;

    @Value("${oj.minio.secret-key}")
    private String secretKey;

    @Value("${oj.minio.bucket-name}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        MinioClient client = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();

        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
        return client;
    }

    @Bean
    public FileOperation defaultFileOperation(@Autowired MinioClient minioClient) {
        return new MinioFileOperationImpl(minioClient, bucketName);
    }



}
