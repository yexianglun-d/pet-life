import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_loading.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/community/presentation/pages/community_post_detail_page.dart';
import 'package:petlife_mobile_app/modules/community/presentation/pages/community_post_editor_page.dart';
import 'package:petlife_mobile_app/modules/community/presentation/pages/community_question_detail_page.dart';
import 'package:petlife_mobile_app/modules/community/presentation/widgets/community_post_card.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/community_post_snapshot.dart';

/// 社区话题页。
class CommunityTopicPage extends StatefulWidget {
  const CommunityTopicPage({
    super.key,
    required this.topicId,
  });

  final String topicId;

  @override
  State<CommunityTopicPage> createState() => _CommunityTopicPageState();
}

class _CommunityTopicPageState extends State<CommunityTopicPage> {
  bool _didLoad = false;
  bool _isLoading = false;
  String? _errorMessage;
  CommunityTopicDetailSnapshot? _detail;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadTopic();
  }

  Future<void> _loadTopic() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final CommunityTopicDetailSnapshot detail =
          await repository.getCommunityTopic(widget.topicId);
      if (!mounted) {
        return;
      }
      setState(() {
        _detail = detail;
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

  Future<void> _openPost(CommunityPostSnapshot post) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => post.postType == 'qa'
            ? CommunityQuestionDetailPage(questionId: post.postId)
            : CommunityPostDetailPage(postId: post.postId),
      ),
    );
  }

  Future<void> _openPublisher(CommunityTopicSnapshot topic) async {
    final CommunityPostSnapshot? createdPost =
        await Navigator.of(context).push<CommunityPostSnapshot>(
      MaterialPageRoute<CommunityPostSnapshot>(
        builder: (_) => CommunityPostEditorPage(initialTopic: topic),
      ),
    );
    if (!mounted || createdPost == null) {
      return;
    }
    await _loadTopic();
  }

  @override
  Widget build(BuildContext context) {
    final CommunityTopicDetailSnapshot? detail = _detail;

    if (_isLoading && detail == null) {
      return const Scaffold(
        body: CompanionPageLoading(
          title: '正在整理话题页',
          description: '话题信息和相关帖子会一起加载出来。',
          icon: Icons.tag_rounded,
          layout: CompanionLoadingLayout.list,
        ),
      );
    }

    if (_errorMessage != null && detail == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('话题')),
        body: Padding(
          padding: const EdgeInsets.all(24),
          child: CompanionEmptyState(
            title: '话题暂时没有加载出来',
            description: _errorMessage!,
            icon: Icons.cloud_off_outlined,
            actionLabel: '重新加载',
            onAction: _loadTopic,
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: Text('# ${detail!.topic.topicName}')),
      body: RefreshIndicator(
        onRefresh: _loadTopic,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _TopicHeroCard(
              topic: detail.topic,
              postCount: detail.posts.length,
              onPublish: () => _openPublisher(detail.topic),
            ),
            const SizedBox(height: 16),
            if (_errorMessage != null) ...[
              CompanionFormNotice(
                message: _errorMessage!,
                tone: CompanionFeedbackTone.warning,
              ),
              const SizedBox(height: 12),
            ],
            if (detail.posts.isEmpty)
              CompanionEmptyState(
                title: '这个话题还在等第一条分享',
                description: '围绕真实经历写一条内容，话题页就会慢慢长出更多经验。',
                icon: Icons.forum_outlined,
                actionLabel: '参与话题',
                onAction: () => _openPublisher(detail.topic),
              )
            else
              ...detail.posts.map(
                (CommunityPostSnapshot post) => Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: CommunityPostCard(
                    post: post,
                    onTap: () => _openPost(post),
                    onTopicTap: (_) {},
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _TopicHeroCard extends StatelessWidget {
  const _TopicHeroCard({
    required this.topic,
    required this.postCount,
    required this.onPublish,
  });

  final CommunityTopicSnapshot topic;
  final int postCount;
  final VoidCallback onPublish;

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
            label: '话题',
            icon: Icons.tag_rounded,
            backgroundColor: AppThemePalette.surface,
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text('# ${topic.topicName}',
              style: Theme.of(context).textTheme.titleLarge),
          if (topic.topicDesc != null) ...[
            const SizedBox(height: 8),
            Text(
              topic.topicDesc!,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ],
          const SizedBox(height: 16),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              CompanionPill(
                label: '相关帖子 $postCount',
                backgroundColor: AppThemePalette.surface,
              ),
              if (topic.cityCode != null)
                CompanionPill(
                  label: '城市 ${topic.cityCode}',
                  icon: Icons.location_on_outlined,
                  backgroundColor: AppThemePalette.surface,
                ),
            ],
          ),
          const SizedBox(height: 18),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: onPublish,
              icon: const Icon(Icons.edit_note_rounded),
              label: const Text('参与话题'),
            ),
          ),
        ],
      ),
    );
  }
}
