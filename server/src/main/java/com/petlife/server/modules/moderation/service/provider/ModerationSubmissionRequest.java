package com.petlife.server.modules.moderation.service.provider;

/**
 * 内容审核提交请求。
 */
public record ModerationSubmissionRequest(
    String targetType,
    Long targetId,
    String contentType,
    String contentSnapshot
) {
}
