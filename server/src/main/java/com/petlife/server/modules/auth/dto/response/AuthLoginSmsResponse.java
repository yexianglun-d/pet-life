package com.petlife.server.modules.auth.dto.response;

import java.util.List;

/**
 * 短信登录响应。
 *
 * @param accessToken 访问令牌
 * @param refreshToken 刷新令牌
 * @param user 用户摘要
 * @param familySummary 家庭摘要
 * @param pets 宠物列表
 * @param currentPetId 当前宠物 ID
 */
public record AuthLoginSmsResponse(
    String accessToken,
    String refreshToken,
    AuthUserResponse user,
    AuthFamilySummaryResponse familySummary,
    List<AuthPetSummaryResponse> pets,
    String currentPetId
) {
}
