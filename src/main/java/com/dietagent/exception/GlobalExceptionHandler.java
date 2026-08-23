package com.dietagent.exception;

import com.dietagent.dto.response.AgentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<AgentResponse> handleBusinessException(BusinessException e) {
        return ResponseEntity.badRequest().body(
                AgentResponse.builder()
                        .message(e.getMessage())
                        .type("error")
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("type", "error");
        response.put("message", "参数验证失败");
        response.put("errors", errors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AgentResponse> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AgentResponse.builder()
                        .message("服务器内部错误: " + e.getMessage())
                        .type("error")
                        .build()
        );
    }
}
