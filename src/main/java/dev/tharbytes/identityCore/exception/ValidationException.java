package dev.tharbytes.identityCore.exception;

public class ValidationException extends ApplicationException {
    public ValidationException(String message) {
        super(message, 400);
    }
}
