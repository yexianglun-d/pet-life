package com.petlife.server.modules.dailylog.domain.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 萌宠日常实体。
 *
 * <p>该实体承接用户记录宠物日常的核心内容，
 * 后续既可服务个人记录，也可作为社区发布源数据。</p>
 */
public final class DailyLogEntity {

    private final Long dailyLogId;
    private final Long petId;
    private final Long authorUserId;
    private final String content;
    private final List<String> mediaAssetIds;
    private final List<String> tags;
    private final String visibility;
    private final boolean syncToCommunity;
    private final Long communityPostId;
    private final LocalDateTime happenedAt;
    private final LocalDateTime createdAt;

    public DailyLogEntity(
        Long dailyLogId,
        Long petId,
        Long authorUserId,
        String content,
        List<String> mediaAssetIds,
        List<String> tags,
        String visibility,
        boolean syncToCommunity,
        Long communityPostId,
        LocalDateTime happenedAt,
        LocalDateTime createdAt
    ) {
        this.dailyLogId = dailyLogId;
        this.petId = petId;
        this.authorUserId = authorUserId;
        this.content = content;
        this.mediaAssetIds = mediaAssetIds == null ? List.of() : List.copyOf(mediaAssetIds);
        this.tags = tags == null ? List.of() : List.copyOf(tags);
        this.visibility = visibility;
        this.syncToCommunity = syncToCommunity;
        this.communityPostId = communityPostId;
        this.happenedAt = happenedAt;
        this.createdAt = createdAt;
    }

    public Long getDailyLogId() {
        return dailyLogId;
    }

    public Long getPetId() {
        return petId;
    }

    public Long getAuthorUserId() {
        return authorUserId;
    }

    public String getContent() {
        return content;
    }

    public List<String> getMediaAssetIds() {
        return mediaAssetIds;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getVisibility() {
        return visibility;
    }

    public boolean isSyncToCommunity() {
        return syncToCommunity;
    }

    public Long getCommunityPostId() {
        return communityPostId;
    }

    public LocalDateTime getHappenedAt() {
        return happenedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
