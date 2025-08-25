package com.example.cloud.service.minio;

import com.example.cloud.exception.FileOperationErrorException;
import com.example.cloud.model.dto.response.ResourceResponseDto;
import com.example.cloud.util.PathUtils;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.List;

import static com.example.cloud.util.PathUtils.extractFileName;
import static com.example.cloud.util.UserContext.getUserFolder;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    @Value("${minio.bucket}")
    private String bucketName;

    private final MinioClient minioClient;
    private final DirectoryService directoryService;

    public void download(Long userId, String path, HttpServletResponse response) {

        if (path.endsWith("/")) {

            response.setContentType("application/zip");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + extractFileName(path) + ".zip\"");

            directoryService.downloadDirectoryAsZip(userId, path, response);
            log.info("Directory by path {} was downloaded as zip", path);

        } else {

            try (InputStream inputStream = downloadFile(userId, path)) {
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + extractFileName(path) + "\"");

                inputStream.transferTo(response.getOutputStream());
                log.info("File {} was downloaded successful", path);

            } catch (IOException e) {
                throw new FileOperationErrorException("File download failed");
            }
        }
    }

    @SneakyThrows
    public List<ResourceResponseDto> uploadFile(Long userId, String path, List<MultipartFile> files) {

        if (files.isEmpty()) {
            throw new IllegalArgumentException("Files list is empty");
        }

        List<ResourceResponseDto> responseDtoList = new ArrayList<>();

        for (MultipartFile file : files) {

            if (file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {
                continue;
            }

            String normalizedPath = PathUtils.normalizePath(path);
            String fullPath = getUserFolder(userId) + normalizedPath + file.getOriginalFilename();

            if (fileAlreadyExists(fullPath)) {
                throw new FileAlreadyExistsException("File " + file.getOriginalFilename() + " already exists");
            }

            try {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fullPath)
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );

                responseDtoList.add(new ResourceResponseDto(
                        normalizedPath,
                        file.getOriginalFilename(),
                        file.getSize(),
                        "FILE"
                ));
            } catch (Exception e) {
                throw new FileUploadException("Failed to upload file" + file.getOriginalFilename());
            }
        }
        return responseDtoList;
    }

    @SneakyThrows
    public ResourceResponseDto getFileInfo(Long userId, String path) {
        String fullPath = getUserFolder(userId) + path;

        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .build()
            );

            String[] parts = path.split("/");
            String name = parts.length > 0 ? parts[parts.length - 1] : path;
            String parentPath = path.substring(0, path.length() - name.length());


            return new ResourceResponseDto(
                    parentPath,
                    name,
                    stat.size(),
                    "FILE"
            );
        } catch (Exception e) {
            throw new FileNotFoundException("File " + path + " not found: " + e.getMessage());
        }

    }


    @SneakyThrows
    public void deleteFile(Long userId, String path) {
        String fullPath = getUserFolder(userId) + path;
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .build()
            );
        } catch (Exception e) {
            throw new FileOperationErrorException("Failed to delete file " + path);
        }
    }


    private InputStream downloadFile(Long userId, String path) {
        String fullPath = getUserFolder(userId) + path;

        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fullPath)
                    .build());
        } catch (Exception e) {
            throw new FileOperationErrorException("Failed to download file " + path);
        }
    }


    @SneakyThrows
    public void moveFile(Long userId, String fromPath, String toPath) {
        String fromFullPath = getUserFolder(userId) + fromPath;
        String toFullPath = getUserFolder(userId) + toPath;

        if (fileAlreadyExists(toFullPath)) {
            throw new FileAlreadyExistsException("File with name " + toPath + " already exists in this directory");
        }

        if (fromFullPath.endsWith("/")) {
            directoryService.moveDirectory(userId, fromPath, toPath);
        } else {

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(toFullPath)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(fromFullPath)
                                    .build())
                            .build()
            );

            deleteFile(userId, fromPath);
        }

    }



    @SneakyThrows
    public List<ResourceResponseDto> searchFiles(Long userId, String query) {
        String fullPath = getUserFolder(userId);
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullPath)
                        .recursive(true)
                        .build()
        );

        List<ResourceResponseDto> resources = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (Result<Item> result : results) {
            Item item = result.get();
            String rawName = item.objectName().substring(fullPath.length());
            String decodedName = URLDecoder.decode(rawName, StandardCharsets.UTF_8);

            if (decodedName.toLowerCase().contains(lowerQuery)) {
                String[] parts = decodedName.split("/");
                String name = parts[parts.length - 1];
                String parentPath = decodedName.substring(0, decodedName.length() - name.length());

                resources.add(new ResourceResponseDto(
                        parentPath,
                        name,
                        item.size(),
                        "FILE"
                ));
            }
        }
        return resources;
    }

    @SneakyThrows
    private boolean fileAlreadyExists(String path) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw e;
        }
    }
}
