package com.example.cloud.service.minio;
import com.example.cloud.exception.DirectoryOperationErrorException;
import com.example.cloud.model.dto.response.DirectoryResponseDto;
import com.example.cloud.model.dto.response.ResourceResponseDto;
import io.minio.*;
import io.minio.messages.Item;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.example.cloud.util.UserContext.getUserFolder;


@Service
@RequiredArgsConstructor
public class DirectoryService {

    @Value("${minio.bucket}")
    private String bucketName;

    private final MinioClient minioClient;

    @SneakyThrows
    public DirectoryResponseDto createDirectory(Long userId, String path) {
        String fullPath = normalizeDirectoryPath(getUserFolder(userId) + path);

        if (directoryAlreadyExists(fullPath)) {
            throw new DirectoryOperationErrorException("Directory '" + fullPath + "' already exists");
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
        String fullPath = normalizeDirectoryPath(getUserFolder(userId) + path);

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
            String parentPath = objectName.contains("/")
                    ? objectName.substring(0, objectName.length() - (name.length() + 1))
                    : "/";

            resources.add(new ResourceResponseDto(
                    parentPath.endsWith("/") ? parentPath : parentPath + "/",
                    item.isDir() ? name + "/" : name,
                    item.size(),
                    item.isDir() ? "DIRECTORY" : "FILE"
            ));
        }
        return resources;
    }

    @SneakyThrows
    public DirectoryResponseDto getDirectoryInfo(Long userId, String path) {
        String fullPath = normalizeDirectoryPath(getUserFolder(userId) + path);

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
            String parentPath = parts.length == 1 ? "/" : path.substring(0, path.length() - name.length());

            return new DirectoryResponseDto(
                    parentPath.endsWith("/") ? parentPath : parentPath + "/",
                    name,
                    "DIRECTORY"
            );
        } else {
            throw new DirectoryOperationErrorException("Directory " + path + " not found");
        }
    }

    @SneakyThrows
    public void moveDirectory(Long userId, String fromPath, String toPath) {
        String fromFullPath = normalizeDirectoryPath(getUserFolder(userId) + fromPath);
        String toFullPath = normalizeDirectoryPath(getUserFolder(userId) + toPath);

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
        String fullPath = normalizeDirectoryPath(getUserFolder(userId) + path);

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
            throw new DirectoryOperationErrorException("Failed to delete directory " + path);
        }
    }

    @SneakyThrows
    public void downloadDirectoryAsZip(Long userId, String path, HttpServletResponse response) {
        String fullPath = normalizeDirectoryPath(getUserFolder(userId) + path);

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fullPath)
                            .recursive(true) // Обходим все вложенные файлы
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.isDir()) {
                    continue; // Пропускаем пустые директории
                }

                String objectName = item.objectName();
                String relativeName = objectName.substring(fullPath.length()); // путь внутри ZIP

                // Получаем поток файла из MinIO
                try (InputStream is = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                )) {
                    // Добавляем в ZIP
                    zipOut.putNextEntry(new ZipEntry(relativeName));
                    is.transferTo(zipOut);
                    zipOut.closeEntry();
                }
            }
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

    private String normalizeDirectoryPath(String fullPath) {
        String normalizedPath = fullPath.replace("//", "/");
        return normalizedPath.endsWith("/") ? normalizedPath : normalizedPath + "/";
    }
}
