package dev.tharbytes.identityCore.exception;

public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String message) {
        super(message, 404);
    }
}
