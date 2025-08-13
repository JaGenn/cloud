package com.example.cloud.service.minio;
import com.example.cloud.exception.DirectoryOperationErrorException;
import com.example.cloud.exception.FileOperationErrorException;
import com.example.cloud.model.dto.DirectoryResponseDto;
import com.example.cloud.model.dto.ResourceResponseDto;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static com.example.cloud.util.UserContext.getUserFolder;


@Service
@RequiredArgsConstructor
public class DirectoryService {

    @Value("${minio.bucket}")
    private String bucketName;

    private final MinioClient minioClient;

    @SneakyThrows
    public DirectoryResponseDto createDirectory(Long userId, String path) {
        String fullPath = getUserFolder(userId) + path;

        if (directoryAlreadyExists(fullPath)) {
            throw new DirectoryOperationErrorException("Directory '" + fullPath + "' already exists");
        }
        if (!fullPath.endsWith("/")) {
            fullPath += "/";
        }

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPath)
                        .stream(new java.io.ByteArrayInputStream(new byte[0]), 0, -1)
                        .contentType("application/octet-stream")
                        .build()
        );
        return getDirectoryInfo(userId, path);
    }

    @SneakyThrows
    public List<ResourceResponseDto> getDirectoryContent(Long userId, String path) {
        String fullPath = getUserFolder(userId) + path;
        if (!fullPath.endsWith("/")) {
            fullPath += "/";
        }
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullPath)
                        .recursive(false)
                        .build()
        );

        List<ResourceResponseDto> resources = new ArrayList<>();
        for (Result<Item> result : results) {
            Item item = result.get();
            String objectName = item.objectName().substring(getUserFolder(userId).length());
            String[] parts = objectName.split("/");
            String name = parts[parts.length - 1];
            String parentPath = objectName.substring(0, objectName.length() - (name.length() + 1));

            resources.add(new ResourceResponseDto(
                    parentPath.endsWith("/") ? parentPath : parentPath + "/",
                    name,
                    item.size(),
                    item.isDir() ? "DIRECTORY" : "FILE"
            ));
        }
        return resources;
    }

    @SneakyThrows
    public DirectoryResponseDto getDirectoryInfo(Long userId, String path) {
        String fullPath = getUserFolder(userId) + path;
        if (!fullPath.endsWith("/")) {
            fullPath += "/";
        }
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullPath)
                        .maxKeys(1)
                        .build()
        );

        if (results.iterator().hasNext()) {
            String[] parts = path.split("/");
            String name = parts.length > 0 ? parts[parts.length - 1] : path;
            String parentPath = path.substring(0, path.length() - (name.length() + 1));

            return new DirectoryResponseDto(
                    parentPath.endsWith("/") ? parentPath : parentPath + "/",
                    name,
                    "DIRECTORY"
            );
        } else {
            throw new FileNotFoundException("Directory " + path + " not found");
        }
    }

    @SneakyThrows
    public void moveDirectory(Long userId, String fromPath, String toPath) {
        String fromFullPath = getUserFolder(userId) + fromPath;
        String toFullPath = getUserFolder(userId) + toPath;

        if (directoryAlreadyExists(toFullPath)) {
            throw new DirectoryOperationErrorException("Directory '" + toPath + "' already exists");
        }

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fromFullPath)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            String objectName = item.objectName();
            String relativePath = objectName.substring(fromFullPath.length());

            String newObjectName = toFullPath + relativePath;

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(newObjectName)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(objectName)
                                    .build())
                            .build()
            );
        }

        deleteDirectory(userId, fromPath);
    }


    public void deleteDirectory(Long userId, String path) {
        String fullPath = getUserFolder(userId) + path;
        if (!fullPath.endsWith("/")) {
            fullPath += "/";
        }
        try {
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fullPath)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : objects) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(result.get().objectName())
                                .build()
                );
            }
        } catch (Exception e) {
            throw new FileOperationErrorException("Failed to delete directory " + path);
        }
    }

    @SneakyThrows
    private boolean directoryAlreadyExists(String path) {
        Iterable<Result<Item>> existing = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(path.endsWith("/") ? path : path + "/")
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : existing) {
            if (result.get() != null) {
                return true;
            }
        }
        return false;
    }
}
