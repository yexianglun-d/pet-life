package com.petlife.server.modules.notification.converter;

import com.petlife.server.modules.notification.domain.entity.NotificationChannelConfigEntity;
import com.petlife.server.modules.notification.dto.response.NotificationChannelConfigResponse;
import com.petlife.server.modules.notification.persistence.dataobject.NotificationChannelConfigDataObject;
import org.springframework.stereotype.Component;

/**
 * 通知渠道配置转换器。
 */
@Component
public class NotificationChannelConfigConverter {

    public NotificationChannelConfigEntity toEntity(NotificationChannelConfigDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new NotificationChannelConfigEntity(
            dataObject.channelConfigId(),
            dataObject.channelType(),
            dataObject.providerCode(),
            dataObject.providerName(),
            Boolean.TRUE.equals(dataObject.enabled()),
            dataObject.configStatus(),
            dataObject.remark(),
            dataObject.createdAt(),
            dataObject.updatedAt()
        );
    }

    public NotificationChannelConfigResponse toResponse(NotificationChannelConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        return new NotificationChannelConfigResponse(
            String.valueOf(entity.getChannelConfigId()),
            entity.getChannelType(),
            entity.getProviderCode(),
            entity.getProviderName(),
            entity.isEnabled(),
            entity.getConfigStatus(),
            entity.getRemark(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
