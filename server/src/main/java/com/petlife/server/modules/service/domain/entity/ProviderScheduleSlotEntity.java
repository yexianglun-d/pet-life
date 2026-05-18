package com.petlife.server.modules.service.domain.entity;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 服务商可预约时段领域实体。
 */
public class ProviderScheduleSlotEntity {

    private final Long slotId;
    private final Long providerId;
    private final String appointmentType;
    private final LocalDate slotDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Integer quota;
    private final Integer bookedCount;
    private final String status;

    public ProviderScheduleSlotEntity(
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
        this.slotId = slotId;
        this.providerId = providerId;
        this.appointmentType = appointmentType;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.quota = quota == null ? 0 : quota;
        this.bookedCount = bookedCount == null ? 0 : bookedCount;
        this.status = status;
    }

    public Long getSlotId() {
        return slotId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public String getAppointmentType() {
        return appointmentType;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Integer getQuota() {
        return quota;
    }

    public Integer getBookedCount() {
        return bookedCount;
    }

    public String getStatus() {
        return status;
    }

    public int getAvailableQuota() {
        return Math.max(0, quota - bookedCount);
    }

    public boolean isBookable() {
        return "open".equals(status) && getAvailableQuota() > 0;
    }
}
