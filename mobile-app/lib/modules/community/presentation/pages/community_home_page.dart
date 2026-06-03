import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_loading.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/community/presentation/pages/community_post_detail_page.dart';
import 'package:petlife_mobile_app/modules/community/presentation/pages/community_post_editor_page.dart';
import 'package:petlife_mobile_app/modules/community/presentation/pages/community_question_detail_page.dart';
import 'package:petlife_mobile_app/modules/community/presentation/pages/community_topic_page.dart';
import 'package:petlife_mobile_app/modules/community/presentation/widgets/community_post_card.dart';
import 'package:petlife_mobile_app/modules/community/presentation/widgets/community_review_status.dart';
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
    _CommunityTab(key: 'mine', label: '我的'),
  ];

  bool _didLoad = false;
  String _selectedTab = 'recommended';
  final Set<String> _loadingTabs = <String>{};
  final Map<String, List<CommunityPostSnapshot>> _postsByTab =
      <String, List<CommunityPostSnapshot>>{};
  final Map<String, String> _errorMessagesByTab = <String, String>{};

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadFeed();
  }

  Future<void> _loadFeed({bool forceRefresh = false}) async {
    final String tab = _selectedTab;
    if (!forceRefresh && _postsByTab.containsKey(tab)) {
      return;
    }
    setState(() {
      _loadingTabs.add(tab);
      _errorMessagesByTab.remove(tab);
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final List<CommunityPostSnapshot> posts = tab == 'mine'
          ? await repository.listMyCommunityPosts()
          : await repository.listCommunityFeed(tab: tab);
      if (!mounted) {
        return;
      }
      setState(() {
        _postsByTab[tab] = posts;
        _errorMessagesByTab.remove(tab);
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _errorMessagesByTab[tab] = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          _loadingTabs.remove(tab);
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
    });
    _loadFeed();
  }

  Future<void> _openPostDetail(CommunityPostSnapshot post) async {
    if (_selectedTab == 'mine' && isCommunityPostRejected(post)) {
      await _openEditorForPost(post);
      return;
    }
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => post.postType == 'qa'
            ? CommunityQuestionDetailPage(questionId: post.postId)
            : CommunityPostDetailPage(postId: post.postId),
      ),
    );
  }

  Future<void> _openEditorForPost(CommunityPostSnapshot post) async {
    final CommunityPostSnapshot? updatedPost =
        await Navigator.of(context).push<CommunityPostSnapshot>(
      MaterialPageRoute<CommunityPostSnapshot>(
        builder: (_) => CommunityPostEditorPage(editingPost: post),
      ),
    );
    if (!mounted || updatedPost == null) {
      return;
    }
    showCompanionFeedback(
      context,
      message: communityReviewStatusMessage(updatedPost.reviewStatus),
      tone: communityReviewFeedbackTone(updatedPost.reviewStatus),
    );
    _postsByTab.remove('mine');
    await _loadFeed(forceRefresh: true);
  }

  Future<void> _openPublisher() async {
    final CommunityPostSnapshot? createdPost =
        await Navigator.of(context).push<CommunityPostSnapshot>(
      MaterialPageRoute<CommunityPostSnapshot>(
        builder: (_) => const CommunityPostEditorPage(),
      ),
    );
    if (!mounted || createdPost == null) {
      return;
    }

    if (createdPost.reviewStatus != 'approved') {
      showCompanionFeedback(
        context,
        message: communityReviewStatusMessage(createdPost.reviewStatus),
        tone: communityReviewFeedbackTone(createdPost.reviewStatus),
      );
    }

    setState(() {
      _selectedTab = createdPost.postType == 'qa' ? 'qa' : 'recommended';
      _postsByTab.remove(_selectedTab);
    });
    await _loadFeed(forceRefresh: true);
  }

  Future<void> _openTopic(CommunityTopicSnapshot topic) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => CommunityTopicPage(topicId: topic.topicId),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final List<CommunityPostSnapshot> currentPosts =
        (_postsByTab[_selectedTab] ?? const <CommunityPostSnapshot>[])
            .where((CommunityPostSnapshot post) =>
                _selectedTab == 'mine' || !isCommunityPostRejected(post))
            .toList();
    final String? currentError = _errorMessagesByTab[_selectedTab];
    final bool isCurrentTabLoading = _loadingTabs.contains(_selectedTab);

    return RefreshIndicator(
      onRefresh: () => _loadFeed(forceRefresh: true),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _CommunityHeroSection(
            selectedTab: _selectedTab,
            postCount: currentPosts.length,
            onCreatePost: _openPublisher,
          ),
          const SizedBox(height: 16),
          PageSection(
            title: '内容分类',
            description: '',
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
            title: _labelOf(_selectedTab),
            description: _descriptionOf(_selectedTab),
            child: _buildFeed(
              posts: currentPosts,
              errorMessage: currentError,
              isLoading: isCurrentTabLoading,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFeed({
    required List<CommunityPostSnapshot> posts,
    required String? errorMessage,
    required bool isLoading,
  }) {
    if (isLoading && posts.isEmpty) {
      return const CompanionSkeletonList(
        itemCount: 3,
      );
    }

    if (errorMessage != null && posts.isEmpty) {
      return CompanionEmptyState(
        title: '${_labelOf(_selectedTab)}暂时没有加载出来',
        description: errorMessage,
        icon: Icons.cloud_off_outlined,
        actionLabel: '重新加载',
        onAction: () => _loadFeed(forceRefresh: true),
      );
    }

    if (posts.isEmpty) {
      return CompanionEmptyState(
        title: _emptyTitleOf(_selectedTab),
        description: _emptyDescriptionOf(_selectedTab),
        icon: _iconOf(_selectedTab),
      );
    }

    return Column(
      children: posts
          .map(
            (CommunityPostSnapshot post) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: CommunityPostCard(
                post: post,
                onTap: () => _openPostDetail(post),
                onTopicTap: _openTopic,
                showRejected: _selectedTab == 'mine',
                actionLabel:
                    _selectedTab == 'mine' && isCommunityPostRejected(post)
                        ? '编辑重提'
                        : null,
                onAction:
                    _selectedTab == 'mine' && isCommunityPostRejected(post)
                        ? () => _openEditorForPost(post)
                        : null,
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
    required this.onCreatePost,
  });

  final String selectedTab;
  final int postCount;
  final VoidCallback onCreatePost;

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
            label: _labelOf(selectedTab),
            icon: Icons.favorite_border_rounded,
            backgroundColor: const Color(0xFFFFE0CF),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(
            '社区',
            style: Theme.of(context).textTheme.headlineSmall,
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
                label: '公开内容',
                backgroundColor: AppThemePalette.surface,
              ),
            ],
          ),
          const SizedBox(height: 18),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: onCreatePost,
              icon: const Icon(Icons.edit_note_rounded),
              label: const Text('发布社区内容'),
            ),
          ),
        ],
      ),
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

String _labelOf(String tabKey) {
  switch (tabKey) {
    case 'following':
      return '关注内容';
    case 'city':
      return '同城内容';
    case 'qa':
      return '问答内容';
    case 'mine':
      return '我的发布';
    case 'recommended':
    default:
      return '推荐内容';
  }
}

String _descriptionOf(String tabKey) {
  switch (tabKey) {
    case 'following':
      return '';
    case 'city':
      return '';
    case 'qa':
      return '';
    case 'mine':
      return '';
    case 'recommended':
    default:
      return '';
  }
}

String _emptyTitleOf(String tabKey) {
  switch (tabKey) {
    case 'following':
      return '关注内容还不多';
    case 'city':
      return '同城还没有新的分享';
    case 'qa':
      return '问答还在慢慢积累';
    case 'mine':
      return '还没有发布内容';
    case 'recommended':
    default:
      return '还没有公开的社区内容';
  }
}

String _emptyDescriptionOf(String tabKey) {
  switch (tabKey) {
    case 'following':
      return '';
    case 'city':
      return '';
    case 'qa':
      return '';
    case 'mine':
      return '';
    case 'recommended':
    default:
      return '';
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
    case 'mine':
      return Icons.person_pin_outlined;
    case 'recommended':
    default:
      return Icons.forum_outlined;
  }
}
