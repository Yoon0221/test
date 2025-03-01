package com.example.demo.base.code.exception;

import com.example.demo.base.ApiResponse;
import com.example.demo.base.status.ErrorStatus;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class CommonException {

    // 1. ConstraintViolationException 처리
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage()
                ));
        log.error("Validation Error: {}", errors);
        return CustomException.createErrorResponse(ErrorStatus.COMMON_BAD_REQUEST, errors);
    }

    // 2. MethodArgumentNotValidException 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage()
                ));
        log.error("Validation Error: {}", errors);
        return CustomException.createErrorResponse(ErrorStatus.COMMON_BAD_REQUEST, errors);
    }

    // 3. 잘못된 요청값 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();

        // 잘못된 Enum 값 처리
        if (cause instanceof InvalidFormatException) {
            InvalidFormatException invalidFormatException = (InvalidFormatException) cause;
            if (invalidFormatException.getTargetType().isEnum()) {
                return CustomException.createErrorResponse(ErrorStatus.USER_INVALID_PROVIDER, null);
            }
        }

        return CustomException.createErrorResponse(ErrorStatus.COMMON_BAD_REQUEST, null);
    }

    // 4. CustomException 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException(CustomException ex) {
        // 예외 발생 시 로그 출력 (로그 메시지 형식 맞추기)
        log.error("Custom Error: {} - Code: {} - Status: {}",
                ex.getMessage(),
                ex.getClass().getSimpleName(),
                ex.getErrorStatus().getHttpStatus());

        // CustomException이 발생했을 때, 해당 ErrorStatus에 맞는 코드 및 메시지를 반환하도록 처리
        return CustomException.createErrorResponse(ex.getErrorStatus(), null); // 해당 ErrorStatus로 응답 반환
    }


    // 5. 기타 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpectedException(Exception ex) {
        log.error("Unexpected Error: ", ex);
        // 일반적인 예외는 COMMON_INTERNAL_SERVER_ERROR로 처리
        return CustomException.createErrorResponse(ErrorStatus.COMMON_INTERNAL_SERVER_ERROR, ex.getMessage());
    }
}