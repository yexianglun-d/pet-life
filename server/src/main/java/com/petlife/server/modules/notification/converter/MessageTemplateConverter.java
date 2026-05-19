package com.petlife.server.modules.notification.converter;

import com.petlife.server.modules.notification.domain.entity.MessageTemplateEntity;
import com.petlife.server.modules.notification.dto.response.MessageTemplateResponse;
import com.petlife.server.modules.notification.persistence.dataobject.MessageTemplateDataObject;
import org.springframework.stereotype.Component;

/**
 * 消息模板转换器。
 */
@Component
public class MessageTemplateConverter {

    private static final String STATUS_ACTIVE = "active";

    public MessageTemplateEntity toEntity(MessageTemplateDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new MessageTemplateEntity(
            dataObject.templateId(),
            dataObject.templateCode(),
            dataObject.channelType(),
            dataObject.titleTemplate(),
            dataObject.contentTemplate(),
            STATUS_ACTIVE.equals(dataObject.status()),
            dataObject.createdAt(),
            dataObject.updatedAt()
        );
    }

    public MessageTemplateResponse toResponse(MessageTemplateEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MessageTemplateResponse(
            String.valueOf(entity.getTemplateId()),
            entity.getTemplateCode(),
            entity.getChannelType(),
            entity.getTitleTemplate(),
            entity.getContentTemplate(),
            entity.isEnabled(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
