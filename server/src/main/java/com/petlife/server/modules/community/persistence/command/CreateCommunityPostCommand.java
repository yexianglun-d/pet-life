package com.petlife.server.modules.community.persistence.command;

import java.time.LocalDateTime;

/**
 * 创建社区帖子命令。
 */
public class CreateCommunityPostCommand {

    private Long id;
    private Long userId;
    private Long petId;
    private String postType;
    private String title;
    private String content;
    private Long sourceDailyLogId;
    private Long topicId;
    private String mediaListJson;
    private String cityCode;
    private String visibility;
    private String reviewStatus;
    private LocalDateTime publishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getSourceDailyLogId() {
        return sourceDailyLogId;
    }

    public void setSourceDailyLogId(Long sourceDailyLogId) {
        this.sourceDailyLogId = sourceDailyLogId;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public String getMediaListJson() {
        return mediaListJson;
    }

    public void setMediaListJson(String mediaListJson) {
        this.mediaListJson = mediaListJson;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
