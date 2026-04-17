package com.petlife.server.config;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.common.response.ResponseCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * <p>该类负责把业务异常、参数校验异常和未处理异常统一转换为稳定的接口返回格式，
 * 是服务端对外契约稳定性的基础设施之一。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.failure(exception.getResponseCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException exception
    ) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? ResponseCode.BAD_REQUEST.getMessage() : fieldError.getDefaultMessage();
        return ResponseEntity.badRequest().body(ApiResponse.failure(ResponseCode.BAD_REQUEST, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
        ConstraintViolationException exception
    ) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.failure(ResponseCode.BAD_REQUEST, exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.failure(ResponseCode.INTERNAL_SERVER_ERROR, exception.getMessage()));
    }
}
