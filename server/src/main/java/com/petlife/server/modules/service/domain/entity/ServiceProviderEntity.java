package com.petlife.server.modules.service.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务商领域实体。
 */
public class ServiceProviderEntity {

    private final Long providerId;
    private final String providerType;
    private final String providerName;
    private final String cityCode;
    private final String address;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String coordinateSource;
    private final Integer distanceMeters;
    private final String contactPhone;
    private final String businessHours;
    private final BigDecimal ratingAvg;
    private final Integer reviewCount;
    private final String status;
    private final List<ProviderServiceItemEntity> serviceItems;
    private final List<ProviderScheduleSlotEntity> availableSlots;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ServiceProviderEntity(
        Long providerId,
        String providerType,
        String providerName,
        String cityCode,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String coordinateSource,
        Integer distanceMeters,
        String contactPhone,
        String businessHours,
        BigDecimal ratingAvg,
        Integer reviewCount,
        String status,
        List<ProviderServiceItemEntity> serviceItems,
        List<ProviderScheduleSlotEntity> availableSlots,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.providerId = providerId;
        this.providerType = providerType;
        this.providerName = providerName;
        this.cityCode = cityCode;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.coordinateSource = coordinateSource;
        this.distanceMeters = distanceMeters;
        this.contactPhone = contactPhone;
        this.businessHours = businessHours;
        this.ratingAvg = ratingAvg;
        this.reviewCount = reviewCount;
        this.status = status;
        this.serviceItems = serviceItems == null ? List.of() : List.copyOf(serviceItems);
        this.availableSlots = availableSlots == null ? List.of() : List.copyOf(availableSlots);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getProviderId() {
        return providerId;
    }

    public String getProviderType() {
        return providerType;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getCityCode() {
        return cityCode;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getCoordinateSource() {
        return coordinateSource;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getBusinessHours() {
        return businessHours;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public String getStatus() {
        return status;
    }

    public List<ProviderServiceItemEntity> getServiceItems() {
        return serviceItems;
    }

    public List<ProviderScheduleSlotEntity> getAvailableSlots() {
        return availableSlots;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isBookable() {
        return "online".equals(status) && availableSlots.stream().anyMatch(ProviderScheduleSlotEntity::isBookable);
    }
}
