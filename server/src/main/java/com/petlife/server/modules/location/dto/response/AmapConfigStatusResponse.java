package com.petlife.server.modules.location.dto.response;

import java.util.List;

/**
 * 地图配置状态响应。
 */
public record AmapConfigStatusResponse(
    String providerCode,
    boolean configured,
    String baseUrl,
    List<String> capabilities,
    String message
) {
}
