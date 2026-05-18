import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_loading.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/dailylog/presentation/pages/daily_log_detail_page.dart';
import 'package:petlife_mobile_app/modules/dailylog/presentation/pages/daily_log_editor_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 萌宠日常列表页。
class DailyLogListPage extends StatefulWidget {
  const DailyLogListPage({
    super.key,
    required this.petId,
    required this.petName,
  });

  final String petId;
  final String petName;

  @override
  State<DailyLogListPage> createState() => _DailyLogListPageState();
}

class _DailyLogListPageState extends State<DailyLogListPage> {
  bool _didLoad = false;
  bool _isLoading = false;
  bool _hasChanges = false;
  String? _errorMessage;
  List<DailyLogSnapshot> _dailyLogs = const <DailyLogSnapshot>[];

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }

    _didLoad = true;
    _loadDailyLogs();
  }

  Future<void> _loadDailyLogs() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final List<DailyLogSnapshot> dailyLogs =
          await repository.listDailyLogs(widget.petId);
      if (!mounted) {
        return;
      }

      setState(() {
        _dailyLogs = dailyLogs;
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

  Future<void> _openCreateDailyLogPage() async {
    final bool? created = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => DailyLogEditorPage(petId: widget.petId),
      ),
    );
    if (!mounted || created != true) {
      return;
    }

    _hasChanges = true;
    await _loadDailyLogs();
  }

  Future<void> _openDailyLogDetail(DailyLogSnapshot dailyLog) async {
    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => DailyLogDetailPage(
          petId: widget.petId,
          petName: widget.petName,
          dailyLogId: dailyLog.dailyLogId,
        ),
      ),
    );
    if (!mounted || changed != true) {
      return;
    }

    _hasChanges = true;
    await _loadDailyLogs();
  }

  @override
  Widget build(BuildContext context) {
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
          title: const Text('萌宠日常'),
          leading: IconButton(
            onPressed: () => Navigator.of(context).pop(_hasChanges),
            icon: const Icon(Icons.arrow_back),
          ),
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
              _DailyLogHeroSection(
                petName: widget.petName,
                dailyLogCount: _dailyLogs.length,
                onCreate: _openCreateDailyLogPage,
              ),
              const SizedBox(height: 16),
              PageSection(
                title: '这些小片段都被记下来了',
                description: '日常里的可爱、状态和小变化，都会慢慢积累成很完整的陪伴记忆。',
                child: _buildDailyLogList(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDailyLogList() {
    if (_isLoading && _dailyLogs.isEmpty) {
      return const CompanionSkeletonList(
        itemCount: 3,
      );
    }

    if (_errorMessage != null && _dailyLogs.isEmpty) {
      return CompanionEmptyState(
        title: '萌宠日常暂时没有加载出来',
        description: _errorMessage!,
        icon: Icons.cloud_off_outlined,
        actionLabel: '重新加载',
        onAction: _loadDailyLogs,
      );
    }

    if (_dailyLogs.isEmpty) {
      return const CompanionEmptyState(
        title: '还没有萌宠日常记录',
        description: '写下一句今天发生的小事，以后回头看时会很温柔。',
        icon: Icons.auto_awesome_outlined,
      );
    }

    return Column(
      children: _dailyLogs
          .map(
            (DailyLogSnapshot dailyLog) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _DailyLogCard(
                dailyLog: dailyLog,
                onTap: () => _openDailyLogDetail(dailyLog),
              ),
            ),
          )
          .toList(),
    );
  }
}

class _DailyLogHeroSection extends StatelessWidget {
  const _DailyLogHeroSection({
    required this.petName,
    required this.dailyLogCount,
    required this.onCreate,
  });

  final String petName;
  final int dailyLogCount;
  final VoidCallback onCreate;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFECDD),
          Color(0xFFFFFAF4),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const CompanionPill(
            label: '萌宠日常',
            icon: Icons.auto_awesome_outlined,
            backgroundColor: Color(0xFFFFE1D0),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(petName, style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 10),
          Text(
            '把今天的样子、心情和有趣的小瞬间记下来，时间久了会很珍贵。',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 16),
          CompanionPill(
            label: '已经记录 $dailyLogCount 条',
            backgroundColor: AppThemePalette.surface,
          ),
          const SizedBox(height: 18),
          FilledButton(
            onPressed: onCreate,
            child: const Text('新增日常'),
          ),
        ],
      ),
    );
  }
}

class _DailyLogCard extends StatelessWidget {
  const _DailyLogCard({
    required this.dailyLog,
    required this.onTap,
  });

  final DailyLogSnapshot dailyLog;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(24),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppThemePalette.surfaceRaised,
          borderRadius: BorderRadius.circular(24),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              dailyLog.content,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(
              _buildDescription(dailyLog),
              style: textTheme.bodyMedium?.copyWith(
                color: AppThemePalette.muted,
              ),
            ),
            if (dailyLog.tags.isNotEmpty) ...[
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: dailyLog.tags
                    .map(
                      (String tag) => CompanionPill(
                        label: '#$tag',
                        backgroundColor: AppThemePalette.warmTint,
                      ),
                    )
                    .toList(),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

String _buildDescription(DailyLogSnapshot dailyLog) {
  final String month = dailyLog.happenedAt.month.toString().padLeft(2, '0');
  final String day = dailyLog.happenedAt.day.toString().padLeft(2, '0');
  final String hour = dailyLog.happenedAt.hour.toString().padLeft(2, '0');
  final String minute = dailyLog.happenedAt.minute.toString().padLeft(2, '0');

  return '${dailyLog.happenedAt.year}-$month-$day $hour:$minute'
      ' · ${_toLocalizedVisibility(dailyLog.visibility)}'
      ' · ${dailyLog.syncToCommunity ? '已同步社区' : '仅保留档案'}';
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
