import 'package:flutter/material.dart';
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

  Future<bool> _handleWillPop() async {
    Navigator.of(context).pop(_hasChanges);
    return false;
  }

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: _handleWillPop,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('萌宠日常'),
          leading: IconButton(
            onPressed: () => Navigator.of(context).pop(_hasChanges),
            icon: const Icon(Icons.arrow_back),
          ),
        ),
        body: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            PageSection(
              title: widget.petName,
              description: '萌宠日常先沉淀为宠物私域内容资产，详情回看和后续社区发布都会从这里出发。',
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      '当前已记录 ${_dailyLogs.length} 条日常',
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ),
                  FilledButton(
                    onPressed: _openCreateDailyLogPage,
                    child: const Text('新增日常'),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            PageSection(
              title: '日常列表',
              description: '当前按时间倒序展示，先保证用户能稳定新增、浏览和回看。',
              child: _buildDailyLogList(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDailyLogList() {
    if (_isLoading && _dailyLogs.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null && _dailyLogs.isEmpty) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            _errorMessage!,
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: const Color(0xFFB91C1C)),
          ),
          const SizedBox(height: 12),
          OutlinedButton(
            onPressed: _loadDailyLogs,
            child: const Text('重新加载'),
          ),
        ],
      );
    }

    if (_dailyLogs.isEmpty) {
      return const Text('还没有萌宠日常记录，先写下第一条吧。');
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
      borderRadius: BorderRadius.circular(18),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFFF8FAFC),
          borderRadius: BorderRadius.circular(18),
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
              style: textTheme.bodyMedium
                  ?.copyWith(color: const Color(0xFF64748B)),
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: dailyLog.tags
                  .map(
                    (String tag) => Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 10,
                        vertical: 6,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFDE68A),
                        borderRadius: BorderRadius.circular(999),
                      ),
                      child: Text(tag),
                    ),
                  )
                  .toList(),
            ),
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
      ' · ${_toLocalizedVisibility(dailyLog.visibility)}';
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
