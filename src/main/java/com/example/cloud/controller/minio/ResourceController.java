package com.example.cloud.controller.minio;


import com.example.cloud.model.dto.response.ResourceResponseDto;
import com.example.cloud.service.minio.ResourceService;
import com.example.cloud.util.PathUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.example.cloud.util.UserContext.getCurrentUserId;

@Slf4j
@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
@Tag(name = "Resource Controller", description = "Resource management API")
public class ResourceController {

    private final ResourceService resourceService;


    @PostMapping
    @Operation(summary = "Upload file or files")
    public ResponseEntity<List<ResourceResponseDto>> uploadFile(@RequestParam("object") List<MultipartFile> file, @RequestParam String path) {
        log.info("POST /api/resource/{}", path);
        PathUtils.validatePath(path);
        Long userId = getCurrentUserId();
        List<ResourceResponseDto> responseInfo = resourceService.uploadFile(userId, path, file);
        log.info("Upload files by path {} was successful", path);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseInfo);
    }

    @GetMapping
    @Operation(summary = "Get file info")
    public ResponseEntity<ResourceResponseDto> getFileInfo(@RequestParam String path) {
        log.info("GET /api/resource/{}", path);
        PathUtils.validatePath(path);
        Long userId = getCurrentUserId();
        ResourceResponseDto responseDto = resourceService.getFileInfo(userId, path);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }

    @DeleteMapping
    @Operation(summary = "Delete file by path to this file")
    public ResponseEntity<Void> deleteFile(@RequestParam String path) {
        log.info("DELETE /api/resource/{}", path);
        PathUtils.validatePath(path);
        Long userId = getCurrentUserId();
        resourceService.deleteFile(userId, path);
        log.info("Delete files by path {} was successful", path);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/download")
    @Operation(summary = "Download file or folder as a zip archive")
    public ResponseEntity<Void> downloadFile(@RequestParam String path, HttpServletResponse response) {
        log.info("GET /api/resource/download {}", path);
        PathUtils.validatePath(path);
        Long userId = getCurrentUserId();
        resourceService.download(userId, path, response);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/move")
    @Operation(summary = "Move file from -> to")
    public ResponseEntity<ResourceResponseDto> moveFile(@RequestParam String from, @RequestParam String to) {
        log.info("MOVE /api/resource/move {} -> {}", from, to);
        PathUtils.validatePath(from);
        PathUtils.validatePath(to);
        Long userId = getCurrentUserId();
        resourceService.moveFile(userId, from, to);
        ResourceResponseDto responseDto = resourceService.getFileInfo(userId, to);
        log.info("File was moved from {} to {}", from, to);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }

    @GetMapping("/search")
    @Operation(summary = "Search file by his name")
    public ResponseEntity<List<ResourceResponseDto>> searchFile(@RequestParam String query) {
        log.info("GET /api/resource/search {}", query);
        PathUtils.validatePath(query);
        Long userId = getCurrentUserId();
        List<ResourceResponseDto> responseDto = resourceService.searchFiles(userId, query);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }


}
