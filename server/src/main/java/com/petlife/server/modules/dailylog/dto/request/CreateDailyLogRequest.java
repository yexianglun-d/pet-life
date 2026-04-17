package com.petlife.server.modules.dailylog.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 萌宠日常创建请求。
 *
 * @param content 内容
 * @param tags 标签
 * @param visibility 可见范围
 * @param happenedAt 记录时间
 */
public record CreateDailyLogRequest(
    @NotBlank(message = "日常内容不能为空")
    String content,
    List<String> tags,
    String visibility,
    OffsetDateTime happenedAt
) {
}
