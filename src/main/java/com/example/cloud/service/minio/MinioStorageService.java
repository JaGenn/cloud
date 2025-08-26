package com.example.cloud.service.minio;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final int PART_SIZE = -1;

    @Value("${minio.bucket}")
    private String bucketName;

    private final MinioClient minioClient;


    protected Iterable<Result<Item>> listObjects(String prefix, boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .recursive(recursive)
                        .build()
        );
    }

    @SneakyThrows
    protected void copyObject(String source, String destination) {
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(bucketName)
                        .object(destination)
                        .source(CopySource.builder()
                                .bucket(bucketName)
                                .object(source)
                                .build())
                        .build()
        );

    }

    @SneakyThrows
    protected void removeObject(String objectPath) {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectPath)
                        .build()
        );

    }

    @SneakyThrows
    protected InputStream getObject(String objectPath) {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectPath)
                        .build()
        );

    }

    @SneakyThrows
    protected void putObject(String objectPath, InputStream inputStream, long size, String contentType) {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectPath)
                        .stream(inputStream, size, PART_SIZE)
                        .contentType(contentType)
                        .build()
        );

    }


}
