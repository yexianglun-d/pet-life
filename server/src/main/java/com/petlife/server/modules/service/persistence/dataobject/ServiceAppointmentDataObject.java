package com.petlife.server.modules.service.persistence.dataobject;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 服务预约持久化读模型。
 */
public record ServiceAppointmentDataObject(
    Long appointmentId,
    Long userId,
    Long petId,
    String petName,
    Long providerId,
    String providerName,
    String providerType,
    String appointmentType,
    LocalDate appointmentDate,
    String appointmentSlot,
    String demandDesc,
    String contactName,
    String contactMobile,
    String status,
    String remark,
    Boolean reviewed,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
