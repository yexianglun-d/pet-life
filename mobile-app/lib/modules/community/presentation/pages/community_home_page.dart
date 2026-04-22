import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/community/presentation/pages/community_post_detail_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/community_post_snapshot.dart';

/// 社区首页。
class CommunityHomePage extends StatefulWidget {
  const CommunityHomePage({super.key});

  @override
  State<CommunityHomePage> createState() => _CommunityHomePageState();
}

class _CommunityHomePageState extends State<CommunityHomePage> {
  static const List<_CommunityTab> _tabs = <_CommunityTab>[
    _CommunityTab(key: 'recommended', label: '推荐'),
    _CommunityTab(key: 'following', label: '关注'),
    _CommunityTab(key: 'city', label: '同城'),
    _CommunityTab(key: 'qa', label: '问答'),
  ];

  bool _didLoad = false;
  bool _isLoading = false;
  String _selectedTab = 'recommended';
  String? _errorMessage;
  List<CommunityPostSnapshot> _posts = const <CommunityPostSnapshot>[];

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadFeed();
  }

  Future<void> _loadFeed() async {
    if (_selectedTab != 'recommended') {
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final List<CommunityPostSnapshot> posts =
          await repository.listCommunityFeed();
      if (!mounted) {
        return;
      }
      setState(() {
        _posts = posts;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _errorMessage = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _selectTab(String tab) {
    if (_selectedTab == tab) {
      return;
    }
    setState(() {
      _selectedTab = tab;
      _errorMessage = null;
    });
    if (tab == 'recommended') {
      _loadFeed();
    }
  }

  Future<void> _openPostDetail(CommunityPostSnapshot post) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => CommunityPostDetailPage(postId: post.postId),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _loadFeed,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _CommunityHeroSection(
            selectedTab: _selectedTab,
            postCount: _posts.length,
          ),
          const SizedBox(height: 16),
          PageSection(
            title: '去看看大家在分享什么',
            description: '每一条公开内容，都来自真实的宠物日常和认真记录的陪伴瞬间。',
            child: Wrap(
              spacing: 10,
              runSpacing: 10,
              children: _tabs
                  .map(
                    (_CommunityTab tab) => _CommunityTabChip(
                      label: tab.label,
                      selected: _selectedTab == tab.key,
                      onTap: () => _selectTab(tab.key),
                    ),
                  )
                  .toList(),
            ),
          ),
          const SizedBox(height: 16),
          PageSection(
            title:
                _selectedTab == 'recommended' ? '推荐内容' : _labelOf(_selectedTab),
            description: _selectedTab == 'recommended'
                ? '这些内容按发布时间整理，适合慢慢翻、慢慢看。'
                : '先在推荐里认识更多毛孩子，其他内容入口也会陆续变得热闹。',
            child: _selectedTab == 'recommended'
                ? _buildRecommendedFeed(context)
                : CompanionEmptyState(
                    title: '${_labelOf(_selectedTab)}正在慢慢热闹起来',
                    description: '先去推荐里看看公开的萌宠日常，也许会遇到和你一样认真生活的人。',
                    icon: _iconOf(_selectedTab),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildRecommendedFeed(BuildContext context) {
    if (_isLoading && _posts.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 20),
          child: CircularProgressIndicator(),
        ),
      );
    }

    if (_errorMessage != null && _posts.isEmpty) {
      return CompanionEmptyState(
        title: '社区暂时没有加载出来',
        description: _errorMessage!,
        icon: Icons.cloud_off_outlined,
        actionLabel: '重新加载',
        onAction: _loadFeed,
      );
    }

    if (_posts.isEmpty) {
      return const CompanionEmptyState(
        title: '还没有公开的社区内容',
        description: '等第一条同步到社区的萌宠日常出现，这里就会慢慢热闹起来。',
        icon: Icons.forum_outlined,
      );
    }

    return Column(
      children: _posts
          .map(
            (CommunityPostSnapshot post) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _CommunityPostCard(
                post: post,
                onTap: () => _openPostDetail(post),
              ),
            ),
          )
          .toList(),
    );
  }
}

class _CommunityHeroSection extends StatelessWidget {
  const _CommunityHeroSection({
    required this.selectedTab,
    required this.postCount,
  });

  final String selectedTab;
  final int postCount;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFEEE2),
          Color(0xFFFFFAF5),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CompanionPill(
            label: selectedTab == 'recommended'
                ? '推荐正在更新'
                : '${_labelOf(selectedTab)}等你来逛',
            icon: Icons.favorite_border_rounded,
            backgroundColor: const Color(0xFFFFE0CF),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(
            '在这里看看别人和毛孩子的生活',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 10),
          Text(
            '公开的日常、温柔的评论和认真记录的瞬间，会让社区慢慢长出真实的陪伴感。',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              CompanionPill(
                label: '当前内容 $postCount',
                backgroundColor: AppThemePalette.surface,
              ),
              const CompanionPill(
                label: '来自公开日常',
                backgroundColor: AppThemePalette.surface,
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _CommunityPostCard extends StatelessWidget {
  const _CommunityPostCard({
    required this.post,
    required this.onTap,
  });

  final CommunityPostSnapshot post;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

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
                Container(
                  width: 42,
                  height: 42,
                  decoration: BoxDecoration(
                    color: AppThemePalette.warmTint,
                    borderRadius: BorderRadius.circular(15),
                  ),
                  child: Center(
                    child: Text(
                      post.author.nickname.isEmpty
                          ? '宠'
                          : post.author.nickname.substring(0, 1),
                      style: textTheme.titleMedium?.copyWith(
                        color: AppThemePalette.primaryDeep,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${post.author.nickname}${post.pet == null ? '' : ' · ${post.pet!.petName}'}',
                        style: textTheme.titleMedium,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _formatFeedTime(post.publishedAt ?? post.createdAt),
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
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _FeedChip(label: '点赞 ${post.likeCount}'),
                _FeedChip(label: '评论 ${post.commentCount}'),
                _FeedChip(label: '收藏 ${post.favoriteCount}'),
                if (post.sourceDailyLogId != null)
                  const _FeedChip(label: '来自萌宠日常'),
              ],
            ),
          ],
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

class _CommunityTabChip extends StatelessWidget {
  const _CommunityTabChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(999),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        decoration: BoxDecoration(
          color: selected ? AppThemePalette.warmTint : AppThemePalette.surface,
          borderRadius: BorderRadius.circular(999),
          border: Border.all(
            color: selected ? AppThemePalette.rose : AppThemePalette.line,
          ),
        ),
        child: Text(
          label,
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: selected
                    ? AppThemePalette.primaryDeep
                    : AppThemePalette.body,
              ),
        ),
      ),
    );
  }
}

class _CommunityTab {
  const _CommunityTab({
    required this.key,
    required this.label,
  });

  final String key;
  final String label;
}

String _formatFeedTime(DateTime? value) {
  if (value == null) {
    return '刚刚';
  }
  final DateTime localValue = value.toLocal();
  return '${localValue.month.toString().padLeft(2, '0')}-${localValue.day.toString().padLeft(2, '0')} '
      '${localValue.hour.toString().padLeft(2, '0')}:${localValue.minute.toString().padLeft(2, '0')}';
}

String _labelOf(String tabKey) {
  switch (tabKey) {
    case 'following':
      return '关注';
    case 'city':
      return '同城';
    case 'qa':
      return '问答';
    case 'recommended':
    default:
      return '推荐';
  }
}

IconData _iconOf(String tabKey) {
  switch (tabKey) {
    case 'following':
      return Icons.people_alt_outlined;
    case 'city':
      return Icons.location_on_outlined;
    case 'qa':
      return Icons.chat_bubble_outline_rounded;
    case 'recommended':
    default:
      return Icons.forum_outlined;
  }
}
