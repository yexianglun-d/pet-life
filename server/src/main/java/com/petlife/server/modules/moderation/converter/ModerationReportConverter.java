package com.petlife.server.modules.moderation.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.moderation.domain.entity.ModerationReportEntity;
import com.petlife.server.modules.moderation.dto.response.ModerationReportResponse;
import com.petlife.server.modules.moderation.persistence.dataobject.ModerationReportDataObject;
import org.springframework.stereotype.Component;

/**
 * 审核中心举报转换器。
 */
@Component
public class ModerationReportConverter {

    public ModerationReportEntity toEntity(ModerationReportDataObject moderationReportDataObject) {
        if (moderationReportDataObject == null) {
            return null;
        }

        return new ModerationReportEntity(
            moderationReportDataObject.reportId(),
            moderationReportDataObject.targetType(),
            moderationReportDataObject.targetId(),
            moderationReportDataObject.reasonCode(),
            moderationReportDataObject.reasonDetail(),
            moderationReportDataObject.status(),
            moderationReportDataObject.processedBy(),
            moderationReportDataObject.adminNotes(),
            moderationReportDataObject.processedAt(),
            moderationReportDataObject.createdAt(),
            moderationReportDataObject.reporterUserId(),
            moderationReportDataObject.reporterNickname(),
            moderationReportDataObject.reporterMobile(),
            moderationReportDataObject.postId(),
            moderationReportDataObject.postTitle(),
            moderationReportDataObject.postContent(),
            moderationReportDataObject.postReviewStatus(),
            moderationReportDataObject.postVisibility(),
            moderationReportDataObject.postDeletedAt() != null,
            moderationReportDataObject.postAuthorUserId(),
            moderationReportDataObject.postAuthorNickname()
        );
    }

    public ModerationReportResponse toResponse(ModerationReportEntity moderationReport) {
        return new ModerationReportResponse(
            String.valueOf(moderationReport.getReportId()),
            moderationReport.getTargetType(),
            String.valueOf(moderationReport.getTargetId()),
            moderationReport.getReasonCode(),
            moderationReport.getReasonDetail(),
            moderationReport.getStatus(),
            moderationReport.getProcessedBy(),
            moderationReport.getAdminNotes(),
            DateTimeConverters.toOffsetDateTime(moderationReport.getProcessedAt()),
            DateTimeConverters.toOffsetDateTime(moderationReport.getCreatedAt()),
            moderationReport.getReporterUserId() == null ? null : String.valueOf(moderationReport.getReporterUserId()),
            moderationReport.getReporterNickname(),
            moderationReport.getReporterMobile(),
            moderationReport.getPostId() == null ? null : String.valueOf(moderationReport.getPostId()),
            moderationReport.getPostTitle(),
            moderationReport.getPostContent(),
            moderationReport.getPostReviewStatus(),
            moderationReport.getPostVisibility(),
            moderationReport.isPostDeleted(),
            moderationReport.getPostAuthorUserId() == null ? null : String.valueOf(moderationReport.getPostAuthorUserId()),
            moderationReport.getPostAuthorNickname()
        );
    }
}
