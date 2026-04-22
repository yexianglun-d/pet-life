package com.petlife.server.modules.health.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.health.domain.entity.HealthRecordEntity;
import com.petlife.server.modules.health.dto.response.HealthRecordResponse;
import com.petlife.server.modules.health.persistence.dataobject.HealthRecordDataObject;
import org.springframework.stereotype.Component;

/**
 * 健康记录实体转换器。
 */
@Component
public class HealthRecordEntityConverter {

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
            healthRecordDataObject.resultSummary(),
            healthRecordDataObject.notes(),
            healthRecordDataObject.createdAt()
        );
    }

    public HealthRecordResponse toResponse(HealthRecordEntity healthRecord) {
        ResultSummaryParts resultSummaryParts = splitResultSummary(healthRecord.getResultSummary());
        return new HealthRecordResponse(
            String.valueOf(healthRecord.getHealthRecordId()),
            String.valueOf(healthRecord.getPetId()),
            healthRecord.getRecordType(),
            healthRecord.getTitle(),
            resultSummaryParts.value(),
            resultSummaryParts.unit(),
            DateTimeConverters.toOffsetDateTime(healthRecord.getOccurredAt()),
            healthRecord.getNotes(),
            DateTimeConverters.toOffsetDateTime(healthRecord.getCreatedAt())
        );
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
