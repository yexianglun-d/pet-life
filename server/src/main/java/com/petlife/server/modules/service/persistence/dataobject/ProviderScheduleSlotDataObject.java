package com.petlife.server.modules.service.persistence.dataobject;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 服务商预约时段持久化读模型。
 */
public record ProviderScheduleSlotDataObject(
    Long slotId,
    Long providerId,
    String appointmentType,
    LocalDate slotDate,
    LocalTime startTime,
    LocalTime endTime,
    Integer quota,
    Integer bookedCount,
    String status
) {
}
