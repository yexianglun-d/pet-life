package com.petlife.server.modules.community.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建社区评论请求。
 *
 * @param content 评论内容
 */
public record CreateCommunityCommentRequest(
    @NotBlank(message = "评论内容不能为空")
    String content
) {
}
