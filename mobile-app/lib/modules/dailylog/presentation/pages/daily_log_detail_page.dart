import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_loading.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/dailylog/presentation/pages/daily_log_editor_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 萌宠日常详情页。
class DailyLogDetailPage extends StatefulWidget {
  const DailyLogDetailPage({
    super.key,
    required this.petId,
    required this.petName,
    required this.dailyLogId,
  });

  final String petId;
  final String petName;
  final String dailyLogId;

  @override
  State<DailyLogDetailPage> createState() => _DailyLogDetailPageState();
}

class _DailyLogDetailPageState extends State<DailyLogDetailPage> {
  bool _didLoad = false;
  bool _isLoading = false;
  bool _isDeleting = false;
  bool _hasChanges = false;
  String? _errorMessage;
  DailyLogSnapshot? _dailyLog;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadDailyLog();
  }

  Future<void> _loadDailyLog() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final DailyLogSnapshot dailyLog = await repository.getDailyLog(
        petId: widget.petId,
        dailyLogId: widget.dailyLogId,
      );
      if (!mounted) {
        return;
      }

      setState(() {
        _dailyLog = dailyLog;
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

  Future<void> _openEditPage() async {
    final DailyLogSnapshot? dailyLog = _dailyLog;
    if (dailyLog == null) {
      return;
    }

    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => DailyLogEditorPage(
          petId: widget.petId,
          initialDailyLog: dailyLog,
        ),
      ),
    );
    if (!mounted || changed != true) {
      return;
    }

    _hasChanges = true;
    await _loadDailyLog();
  }

  Future<void> _deleteDailyLog() async {
    if (_isDeleting) {
      return;
    }

    final bool confirmed = await showCompanionConfirmSheet(
      context,
      title: '删除萌宠日常',
      description: '删除后这条日常会从宠物记录里移除，确认继续吗？',
      confirmLabel: '确认删除',
      confirmColor: AppThemePalette.danger,
    );
    if (!confirmed || !mounted) {
      return;
    }

    setState(() {
      _isDeleting = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      await repository.deleteDailyLog(
        petId: widget.petId,
        dailyLogId: widget.dailyLogId,
      );
      if (!mounted) {
        return;
      }

      showCompanionSuccessFeedback(context, '萌宠日常已删除');
      Navigator.of(context).pop(true);
    } catch (error) {
      if (!mounted) {
        return;
      }

      showCompanionErrorFeedback(context, error.toString());
    } finally {
      if (mounted) {
        setState(() {
          _isDeleting = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final DailyLogSnapshot? dailyLog = _dailyLog;

    return PopScope<bool>(
      canPop: false,
      onPopInvokedWithResult: (bool didPop, bool? result) {
        if (didPop) {
          return;
        }
        Navigator.of(context).pop(_hasChanges);
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('日常详情'),
          leading: IconButton(
            onPressed: () => Navigator.of(context).pop(_hasChanges),
            icon: const Icon(Icons.arrow_back),
          ),
          actions: [
            if (dailyLog != null)
              TextButton(
                onPressed: _openEditPage,
                child: const Text('编辑'),
              ),
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
          child: _buildBody(dailyLog),
        ),
        bottomNavigationBar: dailyLog == null
            ? null
            : SafeArea(
                minimum: const EdgeInsets.fromLTRB(16, 12, 16, 16),
                child: OutlinedButton(
                  onPressed: _isDeleting ? null : _deleteDailyLog,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppThemePalette.danger,
                  ),
                  child: Text(_isDeleting ? '删除中...' : '删除日常'),
                ),
              ),
      ),
    );
  }

  Widget _buildBody(DailyLogSnapshot? dailyLog) {
    if (_isLoading && dailyLog == null) {
      return CompanionPageLoading(
        title: '正在整理${widget.petName}的日常片段',
        description: '内容、标签和照片视频区域会先保持详情结构，加载完成后直接补上内容。',
        icon: Icons.auto_awesome_outlined,
        layout: CompanionLoadingLayout.detail,
      );
    }

    if (_errorMessage != null && dailyLog == null) {
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          CompanionEmptyState(
            title: '日常详情暂时没有加载出来',
            description: _errorMessage!,
            icon: Icons.cloud_off_outlined,
            actionLabel: '重新加载',
            onAction: _loadDailyLog,
          ),
        ],
      );
    }

    if (dailyLog == null) {
      return const SizedBox.shrink();
    }

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _DailyLogHeroCard(
          petName: widget.petName,
          dailyLog: dailyLog,
        ),
        const SizedBox(height: 16),
        _DetailSection(
          title: '这次记录了什么',
          description: '把当时的内容、标签和感受整理在一起，以后回看会更完整。',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(dailyLog.content,
                  style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: dailyLog.tags.isEmpty
                    ? const <Widget>[
                        CompanionPill(
                          label: '还没有标签',
                          backgroundColor: AppThemePalette.surfaceRaised,
                        ),
                      ]
                    : dailyLog.tags
                        .map(
                          (String tag) => CompanionPill(
                            label: '#$tag',
                            backgroundColor: AppThemePalette.warmTint,
                          ),
                        )
                        .toList(),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        _DetailSection(
          title: '记录信息',
          description: '把时间、可见范围和同步状态写清楚，之后就不容易混淆。',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _DetailRow(
                label: '记录时间',
                value: _formatDateTime(dailyLog.happenedAt),
              ),
              const SizedBox(height: 12),
              _DetailRow(
                label: '可见范围',
                value: _toLocalizedVisibility(dailyLog.visibility),
              ),
              const SizedBox(height: 12),
              _DetailRow(
                label: '社区同步',
                value: dailyLog.syncToCommunity ? '已同步到社区' : '未同步到社区',
              ),
              if (dailyLog.createdAt != null) ...[
                const SizedBox(height: 12),
                _DetailRow(
                  label: '创建时间',
                  value: _formatDateTime(dailyLog.createdAt!),
                ),
              ],
            ],
          ),
        ),
      ],
    );
  }
}

class _DailyLogHeroCard extends StatelessWidget {
  const _DailyLogHeroCard({
    required this.petName,
    required this.dailyLog,
  });

  final String petName;
  final DailyLogSnapshot dailyLog;

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
          CompanionPill(
            label: _toLocalizedVisibility(dailyLog.visibility),
            icon: Icons.favorite_border_rounded,
            backgroundColor: const Color(0xFFFFE0CF),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(petName, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          Text(
            dailyLog.content,
            style: Theme.of(context).textTheme.headlineSmall,
          ),
        ],
      ),
    );
  }
}

class _DetailSection extends StatelessWidget {
  const _DetailSection({
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
          const SizedBox(height: 6),
          Text(
            description,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: AppThemePalette.muted,
                ),
          ),
          const SizedBox(height: 16),
          child,
        ],
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      radius: 22,
      color: AppThemePalette.surfaceRaised,
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          SizedBox(
            width: 84,
            child: Text(
              label,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: AppThemePalette.muted,
                  ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ),
        ],
      ),
    );
  }
}

String _formatDateTime(DateTime value) {
  final String month = value.month.toString().padLeft(2, '0');
  final String day = value.day.toString().padLeft(2, '0');
  final String hour = value.hour.toString().padLeft(2, '0');
  final String minute = value.minute.toString().padLeft(2, '0');
  return '${value.year}-$month-$day $hour:$minute';
}

String _toLocalizedVisibility(String visibility) {
  switch (visibility) {
    case 'public':
      return '公开到社区';
    case 'family':
      return '家庭可见';
    case 'private':
      return '仅自己可见';
    default:
      return visibility;
  }
}
