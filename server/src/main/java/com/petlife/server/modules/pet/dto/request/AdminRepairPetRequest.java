package com.petlife.server.modules.pet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台宠物问题数据修复请求。
 *
 * @param repairType 修复类型：family_missing / owner_member_missing / current_pet_context
 * @param reason 修复原因
 */
public record AdminRepairPetRequest(
    @NotBlank(message = "修复类型不能为空")
    @Size(max = 50, message = "修复类型长度不能超过 50 个字符")
    String repairType,

    @Size(max = 200, message = "修复原因长度不能超过 200 个字符")
    String reason
) {
}
