package com.petlife.server.modules.location.domain.entity;

/**
 * 高德距离计算结果。
 */
public record AmapDistanceEntity(
    int originIndex,
    Integer distanceMeters,
    Integer durationSeconds,
    String statusInfo
) {
}
