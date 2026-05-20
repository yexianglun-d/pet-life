package com.petlife.server.modules.service.persistence.command;

import java.math.BigDecimal;

/**
 * 更新服务商坐标持久化命令。
 */
public class UpdateServiceProviderLocationCommand {

    private Long providerId;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String coordinateSource;

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getCoordinateSource() {
        return coordinateSource;
    }

    public void setCoordinateSource(String coordinateSource) {
        this.coordinateSource = coordinateSource;
    }
}
