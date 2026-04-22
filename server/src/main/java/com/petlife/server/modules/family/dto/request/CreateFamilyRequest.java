package com.petlife.server.modules.family.dto.request;

/**
 * 创建或初始化家庭请求。
 *
 * @param familyName 家庭名称
 */
public record CreateFamilyRequest(
    String familyName
) {
}
