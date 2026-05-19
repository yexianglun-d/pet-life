package com.petlife.server.modules.community.dto.response;

import com.petlife.server.modules.media.dto.response.MediaAssetResponse;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 社区帖子响应。
 *
 * @param postId 帖子 ID
 * @param postType 帖子类型
 * @param title 帖子标题
 * @param content 帖子正文
 * @param sourceDailyLogId 来源萌宠日常 ID
 * @param topic 关联话题
 * @param mediaAssetIds 媒体资产 ID
 * @param mediaAssets 媒体资产元数据
 * @param cityCode 城市编码
 * @param visibility 可见性
 * @param reviewStatus 审核状态
 * @param likeCount 点赞数
 * @param commentCount 评论数
 * @param favoriteCount 收藏数
 * @param liked 当前用户是否已点赞
 * @param favorited 当前用户是否已收藏
 * @param publishedAt 发布时间
 * @param createdAt 创建时间
 * @param author 作者摘要
 * @param pet 宠物摘要
 */
public record CommunityPostResponse(
    String postId,
    String postType,
    String title,
    String content,
    String sourceDailyLogId,
    CommunityTopicResponse topic,
    List<String> mediaAssetIds,
    List<MediaAssetResponse> mediaAssets,
    String cityCode,
    String visibility,
    String reviewStatus,
    Integer likeCount,
    Integer commentCount,
    Integer favoriteCount,
    Boolean liked,
    Boolean favorited,
    OffsetDateTime publishedAt,
    OffsetDateTime createdAt,
    CommunityAuthorResponse author,
    CommunityPetResponse pet
) {
}
