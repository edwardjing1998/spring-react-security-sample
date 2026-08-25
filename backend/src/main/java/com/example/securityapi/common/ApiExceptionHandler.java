package com.example.securityapi.common;

import com.example.securityapi.auth.AuthService.EmailAlreadyUsedException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(EmailAlreadyUsedException.class)
    ResponseEntity<?> duplicateEmail() {
        return error(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED", "An account already uses this email.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<?> badCredentials() {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", 400);
        body.put("code", "VALIDATION_FAILED");
        body.put("message", "Please correct the highlighted fields.");
        body.put("fieldErrors", fields);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<?> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now(), "status", status.value(), "code", code, "message", message));
    }
}

