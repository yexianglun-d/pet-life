package com.petlife.server.modules.moderation.persistence.command;

/**
 * 更新内容审核任务结论命令。
 */
public class UpdateModerationTaskReviewCommand {

    private Long taskId;
    private String reviewStatus;
    private String reviewResult;
    private String riskLabels;
    private String failureReason;
    private String callbackPayload;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewResult() {
        return reviewResult;
    }

    public void setReviewResult(String reviewResult) {
        this.reviewResult = reviewResult;
    }

    public String getRiskLabels() {
        return riskLabels;
    }

    public void setRiskLabels(String riskLabels) {
        this.riskLabels = riskLabels;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getCallbackPayload() {
        return callbackPayload;
    }

    public void setCallbackPayload(String callbackPayload) {
        this.callbackPayload = callbackPayload;
    }
}
