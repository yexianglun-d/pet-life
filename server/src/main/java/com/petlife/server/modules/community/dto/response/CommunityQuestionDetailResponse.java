package com.petlife.server.modules.community.dto.response;

import java.util.List;

/**
 * 社区问答详情响应。
 *
 * @param question 问题帖
 * @param answers 问答回复，当前复用社区评论作为回答承载
 */
public record CommunityQuestionDetailResponse(
    CommunityPostResponse question,
    List<CommunityCommentResponse> answers
) {
}
