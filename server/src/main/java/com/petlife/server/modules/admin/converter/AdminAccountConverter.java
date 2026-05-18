package com.petlife.server.modules.admin.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.admin.domain.entity.AdminAccountEntity;
import com.petlife.server.modules.admin.dto.response.AdminAccountResponse;
import com.petlife.server.modules.admin.persistence.dataobject.AdminAccountDataObject;
import org.springframework.stereotype.Component;

/**
 * 后台管理员账号转换器。
 */
@Component
public class AdminAccountConverter {

    public AdminAccountEntity toEntity(AdminAccountDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new AdminAccountEntity(
            dataObject.adminAccountId(),
            dataObject.username(),
            dataObject.passwordHash(),
            dataObject.displayName(),
            dataObject.roleCode(),
            dataObject.status(),
            dataObject.lastLoginAt(),
            dataObject.createdAt()
        );
    }

    public AdminAccountResponse toResponse(AdminAccountEntity entity) {
        return new AdminAccountResponse(
            String.valueOf(entity.getAdminAccountId()),
            entity.getUsername(),
            entity.getDisplayName(),
            entity.getRoleCode(),
            entity.getStatus(),
            DateTimeConverters.toOffsetDateTime(entity.getLastLoginAt()),
            DateTimeConverters.toOffsetDateTime(entity.getCreatedAt())
        );
    }
}
