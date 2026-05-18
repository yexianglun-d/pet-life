package com.petlife.server.modules.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.admin.converter.AuditLogConverter;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.dto.response.AuditLogResponse;
import com.petlife.server.modules.admin.persistence.AuditLogPersistenceMapper;
import com.petlife.server.modules.admin.persistence.command.CreateAuditLogCommand;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 后台审计日志应用服务。
 */
@Service
public class AuditLogApplicationService {

    private static final String OPERATOR_TYPE_ADMIN = "admin";
    private static final String DEFAULT_OPERATOR = "admin-console";
    private static final String UNKNOWN_IP_ADDRESS = "unknown";
    private static final int MAX_OPERATOR_ID_LENGTH = 64;
    private static final int MAX_TARGET_TYPE_LENGTH = 30;
    private static final int MAX_TARGET_ID_LENGTH = 64;
    private static final int MAX_ACTION_LENGTH = 50;
    private static final int MAX_IP_ADDRESS_LENGTH = 64;
    private static final int MAX_USER_AGENT_LENGTH = 255;
    private static final Set<String> SUPPORTED_SERVICE_TARGET_TYPES = Set.of(
        "service_city",
        "service_provider",
        "provider_service_item",
        "provider_schedule_slot",
        "service_appointment",
        "provider_review"
    );
    private static final Set<String> SUPPORTED_MODERATION_TARGET_TYPES = Set.of("moderation_report");

    private final AuditLogPersistenceMapper auditLogPersistenceMapper;
    private final AuditLogConverter auditLogConverter;
    private final ObjectMapper objectMapper;

    public AuditLogApplicationService(
        AuditLogPersistenceMapper auditLogPersistenceMapper,
        AuditLogConverter auditLogConverter,
        ObjectMapper objectMapper
    ) {
        this.auditLogPersistenceMapper = auditLogPersistenceMapper;
        this.auditLogConverter = auditLogConverter;
        this.objectMapper = objectMapper;
    }

    public AdminOperationContext resolveAdminOperationContext(
        String operatorName,
        HttpServletRequest httpServletRequest
    ) {
        return new AdminOperationContext(
            normalizeOperatorId(operatorName),
            resolveIpAddress(httpServletRequest),
            normalizeNullableText(
                httpServletRequest == null ? null : httpServletRequest.getHeader("User-Agent"),
                MAX_USER_AGENT_LENGTH
            )
        );
    }

    public void recordAdminOperation(
        AdminOperationContext operationContext,
        String targetType,
        String targetId,
        String action,
        Map<String, Object> detail
    ) {
        CreateAuditLogCommand command = new CreateAuditLogCommand();
        command.setOperatorType(OPERATOR_TYPE_ADMIN);
        command.setOperatorId(operationContext == null ? DEFAULT_OPERATOR : operationContext.operatorId());
        command.setTargetType(normalizeRequiredText(targetType, "审计目标类型不能为空", MAX_TARGET_TYPE_LENGTH));
        command.setTargetId(normalizeRequiredText(targetId, "审计目标 ID 不能为空", MAX_TARGET_ID_LENGTH));
        command.setAction(normalizeRequiredText(action, "审计动作不能为空", MAX_ACTION_LENGTH));
        command.setDetailJson(toDetailJson(detail));
        command.setIpAddress(operationContext == null ? UNKNOWN_IP_ADDRESS : operationContext.ipAddress());
        command.setUserAgent(operationContext == null ? null : operationContext.userAgent());
        auditLogPersistenceMapper.insertAuditLog(command);
    }

    public List<AuditLogResponse> listServiceAuditLogs(
        String operatorId,
        String targetType,
        String action
    ) {
        String normalizedOperatorId = normalizeNullableText(operatorId, MAX_OPERATOR_ID_LENGTH);
        String normalizedTargetType = normalizeNullableText(targetType, MAX_TARGET_TYPE_LENGTH);
        if (normalizedTargetType != null && !SUPPORTED_SERVICE_TARGET_TYPES.contains(normalizedTargetType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审计目标类型不支持");
        }
        String normalizedAction = normalizeNullableText(action, MAX_ACTION_LENGTH);
        return auditLogPersistenceMapper
            .listServiceAuditLogs(normalizedOperatorId, normalizedTargetType, normalizedAction)
            .stream()
            .map(auditLogConverter::toEntity)
            .map(auditLogConverter::toResponse)
            .toList();
    }

    public List<AuditLogResponse> listModerationAuditLogs(
        String operatorId,
        String targetType,
        String action
    ) {
        String normalizedOperatorId = normalizeNullableText(operatorId, MAX_OPERATOR_ID_LENGTH);
        String normalizedTargetType = normalizeNullableText(targetType, MAX_TARGET_TYPE_LENGTH);
        if (normalizedTargetType != null && !SUPPORTED_MODERATION_TARGET_TYPES.contains(normalizedTargetType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "审核审计目标类型不支持");
        }
        String normalizedAction = normalizeNullableText(action, MAX_ACTION_LENGTH);
        return auditLogPersistenceMapper
            .listModerationAuditLogs(normalizedOperatorId, normalizedTargetType, normalizedAction)
            .stream()
            .map(auditLogConverter::toEntity)
            .map(auditLogConverter::toResponse)
            .toList();
    }

    private String resolveIpAddress(HttpServletRequest httpServletRequest) {
        if (httpServletRequest == null) {
            return UNKNOWN_IP_ADDRESS;
        }
        String forwardedFor = normalizeNullableText(httpServletRequest.getHeader("X-Forwarded-For"), MAX_IP_ADDRESS_LENGTH);
        if (forwardedFor != null) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = normalizeNullableText(httpServletRequest.getHeader("X-Real-IP"), MAX_IP_ADDRESS_LENGTH);
        if (realIp != null) {
            return realIp;
        }
        return normalizeNullableText(httpServletRequest.getRemoteAddr(), MAX_IP_ADDRESS_LENGTH);
    }

    private String normalizeOperatorId(String operatorId) {
        String normalizedOperatorId = normalizeNullableText(operatorId, MAX_OPERATOR_ID_LENGTH);
        return normalizedOperatorId == null ? DEFAULT_OPERATOR : normalizedOperatorId;
    }

    private String normalizeRequiredText(String text, String message, int maxLength) {
        String normalizedText = normalizeNullableText(text, maxLength);
        if (normalizedText == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, message);
        }
        return normalizedText;
    }

    private String normalizeNullableText(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String normalizedText = text.trim();
        if (normalizedText.isEmpty()) {
            return null;
        }
        if (normalizedText.length() > maxLength) {
            return normalizedText.substring(0, maxLength);
        }
        return normalizedText;
    }

    private String toDetailJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail == null ? Map.of() : detail);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR, "审计详情序列化失败");
        }
    }
}
