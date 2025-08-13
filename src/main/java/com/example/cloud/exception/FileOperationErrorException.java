package com.example.cloud.exception;

public class FileOperationErrorException extends RuntimeException {
    public FileOperationErrorException(String message) {
        super(message);
    }
}
