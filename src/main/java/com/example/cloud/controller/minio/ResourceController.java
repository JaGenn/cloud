package com.example.cloud.controller.minio;


import com.example.cloud.model.dto.ResourceResponseDto;
import com.example.cloud.service.minio.ResourceService;
import com.example.cloud.util.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.example.cloud.util.UserContext.getCurrentUserId;


@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestParam String path, @RequestParam List<MultipartFile> files) {
        PathUtils.validatePath(path);
        Long userId = getCurrentUserId();
        List<ResourceResponseDto> responseInfo = resourceService.uploadFile(userId, path, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseInfo);
    }

    @GetMapping
    public ResponseEntity<?> getFileInfo(@RequestParam String path) {
        PathUtils.validatePath(path);
        Long userId = getCurrentUserId();
        ResourceResponseDto responseDto = resourceService.getFileInfo(userId, path);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteFile(@RequestParam String path) {
        PathUtils.validatePath(path);
        Long userId = getCurrentUserId();
        resourceService.deleteFile(userId, path);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam String path) {
        PathUtils.validatePath(path);
        Long userId = getCurrentUserId();
        InputStream inputStream = resourceService.downloadFile(userId, path);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + extractFileName(path) + "\"")
                .body(new InputStreamResource(inputStream));
    }

    @GetMapping("/move")
    public ResponseEntity<?> moveFile(@RequestParam String from, @RequestParam String to) {
        PathUtils.validatePath(from);
        PathUtils.validatePath(to);
        Long userId = getCurrentUserId();
        resourceService.moveFile(userId, from, to);
        ResourceResponseDto responseDto = resourceService.getFileInfo(userId, to);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResourceResponseDto>> searchFile(@RequestParam String query) {
        PathUtils.validatePath(query);
        if (query.endsWith("/")) {
            throw new IllegalArgumentException("File name mustn't end with '/'");
        }
        Long userId = getCurrentUserId();
        List<ResourceResponseDto> responseDto = resourceService.searchFiles(userId, query);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }

    private String extractFileName(String path) {
        String[] split = path.split("/");
        String fileName = split[split.length - 1];
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8);
    }

}
