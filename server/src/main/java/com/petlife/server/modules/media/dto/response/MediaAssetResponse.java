package com.petlife.server.modules.media.dto.response;

import java.time.OffsetDateTime;

/**
 * 媒体资产响应。
 *
 * @param assetId 资产 ID
 * @param bizType 业务类型
 * @param mediaType 媒体类型
 * @param fileName 原始文件名
 * @param contentType 文件内容类型
 * @param fileSize 文件大小
 * @param fileHash 文件哈希
 * @param uploadStatus 上传状态
 * @param reviewStatus 审核状态
 * @param accessUrl 文件访问地址
 * @param completedAt 上传完成时间
 * @param createdAt 创建时间
 */
public record MediaAssetResponse(
    String assetId,
    String bizType,
    String mediaType,
    String fileName,
    String contentType,
    Long fileSize,
    String fileHash,
    String uploadStatus,
    String reviewStatus,
    String accessUrl,
    OffsetDateTime completedAt,
    OffsetDateTime createdAt
) {
}
