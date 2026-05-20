package com.petlife.server.modules.notification.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.notification.domain.entity.PushDeliveryRecordEntity;
import com.petlife.server.modules.notification.domain.entity.PushDeviceTokenEntity;
import com.petlife.server.modules.notification.domain.entity.PushTaskEntity;
import com.petlife.server.modules.notification.dto.response.PushDeliveryRecordResponse;
import com.petlife.server.modules.notification.dto.response.PushDeviceTokenResponse;
import com.petlife.server.modules.notification.dto.response.PushTaskResponse;
import com.petlife.server.modules.notification.persistence.dataobject.PushDeliveryRecordDataObject;
import com.petlife.server.modules.notification.persistence.dataobject.PushDeviceTokenDataObject;
import com.petlife.server.modules.notification.persistence.dataobject.PushTaskDataObject;
import org.springframework.stereotype.Component;

/**
 * Push 通知转换器。
 */
@Component
public class PushNotificationConverter {

    public PushDeviceTokenEntity toEntity(PushDeviceTokenDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new PushDeviceTokenEntity(
            dataObject.deviceTokenId(),
            dataObject.userId(),
            dataObject.platform(),
            dataObject.providerCode(),
            dataObject.deviceTokenSuffix(),
            dataObject.deviceId(),
            dataObject.appVersion(),
            dataObject.enabled(),
            dataObject.lastRegisteredAt(),
            dataObject.unregisteredAt(),
            dataObject.createdAt(),
            dataObject.updatedAt()
        );
    }

    public PushTaskEntity toEntity(PushTaskDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new PushTaskEntity(
            dataObject.pushTaskId(),
            dataObject.userId(),
            dataObject.notificationId(),
            dataObject.notifyType(),
            dataObject.bizType(),
            dataObject.bizId(),
            dataObject.title(),
            dataObject.content(),
            dataObject.providerCode(),
            dataObject.taskStatus(),
            dataObject.failureReason(),
            dataObject.createdAt(),
            dataObject.updatedAt()
        );
    }

    public PushDeliveryRecordEntity toEntity(PushDeliveryRecordDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new PushDeliveryRecordEntity(
            dataObject.deliveryRecordId(),
            dataObject.pushTaskId(),
            dataObject.deviceTokenId(),
            dataObject.userId(),
            dataObject.providerCode(),
            dataObject.deliveryStatus(),
            dataObject.failureReason(),
            dataObject.attemptedAt(),
            dataObject.createdAt()
        );
    }

    public PushDeviceTokenResponse toResponse(PushDeviceTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PushDeviceTokenResponse(
            String.valueOf(entity.getDeviceTokenId()),
            String.valueOf(entity.getUserId()),
            entity.getPlatform(),
            entity.getProviderCode(),
            entity.getDeviceTokenSuffix(),
            entity.getDeviceId(),
            entity.getAppVersion(),
            entity.getEnabled(),
            DateTimeConverters.toOffsetDateTime(entity.getLastRegisteredAt()),
            DateTimeConverters.toOffsetDateTime(entity.getUnregisteredAt()),
            DateTimeConverters.toOffsetDateTime(entity.getCreatedAt()),
            DateTimeConverters.toOffsetDateTime(entity.getUpdatedAt())
        );
    }

    public PushTaskResponse toResponse(PushTaskEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PushTaskResponse(
            String.valueOf(entity.getPushTaskId()),
            String.valueOf(entity.getUserId()),
            entity.getNotificationId() == null ? null : String.valueOf(entity.getNotificationId()),
            entity.getNotifyType(),
            entity.getBizType(),
            entity.getBizId() == null ? null : String.valueOf(entity.getBizId()),
            entity.getTitle(),
            entity.getContent(),
            entity.getProviderCode(),
            entity.getTaskStatus(),
            entity.getFailureReason(),
            DateTimeConverters.toOffsetDateTime(entity.getCreatedAt()),
            DateTimeConverters.toOffsetDateTime(entity.getUpdatedAt())
        );
    }

    public PushDeliveryRecordResponse toResponse(PushDeliveryRecordEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PushDeliveryRecordResponse(
            String.valueOf(entity.getDeliveryRecordId()),
            String.valueOf(entity.getPushTaskId()),
            String.valueOf(entity.getDeviceTokenId()),
            String.valueOf(entity.getUserId()),
            entity.getProviderCode(),
            entity.getDeliveryStatus(),
            entity.getFailureReason(),
            DateTimeConverters.toOffsetDateTime(entity.getAttemptedAt()),
            DateTimeConverters.toOffsetDateTime(entity.getCreatedAt())
        );
    }
}
