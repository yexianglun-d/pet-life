package com.petlife.server.modules.community.persistence.dataobject;

import java.time.LocalDateTime;

/**
 * 社区帖子数据对象。
 *
 * @param postId 帖子 ID
 * @param postType 帖子类型
 * @param title 标题
 * @param content 正文
 * @param sourceDailyLogId 来源萌宠日常 ID
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
 * @param authorUserId 作者用户 ID
 * @param authorNickname 作者昵称
 * @param authorAvatarUrl 作者头像
 * @param petId 宠物 ID
 * @param petName 宠物名
 * @param petType 宠物类型
 * @param petBreed 宠物品种
 */
public record CommunityPostDataObject(
    Long postId,
    String postType,
    String title,
    String content,
    Long sourceDailyLogId,
    String cityCode,
    String visibility,
    String reviewStatus,
    Integer likeCount,
    Integer commentCount,
    Integer favoriteCount,
    Boolean liked,
    Boolean favorited,
    LocalDateTime publishedAt,
    LocalDateTime createdAt,
    Long authorUserId,
    String authorNickname,
    String authorAvatarUrl,
    Long petId,
    String petName,
    String petType,
    String petBreed
) {
}
