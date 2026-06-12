package dev.lucasschottler.api.Exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import dev.lucasschottler.database.queries.ErrorQueries;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final ErrorQueries errorQueries;

    public GlobalExceptionHandler(ErrorQueries errorQueries) {
        this.errorQueries = errorQueries;
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> handleAppException(AppException e) {
       
        errorQueries.addError(e.getBatchId(), e.getSku(), e.getFullContext());

        return ResponseEntity.status(500).body("An error occurred");
    }
}