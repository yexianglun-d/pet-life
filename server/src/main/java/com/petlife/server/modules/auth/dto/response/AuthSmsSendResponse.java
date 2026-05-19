package com.petlife.server.modules.auth.dto.response;

/**
 * 验证码发送响应。
 *
 * @param mobile 手机号
 * @param scene 业务场景
 * @param sent 是否已提交发送
 * @param expiresInSeconds 过期秒数
 * @param resendInSeconds 重发等待秒数
 * @param providerCode 供应商编码
 */
public record AuthSmsSendResponse(
    String mobile,
    String scene,
    Boolean sent,
    Integer expiresInSeconds,
    Integer resendInSeconds,
    String providerCode
) {
}
