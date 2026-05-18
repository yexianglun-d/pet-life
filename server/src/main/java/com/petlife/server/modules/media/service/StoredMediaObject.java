package com.petlife.server.modules.media.service;

/**
 * 已存储媒体对象。
 *
 * @param objectKey 存储对象路径
 * @param bucketName 存储桶名称
 * @param accessUrl 外部访问地址
 * @param contentType 文件内容类型
 * @param fileSize 文件大小
 * @param fileHash 文件哈希
 */
public record StoredMediaObject(
    String objectKey,
    String bucketName,
    String accessUrl,
    String contentType,
    long fileSize,
    String fileHash
) {
}
