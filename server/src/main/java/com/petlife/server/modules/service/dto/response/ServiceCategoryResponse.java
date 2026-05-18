package com.petlife.server.modules.service.dto.response;

/**
 * 服务中心分类响应。
 *
 * @param providerType 服务商类型
 * @param title 分类标题
 * @param description 分类说明
 * @param providerCount 服务商数量
 * @param available 当前城市是否可用
 */
public record ServiceCategoryResponse(
    String providerType,
    String title,
    String description,
    Integer providerCount,
    boolean available
) {
}
