package com.petlife.server.modules.admin.dto.response;

/**
 * 后台用户上下文响应。
 *
 * @param userId 用户 ID
 * @param nickname 用户昵称
 * @param mobile 用户手机号
 */
public record AdminUserContextResponse(
    String userId,
    String nickname,
    String mobile
) {
}
