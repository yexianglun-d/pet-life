import 'package:petlife_mobile_app/shared/domain/models/media_asset_snapshot.dart';

/// 社区帖子快照。
class CommunityPostSnapshot {
  const CommunityPostSnapshot({
    required this.postId,
    required this.postType,
    required this.title,
    required this.content,
    required this.visibility,
    required this.likeCount,
    required this.commentCount,
    required this.favoriteCount,
    required this.liked,
    required this.favorited,
    required this.author,
    this.topic,
    this.mediaAssetIds = const <String>[],
    this.mediaAssets = const <MediaAssetSnapshot>[],
    this.reviewStatus = 'approved',
    this.sourceDailyLogId,
    this.cityCode,
    this.publishedAt,
    this.createdAt,
    this.pet,
  });

  final String postId;
  final String postType;
  final String title;
  final String content;
  final String? sourceDailyLogId;
  final String? cityCode;
  final String visibility;
  final int likeCount;
  final int commentCount;
  final int favoriteCount;
  final bool liked;
  final bool favorited;
  final CommunityTopicSnapshot? topic;
  final List<String> mediaAssetIds;
  final List<MediaAssetSnapshot> mediaAssets;
  final String reviewStatus;
  final DateTime? publishedAt;
  final DateTime? createdAt;
  final CommunityAuthorSnapshot author;
  final CommunityPetSnapshot? pet;

  CommunityPostSnapshot copyWith({
    String? postId,
    String? postType,
    String? title,
    String? content,
    String? sourceDailyLogId,
    String? cityCode,
    String? visibility,
    int? likeCount,
    int? commentCount,
    int? favoriteCount,
    bool? liked,
    bool? favorited,
    CommunityTopicSnapshot? topic,
    List<String>? mediaAssetIds,
    List<MediaAssetSnapshot>? mediaAssets,
    String? reviewStatus,
    DateTime? publishedAt,
    DateTime? createdAt,
    CommunityAuthorSnapshot? author,
    CommunityPetSnapshot? pet,
  }) {
    return CommunityPostSnapshot(
      postId: postId ?? this.postId,
      postType: postType ?? this.postType,
      title: title ?? this.title,
      content: content ?? this.content,
      sourceDailyLogId: sourceDailyLogId ?? this.sourceDailyLogId,
      cityCode: cityCode ?? this.cityCode,
      visibility: visibility ?? this.visibility,
      likeCount: likeCount ?? this.likeCount,
      commentCount: commentCount ?? this.commentCount,
      favoriteCount: favoriteCount ?? this.favoriteCount,
      liked: liked ?? this.liked,
      favorited: favorited ?? this.favorited,
      topic: topic ?? this.topic,
      mediaAssetIds: mediaAssetIds ?? this.mediaAssetIds,
      mediaAssets: mediaAssets ?? this.mediaAssets,
      reviewStatus: reviewStatus ?? this.reviewStatus,
      publishedAt: publishedAt ?? this.publishedAt,
      createdAt: createdAt ?? this.createdAt,
      author: author ?? this.author,
      pet: pet ?? this.pet,
    );
  }
}

/// 社区发帖草稿。
class CommunityPostDraft {
  const CommunityPostDraft({
    required this.postType,
    required this.content,
    required this.mediaAssetIds,
    required this.visibility,
    this.petId,
    this.topicId,
    this.title,
    this.cityCode,
  });

  final String postType;
  final String content;
  final List<String> mediaAssetIds;
  final String visibility;
  final String? petId;
  final String? topicId;
  final String? title;
  final String? cityCode;
}

/// 社区作者快照。
class CommunityAuthorSnapshot {
  const CommunityAuthorSnapshot({
    required this.userId,
    required this.nickname,
    this.avatarUrl,
  });

  final String userId;
  final String nickname;
  final String? avatarUrl;
}

/// 社区关联宠物快照。
class CommunityPetSnapshot {
  const CommunityPetSnapshot({
    required this.petId,
    required this.petName,
    required this.petType,
    this.breed,
  });

  final String petId;
  final String petName;
  final String petType;
  final String? breed;
}

/// 社区话题快照。
class CommunityTopicSnapshot {
  const CommunityTopicSnapshot({
    required this.topicId,
    required this.topicName,
    this.topicDesc,
    this.cityCode,
    this.status,
    this.createdAt,
    this.updatedAt,
  });

  final String topicId;
  final String topicName;
  final String? topicDesc;
  final String? cityCode;
  final int? status;
  final DateTime? createdAt;
  final DateTime? updatedAt;
}

/// 社区话题详情快照。
class CommunityTopicDetailSnapshot {
  const CommunityTopicDetailSnapshot({
    required this.topic,
    required this.posts,
  });

  final CommunityTopicSnapshot topic;
  final List<CommunityPostSnapshot> posts;
}

/// 社区评论快照。
class CommunityCommentSnapshot {
  const CommunityCommentSnapshot({
    required this.commentId,
    required this.postId,
    required this.content,
    required this.author,
    this.createdAt,
  });

  final String commentId;
  final String postId;
  final String content;
  final CommunityAuthorSnapshot author;
  final DateTime? createdAt;
}

/// 社区问答详情快照。
class CommunityQuestionDetailSnapshot {
  const CommunityQuestionDetailSnapshot({
    required this.question,
    required this.answers,
  });

  final CommunityPostSnapshot question;
  final List<CommunityCommentSnapshot> answers;
}

/// 当前用户对社区作者的关注状态。
class CommunityFollowStatusSnapshot {
  const CommunityFollowStatusSnapshot({
    required this.followedUserId,
    required this.following,
  });

  final String followedUserId;
  final bool following;
}
