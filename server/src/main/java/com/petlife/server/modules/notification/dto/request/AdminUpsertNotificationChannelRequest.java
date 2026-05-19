package com.petlife.server.modules.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 后台创建或更新通知渠道配置请求。
 *
 * @param channelType 渠道类型
 * @param providerCode 供应商编码
 * @param providerName 供应商名称
 * @param enabled 是否启用
 * @param configStatus 配置状态
 * @param remark 备注
 */
public record AdminUpsertNotificationChannelRequest(
    @NotBlank(message = "渠道类型不能为空")
    @Size(max = 20, message = "渠道类型不能超过 20 个字符")
    String channelType,

    @NotBlank(message = "供应商编码不能为空")
    @Size(max = 64, message = "供应商编码不能超过 64 个字符")
    String providerCode,

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 100, message = "供应商名称不能超过 100 个字符")
    String providerName,

    @NotNull(message = "启用状态不能为空")
    Boolean enabled,

    @NotBlank(message = "配置状态不能为空")
    @Size(max = 20, message = "配置状态不能超过 20 个字符")
    String configStatus,

    @Size(max = 500, message = "备注不能超过 500 个字符")
    String remark
) {
}
