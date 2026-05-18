package com.petlife.server.modules.dailylog.dto.response;

import com.petlife.server.modules.media.dto.response.MediaAssetResponse;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 萌宠日常响应。
 *
 * @param dailyLogId 日常记录 ID
 * @param petId 宠物 ID
 * @param content 内容
 * @param mediaAssetIds 媒体资产 ID
 * @param mediaAssets 媒体资产元数据
 * @param tags 标签
 * @param visibility 可见范围
 * @param syncToCommunity 是否同步社区
 * @param communityPostId 社区帖子 ID
 * @param happenedAt 记录时间
 * @param createdAt 创建时间
 */
public record DailyLogResponse(
    String dailyLogId,
    String petId,
    String content,
    List<String> mediaAssetIds,
    List<MediaAssetResponse> mediaAssets,
    List<String> tags,
    String visibility,
    Boolean syncToCommunity,
    String communityPostId,
    OffsetDateTime happenedAt,
    OffsetDateTime createdAt
) {
}
