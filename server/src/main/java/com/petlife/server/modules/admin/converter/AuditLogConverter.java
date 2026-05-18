package com.petlife.server.modules.admin.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.admin.domain.entity.AuditLogEntity;
import com.petlife.server.modules.admin.dto.response.AuditLogResponse;
import com.petlife.server.modules.admin.persistence.dataobject.AuditLogDataObject;
import org.springframework.stereotype.Component;

/**
 * 后台审计日志转换器。
 */
@Component
public class AuditLogConverter {

    public AuditLogEntity toEntity(AuditLogDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new AuditLogEntity(
            dataObject.auditLogId(),
            dataObject.operatorType(),
            dataObject.operatorId(),
            dataObject.targetType(),
            dataObject.targetId(),
            dataObject.action(),
            dataObject.detailJson(),
            dataObject.ipAddress(),
            dataObject.userAgent(),
            dataObject.createdAt()
        );
    }

    public AuditLogResponse toResponse(AuditLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AuditLogResponse(
            entity.getAuditLogId().toString(),
            entity.getOperatorType(),
            entity.getOperatorId(),
            entity.getTargetType(),
            entity.getTargetId(),
            entity.getAction(),
            entity.getDetailJson(),
            entity.getIpAddress(),
            entity.getUserAgent(),
            DateTimeConverters.toOffsetDateTime(entity.getCreatedAt())
        );
    }
}
