package dev.tharbytes.identityCore.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import dev.tharbytes.identityCore.dto.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<?>> handleApp(ApplicationException ex) {
        return ResponseEntity
            .status(ex.getStatusCode())
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneral(Exception ex) {
        return ResponseEntity
            .status(500)
            .body(ApiResponse.error("An unexpected error occurred."));
    }
}
