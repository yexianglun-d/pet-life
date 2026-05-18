package com.petlife.server.modules.media.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 媒体资产数据对象。
 */
public record MediaAssetDataObject(
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
}
