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
      publishedAt: publishedAt ?? this.publishedAt,
      createdAt: createdAt ?? this.createdAt,
      author: author ?? this.author,
      pet: pet ?? this.pet,
    );
  }
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
