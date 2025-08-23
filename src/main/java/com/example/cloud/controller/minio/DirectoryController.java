package com.example.cloud.controller.minio;

import com.example.cloud.model.dto.DirectoryResponseDto;
import com.example.cloud.model.dto.ResourceResponseDto;
import com.example.cloud.service.minio.DirectoryService;
import com.example.cloud.util.PathUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


import static com.example.cloud.util.UserContext.getCurrentUserId;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/directory")
@Tag(name = "Directory Controller", description = "Directory management API")
public class DirectoryController {

    private final DirectoryService directoryService;

    @GetMapping
    @Operation(summary = "Get directory content by path")
    public ResponseEntity<List<ResourceResponseDto>> getDirectoryContent(@RequestParam String path) {
        PathUtils.validatePath(path);
        log.info("GET /api/directory/{}", path);
        Long userId = getCurrentUserId();
        List<ResourceResponseDto> responseDto = directoryService.getDirectoryContent(userId, path);
        log.info("Directory content by path {} was found successfully", path.isEmpty() ? "/" : path);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }


    @PostMapping
    @Operation(summary = "Create directory by path")
    public ResponseEntity<DirectoryResponseDto> createDirectory(@RequestParam String path) {
        PathUtils.validatePath(path);
        log.info("POST /api/directory/{}", path);
        Long userId = getCurrentUserId();
        DirectoryResponseDto result = directoryService.createDirectory(userId, path);
        log.info("Directory {} created successfully", path);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
