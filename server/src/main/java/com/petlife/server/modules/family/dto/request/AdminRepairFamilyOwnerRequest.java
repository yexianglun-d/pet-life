package com.petlife.server.modules.family.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 后台修复家庭拥有者成员关系请求。
 *
 * @param reason 修复原因
 */
public record AdminRepairFamilyOwnerRequest(
    @Size(max = 200, message = "修复原因长度不能超过 200 个字符")
    String reason
) {
}
