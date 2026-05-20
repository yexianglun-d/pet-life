import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_loading.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/media_attachment_picker.dart';
import 'package:petlife_mobile_app/modules/community/presentation/widgets/community_review_status.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/community_post_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_detail_snapshot.dart';

/// 独立社区发布页。
class CommunityPostEditorPage extends StatefulWidget {
  const CommunityPostEditorPage({
    super.key,
    this.initialTopic,
    this.initialPostType = 'image_text',
  });

  final CommunityTopicSnapshot? initialTopic;
  final String initialPostType;

  @override
  State<CommunityPostEditorPage> createState() =>
      _CommunityPostEditorPageState();
}

class _CommunityPostEditorPageState extends State<CommunityPostEditorPage> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _titleController = TextEditingController();
  final TextEditingController _contentController = TextEditingController();

  bool _didLoad = false;
  bool _isLoading = false;
  bool _isSubmitting = false;
  bool _isMediaUploading = false;
  bool _hasFailedMedia = false;
  String? _loadErrorMessage;
  String? _formNoticeMessage;
  String? _selectedPetId;
  late String _selectedPostType;
  String _visibility = 'public';
  List<String> _mediaAssetIds = const <String>[];
  CurrentUserSnapshot? _currentUser;
  List<PetDetailSnapshot> _pets = const <PetDetailSnapshot>[];

  @override
  void initState() {
    super.initState();
    _selectedPostType = widget.initialPostType;
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadComposerData();
  }

  @override
  void dispose() {
    _titleController.dispose();
    _contentController.dispose();
    super.dispose();
  }

  Future<void> _loadComposerData() async {
    setState(() {
      _isLoading = true;
      _loadErrorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final CurrentUserSnapshot currentUser = await repository.getCurrentUser();
      final List<PetDetailSnapshot> pets = await repository.listPets();
      if (!mounted) {
        return;
      }
      setState(() {
        _currentUser = currentUser;
        _pets = pets
            .where((PetDetailSnapshot pet) => pet.status == 'active')
            .toList();
        _selectedPetId = currentUser.currentPetId;
        if (_selectedPetId != null &&
            !_pets
                .any((PetDetailSnapshot pet) => pet.petId == _selectedPetId)) {
          _selectedPetId = null;
        }
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _loadErrorMessage = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _submit() async {
    if (_isSubmitting) {
      return;
    }

    setState(() {
      _formNoticeMessage = null;
    });

    final bool isValid = _formKey.currentState?.validate() ?? false;
    if (!isValid) {
      setState(() {
        _formNoticeMessage = '还有几处内容需要补充完整，再发布到社区。';
      });
      return;
    }

    if (_isMediaUploading) {
      setState(() {
        _formNoticeMessage = '媒体附件还在上传，完成后再发布。';
      });
      return;
    }

    if (_hasFailedMedia) {
      setState(() {
        _formNoticeMessage = '有媒体附件上传失败，请重试或移除后再发布。';
      });
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final CommunityPostSnapshot createdPost =
          await repository.createCommunityPost(
        CommunityPostDraft(
          postType: _selectedPostType,
          title: _titleController.text.trim().isEmpty
              ? null
              : _titleController.text.trim(),
          content: _contentController.text.trim(),
          mediaAssetIds: _mediaAssetIds,
          visibility: _visibility,
          petId: _selectedPetId,
          topicId: widget.initialTopic?.topicId,
          cityCode: _currentUser?.cityCode,
        ),
      );
      if (!mounted) {
        return;
      }

      final CompanionFeedbackTone feedbackTone =
          communityReviewFeedbackTone(createdPost.reviewStatus);
      final String feedbackMessage = createdPost.reviewStatus == 'approved'
          ? (_selectedPostType == 'qa' ? '问题已发布到社区' : '社区内容已发布')
          : communityReviewStatusMessage(createdPost.reviewStatus);
      showCompanionFeedback(
        context,
        message: feedbackMessage,
        tone: feedbackTone,
      );
      Navigator.of(context).pop(createdPost);
    } catch (error) {
      if (!mounted) {
        return;
      }
      showCompanionErrorFeedback(context, error.toString());
    } finally {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  void _handleMediaSelection(MediaAttachmentSelectionState selectionState) {
    setState(() {
      _mediaAssetIds = selectionState.assetIds;
      _isMediaUploading = selectionState.isUploading;
      _hasFailedMedia = selectionState.hasFailed;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading && _currentUser == null) {
      return const Scaffold(
        body: CompanionPageLoading(
          title: '正在准备发布空间',
          description: '会先确认你的宠物、城市和社区发布配置。',
          icon: Icons.edit_note_rounded,
          layout: CompanionLoadingLayout.detail,
        ),
      );
    }

    if (_loadErrorMessage != null && _currentUser == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('发布社区内容')),
        body: Padding(
          padding: const EdgeInsets.all(24),
          child: CompanionEmptyState(
            title: '发布页暂时没有准备好',
            description: _loadErrorMessage!,
            icon: Icons.cloud_off_outlined,
            actionLabel: '重新加载',
            onAction: _loadComposerData,
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('发布社区内容')),
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
        child: Form(
          key: _formKey,
          autovalidateMode: AutovalidateMode.onUserInteraction,
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _CommunityComposerHero(topic: widget.initialTopic),
              const SizedBox(height: 16),
              if (_formNoticeMessage != null) ...[
                CompanionFormNotice(message: _formNoticeMessage!),
                const SizedBox(height: 14),
              ],
              CompanionCard(
                padding: const EdgeInsets.all(18),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('内容类型',
                        style: Theme.of(context).textTheme.titleMedium),
                    const SizedBox(height: 10),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: _postTypeOptions
                          .map(
                            (_CommunityPostTypeOption option) => ChoiceChip(
                              label: Text(option.label),
                              selected: _selectedPostType == option.value,
                              onSelected: (_) {
                                setState(() {
                                  _selectedPostType = option.value;
                                });
                              },
                            ),
                          )
                          .toList(),
                    ),
                    const SizedBox(height: 18),
                    TextFormField(
                      controller: _titleController,
                      maxLength: 100,
                      decoration: InputDecoration(
                        labelText:
                            _selectedPostType == 'qa' ? '问题标题' : '标题，可留空',
                        hintText: _selectedPostType == 'qa'
                            ? '例如：猫咪刚到家总躲起来怎么办？'
                            : '给这次分享起个自然的标题',
                      ),
                      validator: (String? value) {
                        if (_selectedPostType == 'qa' &&
                            (value == null || value.trim().isEmpty)) {
                          return '问答帖需要一个清楚的问题标题';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _contentController,
                      minLines: 5,
                      maxLines: 10,
                      maxLength: 5000,
                      decoration: const InputDecoration(
                        labelText: '正文',
                        hintText: '写下真实的观察、经验、问题或温柔瞬间',
                      ),
                      validator: (String? value) {
                        if (value == null || value.trim().isEmpty) {
                          return '正文不能为空';
                        }
                        return null;
                      },
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              CompanionCard(
                padding: const EdgeInsets.all(18),
                color: AppThemePalette.surface,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('关联信息',
                        style: Theme.of(context).textTheme.titleMedium),
                    const SizedBox(height: 12),
                    if (_pets.isEmpty)
                      Text(
                        '还没有可关联的宠物，这条内容会作为普通社区内容发布。',
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                              color: AppThemePalette.muted,
                            ),
                      )
                    else
                      DropdownButtonFormField<String?>(
                        value: _selectedPetId,
                        decoration: const InputDecoration(
                          labelText: '关联宠物',
                        ),
                        items: <DropdownMenuItem<String?>>[
                          const DropdownMenuItem<String?>(
                            value: null,
                            child: Text('不关联宠物'),
                          ),
                          ..._pets.map(
                            (PetDetailSnapshot pet) =>
                                DropdownMenuItem<String?>(
                              value: pet.petId,
                              child: Text(pet.petName),
                            ),
                          ),
                        ],
                        onChanged: (String? value) {
                          setState(() {
                            _selectedPetId = value;
                          });
                        },
                      ),
                    if (widget.initialTopic != null) ...[
                      const SizedBox(height: 14),
                      CompanionPill(
                        label: '# ${widget.initialTopic!.topicName}',
                        icon: Icons.tag_rounded,
                        backgroundColor: AppThemePalette.warmTint,
                        foregroundColor: AppThemePalette.primaryDeep,
                      ),
                    ],
                    if (_currentUser?.cityName != null) ...[
                      const SizedBox(height: 14),
                      CompanionPill(
                        label: '同城：${_currentUser!.cityName}',
                        icon: Icons.location_on_outlined,
                        backgroundColor: AppThemePalette.surfaceRaised,
                      ),
                    ],
                    const SizedBox(height: 16),
                    Text('可见范围',
                        style: Theme.of(context).textTheme.titleMedium),
                    const SizedBox(height: 10),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: const <_CommunityVisibilityOption>[
                        _CommunityVisibilityOption(
                          value: 'public',
                          label: '公开',
                        ),
                        _CommunityVisibilityOption(
                          value: 'follower',
                          label: '关注可见',
                        ),
                      ]
                          .map(
                            (_CommunityVisibilityOption option) => ChoiceChip(
                              label: Text(option.label),
                              selected: _visibility == option.value,
                              onSelected: (_) {
                                setState(() {
                                  _visibility = option.value;
                                });
                              },
                            ),
                          )
                          .toList(),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _visibilityDescription(_visibility),
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              CompanionCard(
                padding: const EdgeInsets.all(18),
                color: AppThemePalette.surface,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('图片或视频',
                        style: Theme.of(context).textTheme.titleMedium),
                    const SizedBox(height: 10),
                    MediaAttachmentPicker(
                      bizType: 'community',
                      initialAssetIds: const <String>[],
                      allowedExtensions: const <String>[
                        'jpg',
                        'jpeg',
                        'png',
                        'webp',
                        'gif',
                        'mp4',
                        'mov',
                      ],
                      pickButtonLabel: '添加社区图片或视频',
                      emptyDescription: '可添加图片或视频，让分享更容易被理解。',
                      onSelectionChanged: _handleMediaSelection,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 18),
              FilledButton.icon(
                onPressed: _isSubmitting ? null : _submit,
                icon: Icon(
                  _isSubmitting
                      ? Icons.hourglass_top_rounded
                      : Icons.send_rounded,
                ),
                label: Text(_isSubmitting ? '发布中...' : '发布到社区'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CommunityComposerHero extends StatelessWidget {
  const _CommunityComposerHero({required this.topic});

  final CommunityTopicSnapshot? topic;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
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
          CompanionPill(
            label: topic == null ? '独立发帖' : '参与话题',
            icon: topic == null ? Icons.edit_note_rounded : Icons.tag_rounded,
            backgroundColor: AppThemePalette.surface,
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(
            topic == null ? '发布社区内容' : '发布到 #${topic!.topicName}',
            style: Theme.of(context).textTheme.titleLarge,
          ),
        ],
      ),
    );
  }
}

class _CommunityPostTypeOption {
  const _CommunityPostTypeOption({
    required this.value,
    required this.label,
  });

  final String value;
  final String label;
}

class _CommunityVisibilityOption {
  const _CommunityVisibilityOption({
    required this.value,
    required this.label,
  });

  final String value;
  final String label;
}

const List<_CommunityPostTypeOption> _postTypeOptions =
    <_CommunityPostTypeOption>[
  _CommunityPostTypeOption(value: 'image_text', label: '图文'),
  _CommunityPostTypeOption(value: 'video', label: '视频'),
  _CommunityPostTypeOption(value: 'qa', label: '问答'),
  _CommunityPostTypeOption(value: 'experience', label: '经验'),
];

String _visibilityDescription(String visibility) {
  switch (visibility) {
    case 'follower':
      return '发布后仅关注你的人能看到。';
    case 'public':
    default:
      return '发布后会进入公开社区流，并按服务端规则出现在推荐、同城或话题页。';
  }
}
