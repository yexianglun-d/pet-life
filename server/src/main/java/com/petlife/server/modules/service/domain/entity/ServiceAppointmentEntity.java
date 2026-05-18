package com.petlife.server.modules.service.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 服务预约领域实体。
 */
public class ServiceAppointmentEntity {

    private final Long appointmentId;
    private final Long userId;
    private final Long petId;
    private final String petName;
    private final Long providerId;
    private final String providerName;
    private final String providerType;
    private final String appointmentType;
    private final LocalDate appointmentDate;
    private final String appointmentSlot;
    private final String demandDesc;
    private final String contactName;
    private final String contactMobile;
    private final String status;
    private final String remark;
    private final boolean reviewed;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ServiceAppointmentEntity(
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
        boolean reviewed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.appointmentId = appointmentId;
        this.userId = userId;
        this.petId = petId;
        this.petName = petName;
        this.providerId = providerId;
        this.providerName = providerName;
        this.providerType = providerType;
        this.appointmentType = appointmentType;
        this.appointmentDate = appointmentDate;
        this.appointmentSlot = appointmentSlot;
        this.demandDesc = demandDesc;
        this.contactName = contactName;
        this.contactMobile = contactMobile;
        this.status = status;
        this.remark = remark;
        this.reviewed = reviewed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getPetId() {
        return petId;
    }

    public String getPetName() {
        return petName;
    }

    public Long getProviderId() {
        return providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getProviderType() {
        return providerType;
    }

    public String getAppointmentType() {
        return appointmentType;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public String getAppointmentSlot() {
        return appointmentSlot;
    }

    public String getDemandDesc() {
        return demandDesc;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactMobile() {
        return contactMobile;
    }

    public String getStatus() {
        return status;
    }

    public String getRemark() {
        return remark;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
