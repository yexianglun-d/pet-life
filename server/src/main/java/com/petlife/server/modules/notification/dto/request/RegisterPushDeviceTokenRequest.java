package com.petlife.server.modules.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册 Push 设备 Token 请求。
 *
 * @param platform 客户端平台
 * @param providerCode Push 供应商编码
 * @param deviceToken 设备 Token
 * @param deviceId 客户端设备标识
 * @param appVersion App 版本
 */
public record RegisterPushDeviceTokenRequest(
    @NotBlank(message = "客户端平台不能为空")
    @Size(max = 20, message = "客户端平台不能超过 20 字符")
    String platform,
    @Size(max = 64, message = "Push 供应商编码不能超过 64 字符")
    String providerCode,
    @NotBlank(message = "设备 Token 不能为空")
    @Size(max = 512, message = "设备 Token 不能超过 512 字符")
    String deviceToken,
    @Size(max = 128, message = "设备标识不能超过 128 字符")
    String deviceId,
    @Size(max = 40, message = "App 版本不能超过 40 字符")
    String appVersion
) {
}
