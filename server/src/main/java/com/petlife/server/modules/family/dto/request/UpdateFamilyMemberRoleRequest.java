package com.petlife.server.modules.family.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 家庭成员角色更新请求。
 *
 * @param role 目标角色
 */
public record UpdateFamilyMemberRoleRequest(
    @NotBlank(message = "角色不能为空")
    String role
) {
}
