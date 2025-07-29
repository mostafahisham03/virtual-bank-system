package com.vbank.transaction_service.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() {
        super("Resource not found.");
    }
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
