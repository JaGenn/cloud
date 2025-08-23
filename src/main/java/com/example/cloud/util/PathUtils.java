package com.example.cloud.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {

    public static String normalizePath(String rawPath) {
        Path trimmedPath = Paths.get(rawPath.trim()).normalize();
        String normalizedPath = trimmedPath.toString().replace("\\", "/");
        return normalizedPath.endsWith("/") ? normalizedPath : normalizedPath + "/";
    }

    public static void validatePath(String path) {
        if (path.equals("/") || path.isEmpty()) {
            return;
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path cannot be empty or null.");
        }
        if (path.contains("..") || path.contains("//")) {
            throw new IllegalArgumentException("Path contains illegal characters.");
        }
    }

    public static String extractFileName(String path) {
        String[] split = path.split("/");
        String fileName = split[split.length - 1];
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8);
    }

    public static String normalizeDirectoryPath(String fullPath) {
        String normalizedPath = fullPath.replace("//", "/");
        return normalizedPath.endsWith("/") ? normalizedPath : normalizedPath + "/";
    }


}
