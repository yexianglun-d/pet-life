package com.petlife.server.modules.auth.dto.response;

/**
 * 验证码发送响应。
 *
 * @param mobile 手机号
 * @param scene 业务场景
 * @param mockedCode 开发期验证码
 * @param expiresInSeconds 过期秒数
 */
public record AuthSmsSendResponse(
    String mobile,
    String scene,
    String mockedCode,
    Integer expiresInSeconds
) {
}
