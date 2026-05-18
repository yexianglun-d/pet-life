package com.petlife.server.modules.service.domain.entity;

import java.math.BigDecimal;

/**
 * 服务项目领域实体。
 */
public class ProviderServiceItemEntity {

    private final Long serviceItemId;
    private final Long providerId;
    private final String serviceCode;
    private final String serviceName;
    private final String serviceDesc;
    private final BigDecimal priceMin;
    private final BigDecimal priceMax;
    private final String status;

    public ProviderServiceItemEntity(
        Long serviceItemId,
        Long providerId,
        String serviceCode,
        String serviceName,
        String serviceDesc,
        BigDecimal priceMin,
        BigDecimal priceMax,
        String status
    ) {
        this.serviceItemId = serviceItemId;
        this.providerId = providerId;
        this.serviceCode = serviceCode;
        this.serviceName = serviceName;
        this.serviceDesc = serviceDesc;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
        this.status = status;
    }

    public Long getServiceItemId() {
        return serviceItemId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceDesc() {
        return serviceDesc;
    }

    public BigDecimal getPriceMin() {
        return priceMin;
    }

    public BigDecimal getPriceMax() {
        return priceMax;
    }

    public String getStatus() {
        return status;
    }
}
