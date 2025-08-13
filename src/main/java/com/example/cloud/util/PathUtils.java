package com.example.cloud.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {

    public static String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("Path cannot be empty or null.");
        }
        Path trimmedPath = Paths.get(rawPath.trim()).normalize();
        String normalizedPath = trimmedPath.toString().replace("\\", "/");
        return normalizedPath.endsWith("/") ? normalizedPath : normalizedPath + "/";
    }

    public static void validatePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path cannot be empty or null.");
        }
        if (path.startsWith("/")) {
            throw new IllegalArgumentException("Path cannot start with '/'.");
        }
        if (path.contains("..") || path.contains("//")) {
            throw new IllegalArgumentException("Path contains illegal characters.");
        }
    }

    public static boolean isDirectory(String path) {
        return path.endsWith("/");
    }

}
