package com.petlife.server.modules.health.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.health.domain.entity.HealthRecordEntity;
import com.petlife.server.modules.health.dto.response.HealthRecordResponse;
import com.petlife.server.modules.health.persistence.dataobject.HealthRecordDataObject;
import com.petlife.server.modules.media.dto.response.MediaAssetResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 健康记录实体转换器。
 */
@Component
public class HealthRecordEntityConverter {

    private static final TypeReference<List<String>> STRING_LIST_TYPE_REFERENCE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public HealthRecordEntityConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public HealthRecordEntity toEntity(HealthRecordDataObject healthRecordDataObject) {
        if (healthRecordDataObject == null) {
            return null;
        }

        return new HealthRecordEntity(
            healthRecordDataObject.healthRecordId(),
            healthRecordDataObject.petId(),
            healthRecordDataObject.operatorUserId(),
            healthRecordDataObject.recordType(),
            healthRecordDataObject.title(),
            healthRecordDataObject.occurredAt(),
            healthRecordDataObject.hospitalName(),
            healthRecordDataObject.doctorName(),
            healthRecordDataObject.severityLevel(),
            healthRecordDataObject.resultSummary(),
            parseAttachmentAssetIds(healthRecordDataObject.attachments()),
            healthRecordDataObject.nextReminderId(),
            healthRecordDataObject.nextReminderAt(),
            healthRecordDataObject.nextReminderStatus(),
            healthRecordDataObject.notes(),
            healthRecordDataObject.createdAt()
        );
    }

    public HealthRecordResponse toResponse(HealthRecordEntity healthRecord) {
        return toResponse(healthRecord, List.of());
    }

    public HealthRecordResponse toResponse(
        HealthRecordEntity healthRecord,
        List<MediaAssetResponse> attachmentAssets
    ) {
        ResultSummaryParts resultSummaryParts = splitResultSummary(healthRecord.getResultSummary());
        return new HealthRecordResponse(
            String.valueOf(healthRecord.getHealthRecordId()),
            String.valueOf(healthRecord.getPetId()),
            healthRecord.getRecordType(),
            healthRecord.getTitle(),
            resultSummaryParts.value(),
            resultSummaryParts.unit(),
            healthRecord.getHospitalName(),
            healthRecord.getDoctorName(),
            healthRecord.getSeverityLevel(),
            healthRecord.getResultSummary(),
            healthRecord.getAttachmentAssetIds(),
            attachmentAssets == null ? List.of() : attachmentAssets,
            healthRecord.getNextReminderId() == null ? null : String.valueOf(healthRecord.getNextReminderId()),
            DateTimeConverters.toOffsetDateTime(healthRecord.getNextReminderAt()),
            healthRecord.getNextReminderStatus(),
            DateTimeConverters.toOffsetDateTime(healthRecord.getOccurredAt()),
            healthRecord.getNotes(),
            DateTimeConverters.toOffsetDateTime(healthRecord.getCreatedAt())
        );
    }

    private List<String> parseAttachmentAssetIds(String attachments) {
        if (attachments == null || attachments.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(attachments, STRING_LIST_TYPE_REFERENCE);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    /**
     * 当前 DDL 使用 {@code result_summary} 承接单值结果。
     *
     * <p>为了不让接口层丢失已经提交的数值与单位，这里按写入规则把摘要还原为
     * {@code value/unit} 两个字段；若历史数据本身不符合该结构，则整体回落到 value。</p>
     */
    private ResultSummaryParts splitResultSummary(String resultSummary) {
        if (resultSummary == null || resultSummary.isBlank()) {
            return new ResultSummaryParts(null, null);
        }

        String normalizedSummary = resultSummary.trim();
        if (!normalizedSummary.matches("^-?\\d+(\\.\\d+)?(\\s+.+)?$")) {
            return new ResultSummaryParts(null, null);
        }

        int separatorIndex = normalizedSummary.indexOf(' ');
        if (separatorIndex < 0) {
            return new ResultSummaryParts(normalizedSummary, null);
        }

        String value = normalizedSummary.substring(0, separatorIndex).trim();
        String unit = normalizedSummary.substring(separatorIndex + 1).trim();
        return new ResultSummaryParts(
            value.isEmpty() ? normalizedSummary : value,
            unit.isEmpty() ? null : unit
        );
    }

    private record ResultSummaryParts(String value, String unit) {
    }
}
