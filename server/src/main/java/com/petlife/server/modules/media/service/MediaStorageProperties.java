package com.petlife.server.modules.media.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 媒体存储配置。
 *
 * <p>当前默认使用本地文件存储；对象存储模式先稳定 key、bucket 与 CDN URL 口径，
 * 后续接入云厂商 SDK 时不影响媒体资产表和业务模块引用的 asset_id。</p>
 */
@Component
@ConfigurationProperties(prefix = "petlife.media.storage")
public class MediaStorageProperties {

    private String provider = "local";
    private String rootPath = "/tmp/petlife-media";
    private String bucketName = "petlife-local";
    private String publicBaseUrl;
    private String endpoint;
    private String region;
    private long maxFileSizeBytes = 20L * 1024L * 1024L;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }
}
