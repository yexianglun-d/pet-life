package com.petlife.server.modules.health.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 健康记录创建请求。
 *
 * @param recordType 记录类型
 * @param title 记录标题
 * @param value 数值
 * @param unit 单位
 * @param hospitalName 医院名称
 * @param doctorName 医生名称
 * @param severityLevel 严重程度
 * @param resultSummary 结果摘要
 * @param attachmentAssetIds 附件资产 ID
 * @param nextReminderAt 下一次提醒时间
 * @param nextReminderTitle 下一次提醒标题
 * @param occurredAt 发生时间
 * @param notes 备注
 */
public record CreateHealthRecordRequest(
    @NotBlank(message = "记录类型不能为空")
    String recordType,
    @NotBlank(message = "记录标题不能为空")
    String title,
    String value,
    String unit,
    @Size(max = 100, message = "医院名称长度不能超过 100 个字符")
    String hospitalName,
    @Size(max = 50, message = "医生名称长度不能超过 50 个字符")
    String doctorName,
    @Size(max = 20, message = "严重程度长度不能超过 20 个字符")
    String severityLevel,
    @Size(max = 500, message = "结果摘要长度不能超过 500 个字符")
    String resultSummary,
    @Size(max = 9, message = "健康记录附件最多上传 9 个")
    List<String> attachmentAssetIds,
    OffsetDateTime nextReminderAt,
    @Size(max = 100, message = "下一次提醒标题长度不能超过 100 个字符")
    String nextReminderTitle,
    OffsetDateTime occurredAt,
    String notes
) {
}
