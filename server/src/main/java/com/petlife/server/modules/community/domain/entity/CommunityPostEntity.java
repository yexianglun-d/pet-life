package com.petlife.server.modules.community.domain.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区帖子实体。
 *
 * <p>社区帖子既承接公开萌宠日常同步，也支持用户独立发布。
 * 审核状态统一决定帖子是否进入用户侧可见流。</p>
 */
public final class CommunityPostEntity {

    private final Long postId;
    private final String postType;
    private final String title;
    private final String content;
    private final Long sourceDailyLogId;
    private final CommunityTopicEntity topic;
    private final List<String> mediaAssetIds;
    private final String cityCode;
    private final String visibility;
    private final String reviewStatus;
    private final Integer likeCount;
    private final Integer commentCount;
    private final Integer favoriteCount;
    private final boolean liked;
    private final boolean favorited;
    private final LocalDateTime publishedAt;
    private final LocalDateTime createdAt;
    private final CommunityAuthorEntity author;
    private final CommunityPetEntity pet;

    public CommunityPostEntity(
        Long postId,
        String postType,
        String title,
        String content,
        Long sourceDailyLogId,
        CommunityTopicEntity topic,
        List<String> mediaAssetIds,
        String cityCode,
        String visibility,
        String reviewStatus,
        Integer likeCount,
        Integer commentCount,
        Integer favoriteCount,
        boolean liked,
        boolean favorited,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        CommunityAuthorEntity author,
        CommunityPetEntity pet
    ) {
        this.postId = postId;
        this.postType = postType;
        this.title = title;
        this.content = content;
        this.sourceDailyLogId = sourceDailyLogId;
        this.topic = topic;
        this.mediaAssetIds = mediaAssetIds == null ? List.of() : List.copyOf(mediaAssetIds);
        this.cityCode = cityCode;
        this.visibility = visibility;
        this.reviewStatus = reviewStatus;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.favoriteCount = favoriteCount;
        this.liked = liked;
        this.favorited = favorited;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.author = author;
        this.pet = pet;
    }

    public Long getPostId() {
        return postId;
    }

    public String getPostType() {
        return postType;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Long getSourceDailyLogId() {
        return sourceDailyLogId;
    }

    public CommunityTopicEntity getTopic() {
        return topic;
    }

    public List<String> getMediaAssetIds() {
        return mediaAssetIds;
    }

    public String getCityCode() {
        return cityCode;
    }

    public String getVisibility() {
        return visibility;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public Integer getFavoriteCount() {
        return favoriteCount;
    }

    public boolean isLiked() {
        return liked;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public CommunityAuthorEntity getAuthor() {
        return author;
    }

    public CommunityPetEntity getPet() {
        return pet;
    }
}
