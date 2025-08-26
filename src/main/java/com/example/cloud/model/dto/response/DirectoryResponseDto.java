package com.example.cloud.model.dto.response;

import com.example.cloud.model.dto.enums.ResourceType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Directory Dto")
public record DirectoryResponseDto (

        @Schema(description = "Path to target directory", example = "folder1/folder2/")
        String path,

        @Schema(description = "Target directory name", example = "folder3")
        String name,

        @Schema(description = "Type of resource", example = "DIRECTORY")
        ResourceType type

) {}
