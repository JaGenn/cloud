package com.example.cloud.service.minio;

import com.example.cloud.exception.DirectoryOperationErrorException;
import com.example.cloud.exception.ResourceDownloadException;
import com.example.cloud.model.dto.enums.ResourceType;
import com.example.cloud.model.dto.response.DirectoryResponseDto;
import com.example.cloud.model.dto.response.ResourceResponseDto;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.example.cloud.util.PathUtils.normalizeDirectoryPath;
import static com.example.cloud.util.UserContext.getUserFolder;


@Service
@RequiredArgsConstructor
public class DirectoryService {

    @Value("${minio.bucket}")
    private String bucketName;

    private final MinioClient minioClient;
    private final MinioStorageService minioStorageService;

    @SneakyThrows
    public DirectoryResponseDto createDirectory(Long userId, String path) {
        String fullPath = normalizeDirectoryPath(getUserFolder(userId) + path);

        if (directoryAlreadyExists(fullPath)) {
            throw new DirectoryOperationErrorException("Directory '" + fullPath + "' already exists");
        }
        try {
            minioStorageService.putObject(fullPath, new ByteArrayInputStream(new byte[0]), 0,
                    MediaType.APPLICATION_OCTET_STREAM_VALUE);

            return getDirectoryInfo(userId, path);
        } catch (Exception e) {
            throw new FileUploadException("Create directory '" + path + "' was failed");
        }
    }


    public List<ResourceResponseDto> getDirectoryContent(Long userId, String path) {
        String fullPath = normalizeDirectoryPath(getUserFolder(userId) + path);
        try {
            Iterable<Result<Item>> results = minioStorageService.listObjects(fullPath, false);

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
                        item.isDir() ? ResourceType.DIRECTORY : ResourceType.FILE
                ));
            }
            return resources;
        } catch (Exception e) {
            throw new DirectoryOperationErrorException("Failed to get '" + fullPath + "' content");
        }
    }


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
                    ResourceType.DIRECTORY
            );
        } else {
            throw new DirectoryOperationErrorException("Directory " + path + " not found");
        }
    }

    public void moveDirectory(Long userId, String fromPath, String toPath) {
        String fromFullPath = normalizeDirectoryPath(getUserFolder(userId) + fromPath);
        String toFullPath = normalizeDirectoryPath(getUserFolder(userId) + toPath);

        if (directoryAlreadyExists(toFullPath)) {
            throw new DirectoryOperationErrorException("Directory '" + toPath + "' already exists");
        }
        try {
            Iterable<Result<Item>> results = minioStorageService.listObjects(fromFullPath, true);

            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();
                String relativePath = objectName.substring(fromFullPath.length());

                String newObjectName = toFullPath + relativePath;

                minioStorageService.copyObject(objectName, newObjectName);
            }
        } catch (MinioException | IOException | GeneralSecurityException e) {
            throw new DirectoryOperationErrorException("Failed to move directory '" + toFullPath + "' to '" + fromFullPath + "'");
        }

        deleteDirectory(userId, fromPath);
    }


    public void deleteDirectory(Long userId, String path) {
        String fullPath = normalizeDirectoryPath(getUserFolder(userId) + path);
        try {
            Iterable<Result<Item>> objects = minioStorageService.listObjects(fullPath, true);

            for (Result<Item> result : objects) {
                minioStorageService.removeObject(result.get().objectName());
            }

        } catch (Exception e) {
            throw new DirectoryOperationErrorException("Failed to delete directory " + path);
        }
    }


    public void downloadDirectoryAsZip(Long userId, String path, HttpServletResponse response) {
        String fullPath = normalizeDirectoryPath(getUserFolder(userId) + path);

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            Iterable<Result<Item>> results = minioStorageService.listObjects(fullPath, true);

            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.isDir()) {
                    continue;
                }

                String objectName = item.objectName();
                String relativeName = objectName.substring(fullPath.length());

                try (InputStream is = minioStorageService.getObject(objectName)) {
                    zipOut.putNextEntry(new ZipEntry(relativeName));
                    is.transferTo(zipOut);
                    zipOut.closeEntry();
                }
            }
        } catch (Exception e) {
            throw new ResourceDownloadException("Failed to download directory '" + fullPath + "'");
        }
    }

    @SneakyThrows
    private boolean directoryAlreadyExists(String path) {
        String normalizedPath = normalizeDirectoryPath(path);
        Iterable<Result<Item>> existing = minioStorageService.listObjects(normalizedPath, true);
        for (Result<Item> result : existing) {
            if (result.get() != null) {
                return true;
            }
        }
        return false;
    }

}
