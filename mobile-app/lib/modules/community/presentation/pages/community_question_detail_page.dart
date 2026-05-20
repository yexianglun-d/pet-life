import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_loading.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/community/presentation/pages/community_topic_page.dart';
import 'package:petlife_mobile_app/modules/community/presentation/widgets/community_author_follow_button.dart';
import 'package:petlife_mobile_app/modules/community/presentation/widgets/community_media_preview_grid.dart';
import 'package:petlife_mobile_app/modules/community/presentation/widgets/community_review_status.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/community_post_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/community_report_draft.dart';

/// 社区问答详情页。
class CommunityQuestionDetailPage extends StatefulWidget {
  const CommunityQuestionDetailPage({
    super.key,
    required this.questionId,
  });

  final String questionId;

  @override
  State<CommunityQuestionDetailPage> createState() =>
      _CommunityQuestionDetailPageState();
}

class _CommunityQuestionDetailPageState
    extends State<CommunityQuestionDetailPage> {
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
  bool _isSubmittingAnswer = false;
  bool _isSubmittingReport = false;
  String? _errorMessage;
  CommunityQuestionDetailSnapshot? _detail;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadQuestionDetail();
  }

  Future<void> _loadQuestionDetail() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final CommunityQuestionDetailSnapshot detail =
          await repository.getCommunityQuestion(widget.questionId);
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
        _errorMessage = communityContentUnavailableMessage(
          error,
          contentLabel: '问答内容',
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
    final CommunityQuestionDetailSnapshot? detail = _detail;
    if (detail == null || _isUpdatingLike) {
      return;
    }

    setState(() {
      _isUpdatingLike = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final CommunityPostSnapshot question = detail.question;
      final CommunityPostSnapshot updatedQuestion = question.liked
          ? await repository.unlikeCommunityPost(question.postId)
          : await repository.likeCommunityPost(question.postId);
      if (!mounted) {
        return;
      }
      setState(() {
        _detail = CommunityQuestionDetailSnapshot(
          question: updatedQuestion,
          answers: detail.answers,
        );
      });
      showCompanionSuccessFeedback(
        context,
        updatedQuestion.liked ? '已点赞' : '已取消点赞',
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
    final CommunityQuestionDetailSnapshot? detail = _detail;
    if (detail == null || _isUpdatingFavorite) {
      return;
    }

    setState(() {
      _isUpdatingFavorite = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final CommunityPostSnapshot question = detail.question;
      final CommunityPostSnapshot updatedQuestion = question.favorited
          ? await repository.unfavoriteCommunityPost(question.postId)
          : await repository.favoriteCommunityPost(question.postId);
      if (!mounted) {
        return;
      }
      setState(() {
        _detail = CommunityQuestionDetailSnapshot(
          question: updatedQuestion,
          answers: detail.answers,
        );
      });
      showCompanionSuccessFeedback(
        context,
        updatedQuestion.favorited ? '已收藏' : '已取消收藏',
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

  Future<void> _openAnswerComposer() async {
    if (_isSubmittingAnswer) {
      return;
    }

    final String? content = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext context) {
        final TextEditingController controller = TextEditingController();
        return Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            bottom: MediaQuery.of(context).viewInsets.bottom + 16,
          ),
          child: CompanionCard(
            padding: const EdgeInsets.all(20),
            radius: 28,
            color: AppThemePalette.surface,
            child: StatefulBuilder(
              builder: (BuildContext context, StateSetter setModalState) {
                return Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('写回答', style: Theme.of(context).textTheme.titleLarge),
                    const SizedBox(height: 10),
                    Text(
                      '把亲身经验、观察和边界讲清楚，会更容易帮到提问的人。',
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: AppThemePalette.muted,
                          ),
                    ),
                    const SizedBox(height: 14),
                    TextField(
                      controller: controller,
                      minLines: 4,
                      maxLines: 7,
                      decoration: const InputDecoration(
                        hintText: '写下你的回答',
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
                          child: const Text('发布回答'),
                        ),
                      ],
                    ),
                  ],
                );
              },
            ),
          ),
        );
      },
    );

    if (!mounted || content == null || content.isEmpty) {
      return;
    }

    setState(() {
      _isSubmittingAnswer = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final CommunityCommentSnapshot answer =
          await repository.createCommunityComment(
        postId: widget.questionId,
        content: content,
      );
      if (!mounted) {
        return;
      }
      final CommunityQuestionDetailSnapshot detail = _detail!;
      setState(() {
        _detail = CommunityQuestionDetailSnapshot(
          question: detail.question.copyWith(
            commentCount: detail.question.commentCount + 1,
          ),
          answers: <CommunityCommentSnapshot>[
            ...detail.answers,
            answer,
          ],
        );
      });
      showCompanionSuccessFeedback(context, '回答已发布');
    } catch (error) {
      if (!mounted) {
        return;
      }
      showCompanionErrorFeedback(context, error.toString());
    } finally {
      if (mounted) {
        setState(() {
          _isSubmittingAnswer = false;
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
      backgroundColor: Colors.transparent,
      builder: (BuildContext context) {
        final TextEditingController detailController = TextEditingController();
        String? selectedReasonCode;
        return Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            bottom: MediaQuery.of(context).viewInsets.bottom + 16,
          ),
          child: CompanionCard(
            padding: const EdgeInsets.all(20),
            radius: 28,
            color: AppThemePalette.surface,
            child: StatefulBuilder(
              builder: (BuildContext context, StateSetter setModalState) {
                final bool shouldRequireDetail = selectedReasonCode == 'other';
                final bool canSubmit = selectedReasonCode != null &&
                    (!shouldRequireDetail ||
                        detailController.text.trim().isNotEmpty);
                return Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('举报问答', style: Theme.of(context).textTheme.titleLarge),
                    const SizedBox(height: 10),
                    Text(
                      '请选择最接近的问题类型，我们会进入人工核查队列。',
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: AppThemePalette.muted,
                          ),
                    ),
                    const SizedBox(height: 12),
                    ..._reportReasonOptions.map(
                      (_ReportReasonOption option) => RadioListTile<String>(
                        value: option.code,
                        groupValue: selectedReasonCode,
                        contentPadding: EdgeInsets.zero,
                        title: Text(option.label),
                        onChanged: (String? value) {
                          setModalState(() {
                            selectedReasonCode = value;
                          });
                        },
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
                );
              },
            ),
          ),
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
        postId: widget.questionId,
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

  @override
  Widget build(BuildContext context) {
    final CommunityQuestionDetailSnapshot? detail = _detail;

    if (_isLoading && detail == null) {
      return const Scaffold(
        body: CompanionPageLoading(
          title: '正在整理问答内容',
          description: '问题、回答和互动状态会按详情页结构准备好。',
          icon: Icons.help_outline_rounded,
          layout: CompanionLoadingLayout.detail,
        ),
      );
    }

    if (_errorMessage != null && detail == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('问答详情')),
        body: Padding(
          padding: const EdgeInsets.all(24),
          child: CompanionEmptyState(
            title: '问答暂时没有加载出来',
            description: _errorMessage!,
            icon: Icons.cloud_off_outlined,
            actionLabel: '重新加载',
            onAction: _loadQuestionDetail,
          ),
        ),
      );
    }

    final CommunityPostSnapshot question = detail!.question;
    if (isCommunityPostRejected(question)) {
      return Scaffold(
        appBar: AppBar(title: const Text('问答详情')),
        body: const Padding(
          padding: EdgeInsets.all(24),
          child: CompanionEmptyState(
            title: '问答暂时不可见',
            description: '这个问题未通过审核，不会公开展示。',
            icon: Icons.visibility_off_outlined,
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('问答详情'),
        actions: [
          TextButton.icon(
            onPressed: _isSubmittingReport ? null : _openReportComposer,
            icon: const Icon(Icons.flag_outlined),
            label: Text(_isSubmittingReport ? '提交中' : '举报'),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadQuestionDetail,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            if (isCommunityPostPendingReview(question)) ...[
              CommunityReviewStatusNotice(reviewStatus: question.reviewStatus),
              const SizedBox(height: 12),
            ],
            _QuestionCard(
              question: question,
              isUpdatingLike: _isUpdatingLike,
              isUpdatingFavorite: _isUpdatingFavorite,
              isSubmittingAnswer: _isSubmittingAnswer,
              onToggleLike: _toggleLike,
              onToggleFavorite: _toggleFavorite,
              onAnswer: _openAnswerComposer,
              onTopicTap: _openTopic,
            ),
            const SizedBox(height: 16),
            _AnswerSection(
              answers: detail.answers,
              isSubmittingAnswer: _isSubmittingAnswer,
              onAnswer: _openAnswerComposer,
            ),
          ],
        ),
      ),
    );
  }
}

class _QuestionCard extends StatelessWidget {
  const _QuestionCard({
    required this.question,
    required this.isUpdatingLike,
    required this.isUpdatingFavorite,
    required this.isSubmittingAnswer,
    required this.onToggleLike,
    required this.onToggleFavorite,
    required this.onAnswer,
    required this.onTopicTap,
  });

  final CommunityPostSnapshot question;
  final bool isUpdatingLike;
  final bool isUpdatingFavorite;
  final bool isSubmittingAnswer;
  final VoidCallback onToggleLike;
  final VoidCallback onToggleFavorite;
  final VoidCallback onAnswer;
  final ValueChanged<CommunityTopicSnapshot> onTopicTap;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;
    final CommunityTopicSnapshot? topic = question.topic;
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
              const CompanionPill(
                label: '问答',
                icon: Icons.help_outline_rounded,
                backgroundColor: AppThemePalette.surface,
              ),
              if (question.reviewStatus != 'approved') ...[
                const SizedBox(width: 8),
                CommunityReviewStatusPill(reviewStatus: question.reviewStatus),
              ],
              const SizedBox(width: 10),
              Flexible(
                child: Align(
                  alignment: Alignment.centerRight,
                  child: CommunityAuthorFollowButton(author: question.author),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Text(question.title, style: textTheme.titleLarge),
          const SizedBox(height: 10),
          Text(
            '${question.author.nickname} · ${_formatCommunityTime(question.publishedAt ?? question.createdAt)}',
            style: textTheme.bodySmall?.copyWith(
              color: AppThemePalette.muted,
            ),
          ),
          if (topic != null) ...[
            const SizedBox(height: 10),
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
          const SizedBox(height: 18),
          Text(
            question.content,
            style: textTheme.bodyLarge?.copyWith(height: 1.7),
          ),
          if (question.mediaAssets.isNotEmpty ||
              question.mediaAssetIds.isNotEmpty) ...[
            const SizedBox(height: 16),
            CommunityMediaPreviewGrid(
              mediaAssets: question.mediaAssets,
              mediaAssetIds: question.mediaAssetIds,
            ),
          ],
          const SizedBox(height: 18),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _InfoChip(label: '点赞 ${question.likeCount}'),
              _InfoChip(label: '回答 ${question.commentCount}'),
              _InfoChip(label: '收藏 ${question.favoriteCount}'),
            ],
          ),
          const SizedBox(height: 18),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: isUpdatingLike ? null : onToggleLike,
                  icon: Icon(
                    question.liked ? Icons.thumb_up : Icons.thumb_up_outlined,
                  ),
                  label: Text(isUpdatingLike ? '处理中...' : '点赞'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: isUpdatingFavorite ? null : onToggleFavorite,
                  icon: Icon(
                    question.favorited ? Icons.bookmark : Icons.bookmark_border,
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
              onPressed: isSubmittingAnswer ? null : onAnswer,
              icon: const Icon(Icons.chat_bubble_outline),
              label: Text(isSubmittingAnswer ? '发布中...' : '写回答'),
            ),
          ),
        ],
      ),
    );
  }
}

class _AnswerSection extends StatelessWidget {
  const _AnswerSection({
    required this.answers,
    required this.isSubmittingAnswer,
    required this.onAnswer,
  });

  final List<CommunityCommentSnapshot> answers;
  final bool isSubmittingAnswer;
  final VoidCallback onAnswer;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(20),
      radius: 28,
      color: AppThemePalette.surface,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text('回答', style: Theme.of(context).textTheme.titleMedium),
              const Spacer(),
              TextButton(
                onPressed: isSubmittingAnswer ? null : onAnswer,
                child: const Text('写回答'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (answers.isEmpty)
            CompanionEmptyState(
              title: '还没有回答',
              description: '把你的真实经验补充上来，也许正好能帮到提问的人。',
              icon: Icons.chat_bubble_outline_rounded,
              actionLabel: '写回答',
              onAction: isSubmittingAnswer ? null : onAnswer,
            )
          else
            Column(
              children: answers
                  .map(
                    (CommunityCommentSnapshot answer) => Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: _AnswerCard(answer: answer),
                    ),
                  )
                  .toList(),
            ),
        ],
      ),
    );
  }
}

class _AnswerCard extends StatelessWidget {
  const _AnswerCard({required this.answer});

  final CommunityCommentSnapshot answer;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(14),
      radius: 22,
      color: AppThemePalette.surfaceRaised,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '${answer.author.nickname} · ${_formatCommunityTime(answer.createdAt)}',
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: AppThemePalette.muted,
                ),
          ),
          const SizedBox(height: 8),
          Text(
            answer.content,
            style:
                Theme.of(context).textTheme.bodyMedium?.copyWith(height: 1.6),
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

class _ReportReasonOption {
  const _ReportReasonOption({
    required this.code,
    required this.label,
  });

  final String code;
  final String label;
}

String _formatCommunityTime(DateTime? value) {
  if (value == null) {
    return '刚刚';
  }
  final DateTime localValue = value.toLocal();
  return '${localValue.month.toString().padLeft(2, '0')}-${localValue.day.toString().padLeft(2, '0')} '
      '${localValue.hour.toString().padLeft(2, '0')}:${localValue.minute.toString().padLeft(2, '0')}';
}
