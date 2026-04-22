package com.petlife.server.modules.moderation.persistence.command;

/**
 * 处理举报命令。
 */
public class ProcessModerationReportCommand {

    private Long reportId;
    private String status;
    private String processedBy;

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }
}
