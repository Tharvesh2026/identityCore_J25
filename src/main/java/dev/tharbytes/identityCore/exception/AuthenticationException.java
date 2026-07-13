package dev.tharbytes.identityCore.exception;

public class AuthenticationException extends ApplicationException {
    public AuthenticationException(String message) {
        super(message, 401);
    }
}
