package com.example.cloud.service.minio;

import com.example.cloud.exception.FileOperationErrorException;
import com.example.cloud.exception.ResourceDownloadException;
import com.example.cloud.model.dto.enums.ResourceType;
import com.example.cloud.model.dto.response.ResourceResponseDto;
import com.example.cloud.util.PathUtils;
import io.minio.*;
import io.minio.messages.Item;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import io.minio.errors.ErrorResponseException;
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
    private final MinioStorageService minioStorageService;

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
                throw new ResourceDownloadException("File download by path " + path + " failed");
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
                minioStorageService.putObject(fullPath, file.getInputStream(), file.getSize(), file.getContentType());
            } catch (Exception e) {
                throw new FileUploadException("Failed to upload file " + file.getOriginalFilename(), e);
            }

            responseDtoList.add(new ResourceResponseDto(
                    normalizedPath,
                    file.getOriginalFilename(),
                    file.getSize(),
                    ResourceType.FILE
            ));

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
                    ResourceType.FILE
            );
        } catch (Exception e) {
            throw new FileNotFoundException("File " + path + " not found: " + e.getMessage());
        }

    }


    public void deleteFile(Long userId, String path) {
        String fullPath = getUserFolder(userId) + path;
        try {
            minioStorageService.removeObject(fullPath);
        } catch (Exception e) {
            throw new FileOperationErrorException("Failed to delete file " + path);
        }
    }


    private InputStream downloadFile(Long userId, String path) {
        String fullPath = getUserFolder(userId) + path;
        return minioStorageService.getObject(fullPath);
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
            minioStorageService.copyObject(fromFullPath, toFullPath);

            deleteFile(userId, fromPath);
        }

    }

    @SneakyThrows
    public List<ResourceResponseDto> searchFiles(Long userId, String query) {
        String fullPath = getUserFolder(userId);
        try {
            Iterable<Result<Item>> results = minioStorageService.listObjects(fullPath, true);

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
                            ResourceType.FILE
                    ));
                }
            }
            return resources;
        } catch (Exception e) {
            throw new FileNotFoundException("Failed to search files: " + e.getMessage());
        }
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
