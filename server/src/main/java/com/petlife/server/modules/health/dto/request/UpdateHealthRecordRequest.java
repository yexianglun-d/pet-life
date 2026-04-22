package com.petlife.server.modules.health.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

/**
 * 健康记录更新请求。
 *
 * @param recordType 记录类型
 * @param title 记录标题
 * @param value 数值
 * @param unit 单位
 * @param occurredAt 发生时间
 * @param notes 备注
 */
public record UpdateHealthRecordRequest(
    @NotBlank(message = "记录类型不能为空")
    String recordType,
    @NotBlank(message = "记录标题不能为空")
    String title,
    String value,
    String unit,
    OffsetDateTime occurredAt,
    String notes
) {
}
