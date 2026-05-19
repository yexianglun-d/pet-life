package com.petlife.server.modules.community.persistence.command;

/**
 * 更新社区帖子命令。
 */
public class UpdateCommunityPostCommand {

    private Long postId;
    private String title;
    private String content;
    private Long topicId;
    private String mediaListJson;
    private String cityCode;
    private String visibility;
    private String reviewStatus;

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
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
}
