package com.petlife.server.modules.location.domain.entity;

import java.util.List;

/**
 * 高德 Web 服务配置状态。
 */
public record AmapConfigStatusEntity(
    String providerCode,
    boolean configured,
    String baseUrl,
    List<String> capabilities,
    String message
) {
}
