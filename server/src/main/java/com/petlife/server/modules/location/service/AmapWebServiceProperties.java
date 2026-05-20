package com.petlife.server.modules.location.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 高德 Web 服务配置。
 */
@Component
@ConfigurationProperties(prefix = "petlife.amap.web-service")
public class AmapWebServiceProperties {

    private String key;
    private String baseUrl = "https://restapi.amap.com";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public boolean isConfigured() {
        return key != null && !key.isBlank();
    }
}
