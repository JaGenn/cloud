package com.example.cloud.exception;

public class ResourceDownloadException extends RuntimeException {
    public ResourceDownloadException(String message) {
        super(message);
    }
}
