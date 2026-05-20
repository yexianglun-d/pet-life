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
        return ResponseEntity.status(resolveBusinessStatus(exception.getResponseCode()))
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

    private HttpStatus resolveBusinessStatus(ResponseCode responseCode) {
        return switch (responseCode) {
            case UNAUTHORIZED, AUTH_REFRESH_TOKEN_INVALID -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, FAMILY_ROLE_FORBIDDEN, PET_PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case AUTH_SMS_SEND_RATE_LIMITED, AUTH_SMS_CODE_ATTEMPT_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case RESOURCE_NOT_FOUND,
                 USER_CURRENT_PET_NOT_FOUND,
                 FAMILY_NOT_FOUND,
                 FAMILY_MEMBER_NOT_FOUND,
                 FAMILY_INVITATION_NOT_FOUND,
                 PET_NOT_FOUND,
                 HEALTH_RECORD_NOT_FOUND,
                 REMINDER_NOT_FOUND,
                 REMINDER_TEMPLATE_NOT_FOUND,
                 DAILY_LOG_NOT_FOUND,
                 COMMUNITY_POST_NOT_FOUND,
                 COMMUNITY_TOPIC_NOT_FOUND,
                 COMMUNITY_QUESTION_NOT_FOUND,
                 MODERATION_REPORT_NOT_FOUND,
                 MODERATION_TASK_NOT_FOUND,
                 NOTIFICATION_NOT_FOUND,
                 MESSAGE_TEMPLATE_NOT_FOUND,
                 NOTIFICATION_CHANNEL_NOT_FOUND,
                 PUSH_DEVICE_TOKEN_NOT_FOUND,
                 MEDIA_ASSET_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INTERNAL_SERVER_ERROR, AUTH_SMS_SEND_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
