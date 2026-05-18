package com.petlife.server.modules.dailylog.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 萌宠日常更新请求。
 *
 * @param content 内容
 * @param mediaAssetIds 媒体资产 ID
 * @param tags 标签
 * @param visibility 可见范围
 * @param syncToCommunity 是否同步社区
 * @param happenedAt 记录时间
 */
public record UpdateDailyLogRequest(
    @NotBlank(message = "日常内容不能为空")
    String content,
    List<String> mediaAssetIds,
    List<String> tags,
    String visibility,
    Boolean syncToCommunity,
    OffsetDateTime happenedAt
) {
}
