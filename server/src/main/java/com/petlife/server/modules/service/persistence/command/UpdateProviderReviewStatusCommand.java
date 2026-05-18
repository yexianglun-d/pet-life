package com.petlife.server.modules.service.persistence.command;

/**
 * 更新服务商评价状态命令。
 */
public class UpdateProviderReviewStatusCommand {

    private Long reviewId;
    private String status;

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
