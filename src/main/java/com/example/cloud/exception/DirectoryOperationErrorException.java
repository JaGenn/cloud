package com.example.cloud.exception;

public class DirectoryOperationErrorException extends RuntimeException {
    public DirectoryOperationErrorException(String message) {
        super(message);
    }
}
