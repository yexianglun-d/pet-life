package com.petlife.server.modules.media.domain.entity;

import java.time.LocalDateTime;

/**
 * 媒体资产实体。
 *
 * <p>媒体上传是健康附件、萌宠日常图片视频和社区内容的共同基础能力。
 * 资产实体只表达文件事实和存储状态，不绑定具体业务记录，避免一个文件被多个模块重复落库。</p>
 */
public final class MediaAssetEntity {

    private final Long mediaAssetId;
    private final Long uploaderUserId;
    private final String bizType;
    private final String mediaType;
    private final String fileName;
    private final String objectKey;
    private final String bucketName;
    private final String cdnUrl;
    private final String contentType;
    private final Long fileSize;
    private final String fileHash;
    private final String uploadStatus;
    private final String reviewStatus;
    private final LocalDateTime completedAt;
    private final LocalDateTime createdAt;

    public MediaAssetEntity(
        Long mediaAssetId,
        Long uploaderUserId,
        String bizType,
        String mediaType,
        String fileName,
        String objectKey,
        String bucketName,
        String cdnUrl,
        String contentType,
        Long fileSize,
        String fileHash,
        String uploadStatus,
        String reviewStatus,
        LocalDateTime completedAt,
        LocalDateTime createdAt
    ) {
        this.mediaAssetId = mediaAssetId;
        this.uploaderUserId = uploaderUserId;
        this.bizType = bizType;
        this.mediaType = mediaType;
        this.fileName = fileName;
        this.objectKey = objectKey;
        this.bucketName = bucketName;
        this.cdnUrl = cdnUrl;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.fileHash = fileHash;
        this.uploadStatus = uploadStatus;
        this.reviewStatus = reviewStatus;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
    }

    public Long getMediaAssetId() {
        return mediaAssetId;
    }

    public Long getUploaderUserId() {
        return uploaderUserId;
    }

    public String getBizType() {
        return bizType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getCdnUrl() {
        return cdnUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getFileHash() {
        return fileHash;
    }

    public String getUploadStatus() {
        return uploadStatus;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
