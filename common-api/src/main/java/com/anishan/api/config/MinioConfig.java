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
    public AmazonS3 s3Client () {
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
                new AwsClientBuilder.EndpointConfiguration(minioUrl, Regions.US_EAST_1.name());

        return AmazonS3ClientBuilder.standard()
                .withClientConfiguration(config)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(endPoint)
                .withPathStyleAccessEnabled(true).build();
    }

    @Bean
    @Autowired
    public FileOperation defaultFileOperation(MinioClient minioClient, AmazonS3 s3Client) {
        return new MinioFileOperationImpl(minioClient, bucketName, s3Client);
    }


}
