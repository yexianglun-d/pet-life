package com.petlife.server.modules.moderation.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.moderation.domain.entity.ModerationTaskEntity;
import com.petlife.server.modules.moderation.dto.response.ModerationTaskResponse;
import com.petlife.server.modules.moderation.persistence.dataobject.ModerationTaskDataObject;
import org.springframework.stereotype.Component;

/**
 * 内容审核任务转换器。
 */
@Component
public class ModerationTaskConverter {

    public ModerationTaskEntity toEntity(ModerationTaskDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new ModerationTaskEntity(
            dataObject.taskId(),
            dataObject.targetType(),
            dataObject.targetId(),
            dataObject.contentType(),
            dataObject.contentSnapshot(),
            dataObject.providerCode(),
            dataObject.reviewStatus(),
            dataObject.reviewResult(),
            dataObject.riskLabels(),
            dataObject.failureReason(),
            dataObject.callbackPayload(),
            dataObject.reviewedAt(),
            dataObject.createdAt(),
            dataObject.updatedAt()
        );
    }

    public ModerationTaskResponse toResponse(ModerationTaskEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ModerationTaskResponse(
            String.valueOf(entity.getTaskId()),
            entity.getTargetType(),
            String.valueOf(entity.getTargetId()),
            entity.getContentType(),
            entity.getContentSnapshot(),
            entity.getProviderCode(),
            entity.getReviewStatus(),
            entity.getReviewResult(),
            entity.getRiskLabels(),
            entity.getFailureReason(),
            entity.getCallbackPayload(),
            DateTimeConverters.toOffsetDateTime(entity.getReviewedAt()),
            DateTimeConverters.toOffsetDateTime(entity.getCreatedAt()),
            DateTimeConverters.toOffsetDateTime(entity.getUpdatedAt())
        );
    }
}
