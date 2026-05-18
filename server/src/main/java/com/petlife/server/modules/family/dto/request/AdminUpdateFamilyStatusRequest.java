package com.petlife.server.modules.family.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 后台更新家庭状态请求。
 *
 * @param status 目标状态：1-正常，2-停用
 * @param reason 操作原因
 */
public record AdminUpdateFamilyStatusRequest(
    @Min(value = 1, message = "家庭状态仅支持 1 或 2")
    @Max(value = 2, message = "家庭状态仅支持 1 或 2")
    Integer status,

    @Size(max = 200, message = "操作原因长度不能超过 200 个字符")
    String reason
) {
}
