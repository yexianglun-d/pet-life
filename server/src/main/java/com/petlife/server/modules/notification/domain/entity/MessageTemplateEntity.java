package com.petlife.server.modules.notification.domain.entity;

import java.time.LocalDateTime;

/**
 * 消息模板领域实体。
 */
public final class MessageTemplateEntity {

    private final Long templateId;
    private final String templateCode;
    private final String channelType;
    private final String titleTemplate;
    private final String contentTemplate;
    private final boolean enabled;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public MessageTemplateEntity(
        Long templateId,
        String templateCode,
        String channelType,
        String titleTemplate,
        String contentTemplate,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.templateId = templateId;
        this.templateCode = templateCode;
        this.channelType = channelType;
        this.titleTemplate = titleTemplate;
        this.contentTemplate = contentTemplate;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public String getChannelType() {
        return channelType;
    }

    public String getTitleTemplate() {
        return titleTemplate;
    }

    public String getContentTemplate() {
        return contentTemplate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
