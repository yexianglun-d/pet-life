package com.petlife.server.modules.auth.dto.response;

/**
 * 用户摘要响应。
 *
 * @param userId 用户 ID
 * @param mobile 手机号
 * @param nickname 昵称
 * @param cityCode 城市编码
 * @param cityName 城市名称
 */
public record AuthUserResponse(
    String userId,
    String mobile,
    String nickname,
    String cityCode,
    String cityName
) {
}
