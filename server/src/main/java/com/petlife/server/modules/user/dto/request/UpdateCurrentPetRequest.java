package com.petlife.server.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 当前宠物切换请求。
 *
 * @param petId 宠物 ID
 */
public record UpdateCurrentPetRequest(
    @NotBlank(message = "宠物 ID 不能为空")
    String petId
) {
}
