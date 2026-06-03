import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/media_attachment_picker.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/daily_log_draft.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 萌宠日常新建/编辑页。
class DailyLogEditorPage extends StatefulWidget {
  const DailyLogEditorPage({
    super.key,
    required this.petId,
    this.initialDailyLog,
  });

  final String petId;
  final DailyLogSnapshot? initialDailyLog;

  @override
  State<DailyLogEditorPage> createState() => _DailyLogEditorPageState();
}

class _DailyLogEditorPageState extends State<DailyLogEditorPage> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  late final TextEditingController _contentController;
  late final TextEditingController _tagsController;
  late final TextEditingController _happenedAtController;
  late String _visibility;
  late DateTime _happenedAt;
  late List<String> _mediaAssetIds;
  late bool _syncToCommunity;
  bool _isSubmitting = false;
  bool _isUploadingMedia = false;
  bool _hasFailedMedia = false;
  String? _formNoticeMessage;

  @override
  void initState() {
    super.initState();
    final DailyLogSnapshot? initialDailyLog = widget.initialDailyLog;
    _contentController = TextEditingController(
      text: initialDailyLog == null ? '' : initialDailyLog.content,
    );
    _tagsController = TextEditingController(
      text: initialDailyLog == null ? '' : initialDailyLog.tags.join('，'),
    );
    _visibility = initialDailyLog?.visibility ?? 'family';
    _syncToCommunity = initialDailyLog?.visibility == 'public' &&
        (initialDailyLog?.syncToCommunity ?? false);
    _happenedAt = initialDailyLog?.happenedAt ?? DateTime.now();
    _mediaAssetIds =
        List<String>.of(initialDailyLog?.mediaAssetIds ?? const <String>[]);
    _happenedAtController =
        TextEditingController(text: _formatDateTimeLabel(_happenedAt));
  }

  @override
  void dispose() {
    _contentController.dispose();
    _tagsController.dispose();
    _happenedAtController.dispose();
    super.dispose();
  }

  Future<void> _pickHappenedAt() async {
    final DateTime? selectedDate = await showDatePicker(
      context: context,
      initialDate: _happenedAt,
      firstDate: DateTime(2000, 1, 1),
      lastDate: DateTime.now(),
    );
    if (!mounted || selectedDate == null) {
      return;
    }

    final TimeOfDay? selectedTime = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(_happenedAt),
    );
    if (!mounted) {
      return;
    }

    final TimeOfDay resolvedTime =
        selectedTime ?? TimeOfDay.fromDateTime(_happenedAt);
    final DateTime selectedDateTime = DateTime(
      selectedDate.year,
      selectedDate.month,
      selectedDate.day,
      resolvedTime.hour,
      resolvedTime.minute,
    );

    setState(() {
      _happenedAt = selectedDateTime;
      _happenedAtController.text = _formatDateTimeLabel(selectedDateTime);
    });
  }

  Future<void> _submit() async {
    if (_isSubmitting) {
      return;
    }
    if (!_formKey.currentState!.validate()) {
      _showFormNotice('还有日常内容没有填完整，请先看标红的输入框。');
      return;
    }
    if (_isUploadingMedia) {
      _showFormNotice('图片或视频还在上传中，请稍后再保存。');
      return;
    }
    if (_hasFailedMedia) {
      _showFormNotice('请先移除上传失败的图片或视频。');
      return;
    }

    setState(() {
      _isSubmitting = true;
      _formNoticeMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final DailyLogDraft draft = DailyLogDraft(
        content: _contentController.text.trim(),
        mediaAssetIds: _mediaAssetIds,
        tags: _parseTags(_tagsController.text),
        visibility: _visibility,
        syncToCommunity: _syncToCommunity,
        happenedAt: _happenedAt,
      );
      if (widget.initialDailyLog == null) {
        await repository.createDailyLog(
          petId: widget.petId,
          draft: draft,
        );
      } else {
        await repository.updateDailyLog(
          petId: widget.petId,
          dailyLogId: widget.initialDailyLog!.dailyLogId,
          draft: draft,
        );
      }
      if (!mounted) {
        return;
      }

      showCompanionSuccessFeedback(
        context,
        widget.initialDailyLog == null ? '萌宠日常已保存' : '萌宠日常已更新',
      );
      Navigator.of(context).pop(true);
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

  void _showFormNotice(String message) {
    setState(() {
      _formNoticeMessage = message;
    });
  }

  @override
  Widget build(BuildContext context) {
    final bool isEditMode = widget.initialDailyLog != null;

    return Scaffold(
      appBar: AppBar(
        title: Text(isEditMode ? '编辑萌宠日常' : '新建萌宠日常'),
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
        child: Form(
          key: _formKey,
          autovalidateMode: AutovalidateMode.onUserInteraction,
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _DailyLogEditorHeroCard(isEditMode: isEditMode),
              if (_formNoticeMessage != null) ...[
                const SizedBox(height: 12),
                CompanionFormNotice(message: _formNoticeMessage!),
              ],
              const SizedBox(height: 16),
              _DailyLogFormSection(
                title: '记录内容',
                description: '把今天发生的小事、心情和变化写下来，之后回看会很有温度。',
                child: Column(
                  children: [
                    TextFormField(
                      controller: _contentController,
                      minLines: 4,
                      maxLines: 6,
                      decoration: const InputDecoration(
                        labelText: '日常内容',
                        hintText: '例如：今天第一次主动跳上窗台晒太阳。',
                      ),
                      validator: (String? value) {
                        return value == null || value.trim().isEmpty
                            ? '请输入日常内容'
                            : null;
                      },
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _tagsController,
                      decoration: const InputDecoration(
                        labelText: '标签',
                        hintText: '多个标签请用中文逗号或英文逗号分隔',
                      ),
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _happenedAtController,
                      readOnly: true,
                      decoration: const InputDecoration(
                        labelText: '记录时间',
                        suffixIcon: Icon(Icons.schedule_outlined),
                      ),
                      onTap: _pickHappenedAt,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              _DailyLogFormSection(
                title: '可见范围',
                description: '先想清楚这条内容想留给谁看，后面整理时会更轻松。',
                child: DropdownButtonFormField<String>(
                  initialValue: _visibility,
                  decoration: const InputDecoration(labelText: '可见范围'),
                  items: const [
                    DropdownMenuItem(value: 'private', child: Text('仅自己可见')),
                    DropdownMenuItem(value: 'family', child: Text('家庭可见')),
                    DropdownMenuItem(value: 'public', child: Text('公开到社区')),
                  ],
                  onChanged: (String? value) {
                    if (value == null) {
                      return;
                    }
                    setState(() {
                      _visibility = value;
                      if (_visibility != 'public') {
                        _syncToCommunity = false;
                      }
                    });
                  },
                ),
              ),
              const SizedBox(height: 16),
              _DailyLogFormSection(
                title: '照片与视频',
                description: '把这一刻的画面一起留下来，之后翻日常时会更完整。',
                child: MediaAttachmentPicker(
                  bizType: 'daily_log',
                  initialAssetIds: _mediaAssetIds,
                  allowedExtensions: const [
                    'jpg',
                    'jpeg',
                    'png',
                    'webp',
                    'gif',
                    'mp4',
                    'mov',
                  ],
                  maxItems: 9,
                  pickButtonLabel: '添加图片或视频',
                  emptyDescription: '可以选择照片或短视频，上传成功后会随这条日常保存。',
                  onSelectionChanged: _handleMediaSelectionChanged,
                ),
              ),
              const SizedBox(height: 16),
              _DailyLogFormSection(
                title: '社区同步',
                description: '只有公开内容才能同步到社区。关闭后，这条日常仍然会留在宠物档案里。',
                child: SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('同步到社区'),
                  subtitle: Text(
                    _visibility == 'public'
                        ? '开启后会把这条公开日常同步到社区推荐流。'
                        : '只有选择“公开到社区”后才允许同步。',
                  ),
                  value: _syncToCommunity,
                  onChanged: _visibility == 'public'
                      ? (bool value) {
                          setState(() {
                            _syncToCommunity = value;
                          });
                        }
                      : null,
                ),
              ),
            ],
          ),
        ),
      ),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(16, 12, 16, 16),
        child: FilledButton(
          onPressed: _isSubmitting || _isUploadingMedia ? null : _submit,
          child: Text(
            _isSubmitting
                ? '保存中...'
                : _isUploadingMedia
                    ? '媒体上传中...'
                    : isEditMode
                        ? '保存修改'
                        : '保存日常',
          ),
        ),
      ),
    );
  }

  void _handleMediaSelectionChanged(
    MediaAttachmentSelectionState selectionState,
  ) {
    setState(() {
      _mediaAssetIds = selectionState.assetIds;
      _isUploadingMedia = selectionState.isUploading;
      _hasFailedMedia = selectionState.hasFailed;
    });
  }
}

class _DailyLogEditorHeroCard extends StatelessWidget {
  const _DailyLogEditorHeroCard({required this.isEditMode});

  final bool isEditMode;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFECDD),
          Color(0xFFFFFBF5),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const CompanionPill(
            label: '萌宠日常编辑',
            icon: Icons.edit_note_rounded,
            backgroundColor: Color(0xFFFFE2CF),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(
            isEditMode ? '把这一刻补得更完整一些' : '把今天的小瞬间记下来',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 10),
          Text(
            '一句话、一个小动作、一次状态变化，都可能是以后回头看时最柔软的记忆。',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ),
    );
  }
}

class _DailyLogFormSection extends StatelessWidget {
  const _DailyLogFormSection({
    required this.title,
    required this.description,
    required this.child,
  });

  final String title;
  final String description;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(18),
      radius: 24,
      color: AppThemePalette.surface,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 16),
          child,
        ],
      ),
    );
  }
}

List<String> _parseTags(String value) {
  return value
      .split(RegExp(r'[，,]'))
      .map((String item) => item.trim())
      .where((String item) => item.isNotEmpty)
      .toList();
}

String _formatDateTimeLabel(DateTime value) {
  final String month = value.month.toString().padLeft(2, '0');
  final String day = value.day.toString().padLeft(2, '0');
  final String hour = value.hour.toString().padLeft(2, '0');
  final String minute = value.minute.toString().padLeft(2, '0');
  return '${value.year}-$month-$day $hour:$minute';
}
