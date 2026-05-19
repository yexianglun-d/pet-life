package com.petlife.server.modules.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 后台创建或更新消息模板请求。
 *
 * @param templateCode 模板编码
 * @param channelType 渠道类型
 * @param titleTemplate 标题模板
 * @param contentTemplate 内容模板
 * @param enabled 是否启用
 */
public record AdminUpsertMessageTemplateRequest(
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 64, message = "模板编码不能超过 64 个字符")
    String templateCode,

    @NotBlank(message = "渠道类型不能为空")
    @Size(max = 20, message = "渠道类型不能超过 20 个字符")
    String channelType,

    @Size(max = 100, message = "标题模板不能超过 100 个字符")
    String titleTemplate,

    @NotBlank(message = "内容模板不能为空")
    @Size(max = 500, message = "内容模板不能超过 500 个字符")
    String contentTemplate,

    @NotNull(message = "启用状态不能为空")
    Boolean enabled
) {
}
