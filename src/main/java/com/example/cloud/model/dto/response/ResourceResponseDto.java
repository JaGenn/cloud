package com.example.cloud.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "File Dto")
public record ResourceResponseDto (

        @Schema(description = "Path to target file", example = "folder1/folder2/")
        String path,

        @Schema(description = "Target file name", example = "myDocument.txt")
        String name,

        @Schema(description = "Size of file", example = "18MB")
        long size,

        @Schema(description = "Type of resource", example = "FILE")
        String type
) { }