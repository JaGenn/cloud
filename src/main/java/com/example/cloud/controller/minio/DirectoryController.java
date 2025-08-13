package com.example.cloud.controller.minio;

import com.example.cloud.model.dto.DirectoryResponseDto;
import com.example.cloud.model.dto.ResourceResponseDto;
import com.example.cloud.service.minio.DirectoryService;
import com.example.cloud.util.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.cloud.util.UserContext.getCurrentUserId;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/directory")
public class DirectoryController {

    private final DirectoryService directoryService;

    @GetMapping
    public ResponseEntity<?> getDirectoryInfo(@RequestParam String path) {
        PathUtils.validatePath(path);
        Long userId = getCurrentUserId();
        List<ResourceResponseDto> responseDto = directoryService.getDirectoryContent(userId, path);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }


    @DeleteMapping
    public ResponseEntity<?> deleteDirectory(@RequestParam String path) {
        PathUtils.validatePath(path);
        Long userId = getCurrentUserId();
        directoryService.deleteDirectory(userId, path);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/move")
    public ResponseEntity<?> moveDirectory(@RequestParam String from, @RequestParam String to) {
        PathUtils.validatePath(from);
        PathUtils.validatePath(to);
        Long userId = getCurrentUserId();
        directoryService.moveDirectory(userId, from, to);
        List<ResourceResponseDto> responseDto = directoryService.getDirectoryContent(userId, to);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }

    @PostMapping
    public ResponseEntity<?> createDirectory(@RequestParam String path) {
        PathUtils.validatePath(path);
        Long userId = getCurrentUserId();
        DirectoryResponseDto result = directoryService.createDirectory(userId, path);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
