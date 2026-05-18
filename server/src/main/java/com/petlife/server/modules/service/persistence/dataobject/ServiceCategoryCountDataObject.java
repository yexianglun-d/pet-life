package com.petlife.server.modules.service.persistence.dataobject;

/**
 * 服务分类数量读模型。
 */
public record ServiceCategoryCountDataObject(
    String providerType,
    Integer providerCount
) {
}
