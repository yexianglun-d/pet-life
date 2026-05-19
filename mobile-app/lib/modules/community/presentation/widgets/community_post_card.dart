import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/community/presentation/widgets/community_media_preview_grid.dart';
import 'package:petlife_mobile_app/shared/domain/models/community_post_snapshot.dart';

/// 社区内容流卡片。
class CommunityPostCard extends StatelessWidget {
  const CommunityPostCard({
    super.key,
    required this.post,
    required this.onTap,
    this.onTopicTap,
  });

  final CommunityPostSnapshot post;
  final VoidCallback onTap;
  final ValueChanged<CommunityTopicSnapshot>? onTopicTap;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;
    final CommunityTopicSnapshot? topic = post.topic;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(24),
      child: CompanionCard(
        radius: 24,
        padding: const EdgeInsets.all(16),
        color: AppThemePalette.surfaceRaised,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                _CommunityAvatar(author: post.author),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${post.author.nickname}${post.pet == null ? '' : ' · ${post.pet!.petName}'}',
                        style: textTheme.titleMedium,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _formatCommunityTime(
                          post.publishedAt ?? post.createdAt,
                        ),
                        style: textTheme.bodySmall,
                      ),
                    ],
                  ),
                ),
                if (post.liked)
                  const Icon(
                    Icons.favorite_rounded,
                    color: AppThemePalette.primaryDeep,
                    size: 18,
                  ),
              ],
            ),
            const SizedBox(height: 14),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _FeedChip(label: _postTypeLabel(post.postType)),
                if (topic != null)
                  InkWell(
                    onTap: onTopicTap == null
                        ? null
                        : () => onTopicTap!.call(topic),
                    borderRadius: BorderRadius.circular(999),
                    child: _FeedChip(label: '# ${topic.topicName}'),
                  ),
                if (post.sourceDailyLogId != null)
                  const _FeedChip(label: '来自萌宠日常'),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              post.title,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(
              post.content,
              maxLines: 3,
              overflow: TextOverflow.ellipsis,
              style: textTheme.bodyMedium,
            ),
            if (post.mediaAssets.isNotEmpty ||
                post.mediaAssetIds.isNotEmpty) ...[
              const SizedBox(height: 12),
              CommunityMediaPreviewGrid(
                mediaAssets: post.mediaAssets,
                mediaAssetIds: post.mediaAssetIds,
                compact: true,
              ),
            ],
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _FeedChip(label: '点赞 ${post.likeCount}'),
                _FeedChip(label: '评论 ${post.commentCount}'),
                _FeedChip(label: '收藏 ${post.favoriteCount}'),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _CommunityAvatar extends StatelessWidget {
  const _CommunityAvatar({required this.author});

  final CommunityAuthorSnapshot author;

  @override
  Widget build(BuildContext context) {
    final String avatarLabel =
        author.nickname.isEmpty ? '宠' : author.nickname.substring(0, 1);
    return Container(
      width: 42,
      height: 42,
      decoration: BoxDecoration(
        color: AppThemePalette.warmTint,
        borderRadius: BorderRadius.circular(15),
      ),
      child: Center(
        child: Text(
          avatarLabel,
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                color: AppThemePalette.primaryDeep,
              ),
        ),
      ),
    );
  }
}

class _FeedChip extends StatelessWidget {
  const _FeedChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return CompanionPill(
      label: label,
      backgroundColor: AppThemePalette.surface,
    );
  }
}

String _postTypeLabel(String postType) {
  switch (postType) {
    case 'qa':
      return '问答';
    case 'video':
      return '视频';
    case 'experience':
      return '经验';
    case 'image_text':
    default:
      return '图文';
  }
}

String _formatCommunityTime(DateTime? value) {
  if (value == null) {
    return '刚刚';
  }
  final DateTime localValue = value.toLocal();
  return '${localValue.month.toString().padLeft(2, '0')}-${localValue.day.toString().padLeft(2, '0')} '
      '${localValue.hour.toString().padLeft(2, '0')}:${localValue.minute.toString().padLeft(2, '0')}';
}
