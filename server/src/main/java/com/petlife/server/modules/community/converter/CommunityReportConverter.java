package com.petlife.server.modules.community.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.community.domain.entity.CommunityReportEntity;
import com.petlife.server.modules.community.dto.response.CommunityReportResponse;
import com.petlife.server.modules.community.persistence.dataobject.CommunityReportDataObject;
import org.springframework.stereotype.Component;

/**
 * 社区举报实体转换器。
 */
@Component
public class CommunityReportConverter {

    public CommunityReportEntity toEntity(CommunityReportDataObject communityReportDataObject) {
        if (communityReportDataObject == null) {
            return null;
        }

        return new CommunityReportEntity(
            communityReportDataObject.reportId(),
            communityReportDataObject.reporterUserId(),
            communityReportDataObject.targetType(),
            communityReportDataObject.targetId(),
            communityReportDataObject.reasonCode(),
            communityReportDataObject.reasonDetail(),
            communityReportDataObject.status(),
            communityReportDataObject.createdAt()
        );
    }

    public CommunityReportResponse toResponse(CommunityReportEntity communityReport) {
        return new CommunityReportResponse(
            String.valueOf(communityReport.getReportId()),
            communityReport.getTargetType(),
            String.valueOf(communityReport.getTargetId()),
            communityReport.getReasonCode(),
            communityReport.getReasonDetail(),
            communityReport.getStatus(),
            DateTimeConverters.toOffsetDateTime(communityReport.getCreatedAt())
        );
    }
}
