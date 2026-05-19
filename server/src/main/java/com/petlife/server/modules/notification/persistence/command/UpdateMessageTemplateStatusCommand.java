package com.petlife.server.modules.notification.persistence.command;

/**
 * 更新消息模板状态命令。
 */
public class UpdateMessageTemplateStatusCommand {

    private Long templateId;
    private String status;

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
