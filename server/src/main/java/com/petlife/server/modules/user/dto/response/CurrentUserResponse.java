package com.petlife.server.modules.user.dto.response;

import com.petlife.server.modules.auth.dto.response.AuthFamilySummaryResponse;
import com.petlife.server.modules.auth.dto.response.AuthPetSummaryResponse;
import com.petlife.server.modules.auth.dto.response.AuthUserResponse;

/**
 * 当前用户信息响应。
 *
 * @param user 用户信息
 * @param currentPetId 当前宠物 ID
 * @param currentPet 当前宠物摘要
 * @param familySummary 家庭摘要
 */
public record CurrentUserResponse(
    AuthUserResponse user,
    String currentPetId,
    AuthPetSummaryResponse currentPet,
    AuthFamilySummaryResponse familySummary
) {
}
