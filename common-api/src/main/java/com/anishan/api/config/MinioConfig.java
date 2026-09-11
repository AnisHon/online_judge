package com.anishan.api.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.anishan.api.file.FileOperation;
import com.anishan.api.file.impl.MinioFileOperationImpl;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MinioConfig {

    private final MinioConfigProperties minio;

    @Bean
    public MinioClient minioClient() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        final String url = minio.getUrl();

        final String accessKey = minio.getAccessKey();

        final String secretKey = minio.getSecretKey();

        final String bucketName = minio.getBucketName();

        log.info("minioUrl: {}", url);
        log.info("bucketName: {}", bucketName);

        MinioClient client = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();

        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
        return client;
    }



    @Bean
    public AmazonS3 s3Client () {

        final String url = minio.getUrl();

        final String accessKey = minio.getAccessKey();

        final String secretKey = minio.getSecretKey();

        final String bucketName = minio.getBucketName();

        log.info("s3 Url: {}", url);
        log.info("s3 bucketName: {}", bucketName);

        //设置连接时的参数
        ClientConfiguration config = new ClientConfiguration();
        //设置连接方式为HTTP，可选参数为HTTP和HTTPS
        config.setProtocol(Protocol.HTTP);
        //设置网络访问超时时间
        config.setConnectionTimeout(5000);
        config.setUseExpectContinue(true);
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        //设置Endpoint
        AwsClientBuilder.EndpointConfiguration endPoint =
                new AwsClientBuilder.EndpointConfiguration(url, Regions.US_EAST_1.name());

        return AmazonS3ClientBuilder.standard()
                .withClientConfiguration(config)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(endPoint)
                .withPathStyleAccessEnabled(true).build();
    }

    @Bean
    @Autowired
    public FileOperation defaultFileOperation(MinioClient minioClient, AmazonS3 s3Client) {
        final String bucketName = minio.getBucketName();
        return new MinioFileOperationImpl(minioClient, bucketName, s3Client);
    }


}
