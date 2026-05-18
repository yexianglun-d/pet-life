package com.petlife.server.modules.notification.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.notification.domain.entity.NotificationEntity;
import com.petlife.server.modules.notification.dto.response.NotificationResponse;
import com.petlife.server.modules.notification.persistence.dataobject.NotificationDataObject;
import org.springframework.stereotype.Component;

/**
 * 通知转换器。
 */
@Component
public class NotificationConverter {

    public NotificationEntity toEntity(NotificationDataObject notificationDataObject) {
        if (notificationDataObject == null) {
            return null;
        }

        return new NotificationEntity(
            notificationDataObject.notificationId(),
            notificationDataObject.userId(),
            notificationDataObject.notifyType(),
            notificationDataObject.bizType(),
            notificationDataObject.bizId(),
            notificationDataObject.title(),
            notificationDataObject.content(),
            notificationDataObject.readStatus(),
            notificationDataObject.sentAt(),
            notificationDataObject.readAt()
        );
    }

    public NotificationResponse toResponse(NotificationEntity notificationEntity) {
        return new NotificationResponse(
            String.valueOf(notificationEntity.getNotificationId()),
            notificationEntity.getNotifyType(),
            notificationEntity.getBizType(),
            notificationEntity.getBizId() == null ? null : String.valueOf(notificationEntity.getBizId()),
            notificationEntity.getTitle(),
            notificationEntity.getContent(),
            notificationEntity.isRead(),
            DateTimeConverters.toOffsetDateTime(notificationEntity.getSentAt()),
            DateTimeConverters.toOffsetDateTime(notificationEntity.getReadAt())
        );
    }
}
