package com.vighnesh.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionNotFound(
            TransactionNotFoundException exception,
            HttpServletRequest request) {

        log.warn("Transaction not found: {}", exception.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AnalysisRunNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAnalysisRunNotFound(
            AnalysisRunNotFoundException exception,
            HttpServletRequest request) {

        log.warn("Analysis run not found: {}", exception.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {

        log.warn("Bad request parameter: {}", exception.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MethodArgumentNotValidException.class
    })
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            Exception exception,
            HttpServletRequest request) {

        log.warn("Request validation failed: {}", exception.getMessage());
        String message = "Invalid request parameter or missing parameter";
        if (exception instanceof org.springframework.web.bind.MethodArgumentNotValidException manv &&
                manv.getBindingResult().getFieldError() != null) {
            message = manv.getBindingResult().getFieldError().getDefaultMessage();
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", message, request.getRequestURI());
    }


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request) {

        log.warn("File upload limit exceeded: {}", exception.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", "Uploaded file size exceeds maximum limit (50MB)", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception exception,
            HttpServletRequest request) {

        log.error("Unhandled internal server error on endpoint {}", request.getRequestURI(), exception);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected server error occurred. Please contact system administrator.",
                request.getRequestURI()
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            String path) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        response.put("path", path);

        return ResponseEntity.status(status).body(response);
    }
}
