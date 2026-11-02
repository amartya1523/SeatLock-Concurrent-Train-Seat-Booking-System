package com.amartya.seatlock.exception;

import com.amartya.seatlock.api.dto.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BookingException.class)
    ResponseEntity<ApiError> booking(BookingException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(Instant.now(), ex.getStatus().value(), ex.getCode(), ex.getMessage(), null));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String,String> details = ex.getBindingResult().getFieldErrors().stream().collect(Collectors.toMap(e -> e.getField(), e -> Objects.requireNonNullElse(e.getDefaultMessage(), "Invalid value"), (a,b) -> a));
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "VALIDATION_FAILED", "Please check the submitted details.", details));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(Instant.now(), 409, "SEAT_UNAVAILABLE", "One or more seats were booked by another customer.", null));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex) {
        return ResponseEntity.internalServerError().body(new ApiError(Instant.now(), 500, "INTERNAL_ERROR", "An unexpected error occurred.", null));
    }
}
