import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_loading.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/community/presentation/pages/community_post_editor_page.dart';
import 'package:petlife_mobile_app/modules/community/presentation/pages/community_topic_page.dart';
import 'package:petlife_mobile_app/modules/community/presentation/widgets/community_author_follow_button.dart';
import 'package:petlife_mobile_app/modules/community/presentation/widgets/community_media_preview_grid.dart';
import 'package:petlife_mobile_app/modules/community/presentation/widgets/community_review_status.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/community_post_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/community_report_draft.dart';

/// 社区帖子详情页。
class CommunityPostDetailPage extends StatefulWidget {
  const CommunityPostDetailPage({
    super.key,
    required this.postId,
  });

  final String postId;

  @override
  State<CommunityPostDetailPage> createState() =>
      _CommunityPostDetailPageState();
}

class _CommunityPostDetailPageState extends State<CommunityPostDetailPage> {
  static const List<_ReportReasonOption> _reportReasonOptions =
      <_ReportReasonOption>[
    _ReportReasonOption(code: 'spam', label: '广告引流'),
    _ReportReasonOption(code: 'pornography', label: '低俗色情'),
    _ReportReasonOption(code: 'harassment', label: '攻击辱骂'),
    _ReportReasonOption(code: 'illegal', label: '违法违规'),
    _ReportReasonOption(code: 'other', label: '其他原因'),
  ];

  bool _didLoad = false;
  bool _isLoading = false;
  bool _isUpdatingLike = false;
  bool _isUpdatingFavorite = false;
  bool _isSubmittingComment = false;
  bool _isSubmittingReport = false;
  String? _errorMessage;
  CommunityPostSnapshot? _post;
  List<CommunityCommentSnapshot> _comments = const <CommunityCommentSnapshot>[];

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadPostDetail();
  }

  Future<void> _loadPostDetail() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final CommunityPostSnapshot post =
          await repository.getCommunityPost(widget.postId);
      final List<CommunityCommentSnapshot> comments =
          await repository.listCommunityComments(widget.postId);
      if (!mounted) {
        return;
      }

      setState(() {
        _post = post;
        _comments = comments;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        _errorMessage = communityContentUnavailableMessage(
          error,
          contentLabel: '社区内容',
        );
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _toggleLike() async {
    final CommunityPostSnapshot? post = _post;
    if (post == null || _isUpdatingLike) {
      return;
    }

    setState(() {
      _isUpdatingLike = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final CommunityPostSnapshot updatedPost = post.liked
          ? await repository.unlikeCommunityPost(post.postId)
          : await repository.likeCommunityPost(post.postId);
      if (!mounted) {
        return;
      }

      setState(() {
        _post = updatedPost;
      });
      showCompanionSuccessFeedback(
        context,
        updatedPost.liked ? '已点赞' : '已取消点赞',
      );
    } catch (error) {
      if (!mounted) {
        return;
      }

      showCompanionErrorFeedback(context, error.toString());
    } finally {
      if (mounted) {
        setState(() {
          _isUpdatingLike = false;
        });
      }
    }
  }

  Future<void> _toggleFavorite() async {
    final CommunityPostSnapshot? post = _post;
    if (post == null || _isUpdatingFavorite) {
      return;
    }

    setState(() {
      _isUpdatingFavorite = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final CommunityPostSnapshot updatedPost = post.favorited
          ? await repository.unfavoriteCommunityPost(post.postId)
          : await repository.favoriteCommunityPost(post.postId);
      if (!mounted) {
        return;
      }

      setState(() {
        _post = updatedPost;
      });
      showCompanionSuccessFeedback(
        context,
        updatedPost.favorited ? '已收藏' : '已取消收藏',
      );
    } catch (error) {
      if (!mounted) {
        return;
      }

      showCompanionErrorFeedback(context, error.toString());
    } finally {
      if (mounted) {
        setState(() {
          _isUpdatingFavorite = false;
        });
      }
    }
  }

  Future<void> _openCommentComposer() async {
    if (_isSubmittingComment) {
      return;
    }

    final String? content = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      builder: (BuildContext context) {
        final TextEditingController controller = TextEditingController();

        return Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 20,
            bottom: MediaQuery.of(context).viewInsets.bottom + 20,
          ),
          child: StatefulBuilder(
            builder: (BuildContext context, StateSetter setModalState) {
              return Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('写评论', style: Theme.of(context).textTheme.titleLarge),
                  const SizedBox(height: 14),
                  TextField(
                    controller: controller,
                    minLines: 3,
                    maxLines: 5,
                    decoration: const InputDecoration(
                      hintText: '写下你的评论',
                    ),
                    onChanged: (_) => setModalState(() {}),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      TextButton(
                        onPressed: () => Navigator.of(context).pop(),
                        child: const Text('取消'),
                      ),
                      const Spacer(),
                      FilledButton(
                        onPressed: controller.text.trim().isEmpty
                            ? null
                            : () => Navigator.of(context)
                                .pop(controller.text.trim()),
                        child: const Text('发布评论'),
                      ),
                    ],
                  ),
                ],
              );
            },
          ),
        );
      },
    );

    if (!mounted || content == null || content.isEmpty) {
      return;
    }

    setState(() {
      _isSubmittingComment = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final CommunityCommentSnapshot createdComment =
          await repository.createCommunityComment(
        postId: widget.postId,
        content: content,
      );
      if (!mounted) {
        return;
      }

      setState(() {
        _comments = <CommunityCommentSnapshot>[
          ..._comments,
          createdComment,
        ];
        _post = _post?.copyWith(
          commentCount: (_post?.commentCount ?? 0) + 1,
        );
      });
      showCompanionSuccessFeedback(context, '评论已发布');
    } catch (error) {
      if (!mounted) {
        return;
      }

      showCompanionErrorFeedback(context, error.toString());
    } finally {
      if (mounted) {
        setState(() {
          _isSubmittingComment = false;
        });
      }
    }
  }

  Future<void> _openReportComposer() async {
    if (_isSubmittingReport) {
      return;
    }

    final CommunityReportDraft? draft =
        await showModalBottomSheet<CommunityReportDraft>(
      context: context,
      isScrollControlled: true,
      builder: (BuildContext context) {
        final TextEditingController detailController = TextEditingController();
        String? selectedReasonCode;

        return StatefulBuilder(
          builder: (BuildContext context, StateSetter setModalState) {
            final bool shouldRequireDetail = selectedReasonCode == 'other';
            final bool canSubmit = selectedReasonCode != null &&
                (!shouldRequireDetail ||
                    detailController.text.trim().isNotEmpty);

            return Padding(
              padding: EdgeInsets.only(
                left: 16,
                right: 16,
                top: 20,
                bottom: MediaQuery.of(context).viewInsets.bottom + 20,
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('举报内容', style: Theme.of(context).textTheme.titleLarge),
                  const SizedBox(height: 12),
                  RadioGroup<String>(
                    groupValue: selectedReasonCode,
                    onChanged: (String? value) {
                      setModalState(() {
                        selectedReasonCode = value;
                      });
                    },
                    child: Column(
                      children: _reportReasonOptions
                          .map(
                            (_ReportReasonOption option) =>
                                RadioListTile<String>(
                              value: option.code,
                              contentPadding: EdgeInsets.zero,
                              title: Text(option.label),
                            ),
                          )
                          .toList(),
                    ),
                  ),
                  TextField(
                    controller: detailController,
                    minLines: 3,
                    maxLines: 5,
                    decoration: InputDecoration(
                      hintText: shouldRequireDetail
                          ? '请补充具体问题，方便人工核查。'
                          : '可补充问题细节，帮助提升处理准确度。',
                    ),
                    onChanged: (_) => setModalState(() {}),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      TextButton(
                        onPressed: () => Navigator.of(context).pop(),
                        child: const Text('取消'),
                      ),
                      const Spacer(),
                      FilledButton(
                        onPressed: canSubmit
                            ? () => Navigator.of(context).pop(
                                  CommunityReportDraft(
                                    reasonCode: selectedReasonCode!,
                                    reasonDetail:
                                        detailController.text.trim().isEmpty
                                            ? null
                                            : detailController.text.trim(),
                                  ),
                                )
                            : null,
                        child: const Text('提交举报'),
                      ),
                    ],
                  ),
                ],
              ),
            );
          },
        );
      },
    );

    if (!mounted || draft == null) {
      return;
    }

    setState(() {
      _isSubmittingReport = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      await repository.reportCommunityPost(
        postId: widget.postId,
        draft: draft,
      );
      if (!mounted) {
        return;
      }

      showCompanionSuccessFeedback(context, '举报已提交，我们会尽快核查');
    } catch (error) {
      if (!mounted) {
        return;
      }

      showCompanionErrorFeedback(context, error.toString());
    } finally {
      if (mounted) {
        setState(() {
          _isSubmittingReport = false;
        });
      }
    }
  }

  Future<void> _openTopic(CommunityTopicSnapshot topic) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => CommunityTopicPage(topicId: topic.topicId),
      ),
    );
  }

  Future<void> _openEditor() async {
    final CommunityPostSnapshot? post = _post;
    if (post == null) {
      return;
    }
    final CommunityPostSnapshot? updatedPost =
        await Navigator.of(context).push<CommunityPostSnapshot>(
      MaterialPageRoute<CommunityPostSnapshot>(
        builder: (_) => CommunityPostEditorPage(editingPost: post),
      ),
    );
    if (!mounted || updatedPost == null) {
      return;
    }
    setState(() {
      _post = updatedPost;
      _comments = const <CommunityCommentSnapshot>[];
    });
    showCompanionFeedback(
      context,
      message: communityReviewStatusMessage(updatedPost.reviewStatus),
      tone: communityReviewFeedbackTone(updatedPost.reviewStatus),
    );
  }

  @override
  Widget build(BuildContext context) {
    final CommunityPostSnapshot? post = _post;

    if (_isLoading && post == null) {
      return const Scaffold(
        body: CompanionPageLoading(
          title: '正在整理社区内容',
          description: '帖子、评论和互动状态会按详情页结构准备好。',
          icon: Icons.forum_outlined,
          layout: CompanionLoadingLayout.detail,
        ),
      );
    }

    if (_errorMessage != null && post == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('社区内容详情')),
        body: Padding(
          padding: const EdgeInsets.all(24),
          child: CompanionEmptyState(
            title: '社区内容暂时没有加载出来',
            description: _errorMessage!,
            icon: Icons.cloud_off_outlined,
            actionLabel: '重新加载',
            onAction: _loadPostDetail,
          ),
        ),
      );
    }

    final CommunityPostSnapshot visiblePost = post!;

    return Scaffold(
      appBar: AppBar(
        title: const Text('社区内容详情'),
        actions: [
          if (isCommunityPostRejected(visiblePost))
            TextButton.icon(
              onPressed: _openEditor,
              icon: const Icon(Icons.edit_note_outlined),
              label: const Text('编辑重提'),
            )
          else if (visiblePost.reviewStatus == 'approved')
            TextButton.icon(
              onPressed: _isSubmittingReport ? null : _openReportComposer,
              icon: const Icon(Icons.flag_outlined),
              label: Text(_isSubmittingReport ? '提交中' : '举报'),
            ),
          const SizedBox(width: 8),
        ],
      ),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: <Color>[
              Color(0xFFFFFBF7),
              AppThemePalette.background,
            ],
          ),
        ),
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            if (visiblePost.reviewStatus != 'approved') ...[
              CommunityReviewStatusNotice(
                reviewStatus: visiblePost.reviewStatus,
              ),
              const SizedBox(height: 12),
            ],
            _CommunityPostDetailCard(
              post: visiblePost,
              isUpdatingLike: _isUpdatingLike,
              isUpdatingFavorite: _isUpdatingFavorite,
              isSubmittingComment: _isSubmittingComment,
              onToggleLike: _toggleLike,
              onToggleFavorite: _toggleFavorite,
              onComment: _openCommentComposer,
              onTopicTap: _openTopic,
            ),
            const SizedBox(height: 16),
            if (visiblePost.reviewStatus == 'approved')
              _CommunityCommentsSection(
                comments: _comments,
                onComment: _openCommentComposer,
                isSubmittingComment: _isSubmittingComment,
              ),
          ],
        ),
      ),
    );
  }
}

class _ReportReasonOption {
  const _ReportReasonOption({
    required this.code,
    required this.label,
  });

  final String code;
  final String label;
}

class _CommunityPostDetailCard extends StatelessWidget {
  const _CommunityPostDetailCard({
    required this.post,
    required this.isUpdatingLike,
    required this.isUpdatingFavorite,
    required this.isSubmittingComment,
    required this.onToggleLike,
    required this.onToggleFavorite,
    required this.onComment,
    required this.onTopicTap,
  });

  final CommunityPostSnapshot post;
  final bool isUpdatingLike;
  final bool isUpdatingFavorite;
  final bool isSubmittingComment;
  final VoidCallback onToggleLike;
  final VoidCallback onToggleFavorite;
  final VoidCallback onComment;
  final ValueChanged<CommunityTopicSnapshot> onTopicTap;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;
    final CommunityTopicSnapshot? topic = post.topic;

    return CompanionCard(
      padding: const EdgeInsets.all(20),
      radius: 28,
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFECDD),
          Color(0xFFFFFBF6),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              CompanionPill(
                label: post.sourceDailyLogId == null ? '社区内容' : '来自萌宠日常',
                icon: Icons.forum_outlined,
                backgroundColor: AppThemePalette.surface,
              ),
              if (post.reviewStatus != 'approved') ...[
                const SizedBox(width: 8),
                CommunityReviewStatusPill(reviewStatus: post.reviewStatus),
              ],
              const SizedBox(width: 10),
              Flexible(
                child: Align(
                  alignment: Alignment.centerRight,
                  child: CommunityAuthorFollowButton(author: post.author),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(post.title, style: textTheme.titleLarge),
          const SizedBox(height: 12),
          Text(
            '${post.author.nickname} · ${_formatCommunityTime(post.publishedAt ?? post.createdAt)}',
            style: textTheme.bodySmall?.copyWith(
              color: AppThemePalette.muted,
            ),
          ),
          if (post.pet != null) ...[
            const SizedBox(height: 8),
            _InfoChip(label: '${post.pet!.petName} · ${post.pet!.petType}'),
          ],
          if (topic != null) ...[
            const SizedBox(height: 8),
            InkWell(
              onTap: () => onTopicTap(topic),
              borderRadius: BorderRadius.circular(999),
              child: CompanionPill(
                label: '# ${topic.topicName}',
                icon: Icons.tag_rounded,
                backgroundColor: AppThemePalette.surface,
                foregroundColor: AppThemePalette.primaryDeep,
              ),
            ),
          ],
          const SizedBox(height: 20),
          Text(
            post.content,
            style: textTheme.bodyLarge?.copyWith(height: 1.7),
          ),
          if (post.mediaAssets.isNotEmpty || post.mediaAssetIds.isNotEmpty) ...[
            const SizedBox(height: 16),
            CommunityMediaPreviewGrid(
              mediaAssets: post.mediaAssets,
              mediaAssetIds: post.mediaAssetIds,
            ),
          ],
          const SizedBox(height: 20),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _InfoChip(label: '点赞 ${post.likeCount}'),
              _InfoChip(label: '评论 ${post.commentCount}'),
              _InfoChip(label: '收藏 ${post.favoriteCount}'),
            ],
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: isUpdatingLike ? null : onToggleLike,
                  icon: Icon(
                    post.liked ? Icons.thumb_up : Icons.thumb_up_outlined,
                  ),
                  label: Text(isUpdatingLike ? '处理中...' : '点赞'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: isUpdatingFavorite ? null : onToggleFavorite,
                  icon: Icon(
                    post.favorited ? Icons.bookmark : Icons.bookmark_border,
                  ),
                  label: Text(isUpdatingFavorite ? '处理中...' : '收藏'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: FilledButton.tonalIcon(
              onPressed: isSubmittingComment ? null : onComment,
              icon: const Icon(Icons.chat_bubble_outline),
              label: Text(isSubmittingComment ? '发布中...' : '写评论'),
            ),
          ),
        ],
      ),
    );
  }
}

class _CommunityCommentsSection extends StatelessWidget {
  const _CommunityCommentsSection({
    required this.comments,
    required this.onComment,
    required this.isSubmittingComment,
  });

  final List<CommunityCommentSnapshot> comments;
  final VoidCallback onComment;
  final bool isSubmittingComment;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return CompanionCard(
      padding: const EdgeInsets.all(20),
      radius: 28,
      color: AppThemePalette.surface,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text('评论区', style: textTheme.titleMedium),
              const Spacer(),
              TextButton(
                onPressed: isSubmittingComment ? null : onComment,
                child: const Text('发表评论'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (comments.isEmpty)
            Text(
              '还没有评论，先留下第一条真实反馈。',
              style: textTheme.bodyMedium?.copyWith(
                color: AppThemePalette.muted,
              ),
            )
          else
            Column(
              children: comments
                  .map(
                    (CommunityCommentSnapshot comment) => Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: _CommunityCommentCard(comment: comment),
                    ),
                  )
                  .toList(),
            ),
        ],
      ),
    );
  }
}

class _CommunityCommentCard extends StatelessWidget {
  const _CommunityCommentCard({required this.comment});

  final CommunityCommentSnapshot comment;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return CompanionCard(
      padding: const EdgeInsets.all(14),
      radius: 22,
      color: AppThemePalette.surfaceRaised,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '${comment.author.nickname} · ${_formatCommunityTime(comment.createdAt)}',
            style: textTheme.bodySmall?.copyWith(
              color: AppThemePalette.muted,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            comment.content,
            style: textTheme.bodyMedium?.copyWith(height: 1.6),
          ),
        ],
      ),
    );
  }
}

class _InfoChip extends StatelessWidget {
  const _InfoChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return CompanionPill(
      label: label,
      backgroundColor: AppThemePalette.surface,
    );
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
