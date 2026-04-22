import 'package:flutter/material.dart';
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

    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('删除萌宠日常'),
          content: const Text('删除后这条日常将不再出现在宠物记录中，确认继续吗？'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: const Text('取消'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(context).pop(true),
              child: const Text('确认删除'),
            ),
          ],
        );
      },
    );
    if (confirmed != true || !mounted) {
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

      Navigator.of(context).pop(true);
    } catch (error) {
      if (!mounted) {
        return;
      }

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error.toString())),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isDeleting = false;
        });
      }
    }
  }

  Future<bool> _handleWillPop() async {
    Navigator.of(context).pop(_hasChanges);
    return false;
  }

  @override
  Widget build(BuildContext context) {
    final DailyLogSnapshot? dailyLog = _dailyLog;

    return WillPopScope(
      onWillPop: _handleWillPop,
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
        body: _buildBody(dailyLog),
        bottomNavigationBar: dailyLog == null
            ? null
            : SafeArea(
                minimum: const EdgeInsets.fromLTRB(16, 12, 16, 16),
                child: OutlinedButton(
                  onPressed: _isDeleting ? null : _deleteDailyLog,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFFB91C1C),
                  ),
                  child: Text(_isDeleting ? '删除中...' : '删除日常'),
                ),
              ),
      ),
    );
  }

  Widget _buildBody(DailyLogSnapshot? dailyLog) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    if (_isLoading && dailyLog == null) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null && dailyLog == null) {
      return ListView(
        padding: const EdgeInsets.all(16),
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
            onPressed: _loadDailyLog,
            child: const Text('重新加载'),
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
        _DetailSection(
          title: widget.petName,
          description: '萌宠日常先沉淀为宠物资产，详情页用于稳定回看内容、标签和可见范围。',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(dailyLog.content, style: textTheme.titleLarge),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: dailyLog.tags.isEmpty
                    ? const <Widget>[Text('未填写标签')]
                    : dailyLog.tags
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
        const SizedBox(height: 16),
        _DetailSection(
          title: '记录信息',
          description: '当前阶段先稳定呈现记录时间和可见范围，后续媒体、社区同步等能力会在这里扩展。',
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
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 6),
          Text(
            description,
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: const Color(0xFF64748B)),
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
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 72,
          child: Text(
            label,
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: const Color(0xFF64748B)),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Text(value, style: Theme.of(context).textTheme.bodyLarge),
        ),
      ],
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
