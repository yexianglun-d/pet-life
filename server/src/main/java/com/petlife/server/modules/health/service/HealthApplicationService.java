package com.petlife.server.modules.health.service;

import com.petlife.server.bootstrap.devsupport.BootstrapMemoryStore;
import com.petlife.server.bootstrap.devsupport.model.DevHealthRecord;
import com.petlife.server.modules.health.dto.request.CreateHealthRecordRequest;
import com.petlife.server.modules.health.dto.response.HealthRecordResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 健康记录应用服务。
 *
 * <p>该服务负责把宠物维度的健康记录请求转换为统一输出格式，后续切换数据库或聚合更多健康事件时，
 * 控制器层不需要感知底层存储变化。</p>
 */
@Service
public class HealthApplicationService {

    private final BootstrapMemoryStore bootstrapMemoryStore;

    public HealthApplicationService(BootstrapMemoryStore bootstrapMemoryStore) {
        this.bootstrapMemoryStore = bootstrapMemoryStore;
    }

    public List<HealthRecordResponse> listHealthRecords(Long petId) {
        return bootstrapMemoryStore.listHealthRecords(petId).stream()
            .map(this::toHealthRecordResponse)
            .toList();
    }

    public HealthRecordResponse createHealthRecord(Long petId, CreateHealthRecordRequest request) {
        DevHealthRecord healthRecord = bootstrapMemoryStore.createHealthRecord(
            petId,
            request.recordType(),
            request.title(),
            request.value(),
            request.unit(),
            request.occurredAt(),
            request.notes()
        );
        return toHealthRecordResponse(healthRecord);
    }

    private HealthRecordResponse toHealthRecordResponse(DevHealthRecord healthRecord) {
        return new HealthRecordResponse(
            String.valueOf(healthRecord.healthRecordId()),
            String.valueOf(healthRecord.petId()),
            healthRecord.recordType(),
            healthRecord.title(),
            healthRecord.value(),
            healthRecord.unit(),
            healthRecord.occurredAt(),
            healthRecord.notes(),
            healthRecord.createdAt()
        );
    }
}
