package com.petlife.server.modules.service.domain.entity;

import java.time.LocalDateTime;

/**
 * 服务商评价领域实体。
 */
public class ProviderReviewEntity {

    private final Long reviewId;
    private final Long providerId;
    private final String providerName;
    private final String providerType;
    private final Long appointmentId;
    private final Long userId;
    private final String reviewerNickname;
    private final Long petId;
    private final String petName;
    private final Integer rating;
    private final String content;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ProviderReviewEntity(
        Long reviewId,
        Long providerId,
        String providerName,
        String providerType,
        Long appointmentId,
        Long userId,
        String reviewerNickname,
        Long petId,
        String petName,
        Integer rating,
        String content,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.reviewId = reviewId;
        this.providerId = providerId;
        this.providerName = providerName;
        this.providerType = providerType;
        this.appointmentId = appointmentId;
        this.userId = userId;
        this.reviewerNickname = reviewerNickname;
        this.petId = petId;
        this.petName = petName;
        this.rating = rating;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getReviewId() {
        return reviewId;
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

    public Long getAppointmentId() {
        return appointmentId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getReviewerNickname() {
        return reviewerNickname;
    }

    public Long getPetId() {
        return petId;
    }

    public String getPetName() {
        return petName;
    }

    public Integer getRating() {
        return rating;
    }

    public String getContent() {
        return content;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
