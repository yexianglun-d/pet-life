import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/health/presentation/pages/health_record_detail_page.dart';
import 'package:petlife_mobile_app/modules/health/presentation/pages/health_record_editor_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 健康记录列表页。
class HealthRecordListPage extends StatefulWidget {
  const HealthRecordListPage({
    super.key,
    required this.petId,
    required this.petName,
  });

  final String petId;
  final String petName;

  @override
  State<HealthRecordListPage> createState() => _HealthRecordListPageState();
}

class _HealthRecordListPageState extends State<HealthRecordListPage> {
  bool _didLoad = false;
  bool _isLoading = false;
  bool _hasChanges = false;
  String? _errorMessage;
  List<HealthRecordSnapshot> _healthRecords = const <HealthRecordSnapshot>[];

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadHealthRecords();
  }

  Future<void> _loadHealthRecords() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final List<HealthRecordSnapshot> healthRecords =
          await repository.listHealthRecords(widget.petId);
      if (!mounted) {
        return;
      }
      setState(() {
        _healthRecords = healthRecords;
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

  Future<void> _openCreateHealthRecordPage() async {
    final bool? created = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => HealthRecordEditorPage(petId: widget.petId),
      ),
    );
    if (!mounted || created != true) {
      return;
    }

    _hasChanges = true;
    await _loadHealthRecords();
  }

  Future<void> _openHealthRecordDetailPage(HealthRecordSnapshot record) async {
    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => HealthRecordDetailPage(
          petId: widget.petId,
          petName: widget.petName,
          healthRecordId: record.healthRecordId,
        ),
      ),
    );
    if (!mounted || changed != true) {
      return;
    }

    _hasChanges = true;
    await _loadHealthRecords();
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
          title: const Text('健康记录'),
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
              _HealthHeroSection(
                petName: widget.petName,
                recordCount: _healthRecords.length,
                onCreate: _openCreateHealthRecordPage,
              ),
              const SizedBox(height: 16),
              PageSection(
                title: '最近的健康变化',
                description: '重要记录都整理在这里，回头看时会更容易想起当时发生了什么。',
                child: _buildHealthRecordList(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHealthRecordList() {
    if (_isLoading && _healthRecords.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 20),
        child: Center(child: CircularProgressIndicator()),
      );
    }

    if (_errorMessage != null && _healthRecords.isEmpty) {
      return CompanionEmptyState(
        title: '健康记录暂时没有加载出来',
        description: _errorMessage!,
        icon: Icons.cloud_off_outlined,
        actionLabel: '重新加载',
        onAction: _loadHealthRecords,
      );
    }

    if (_healthRecords.isEmpty) {
      return const CompanionEmptyState(
        title: '还没有健康记录',
        description: '第一次疫苗、体检或用药记录，也会成为以后回看时很重要的起点。',
        icon: Icons.health_and_safety_outlined,
      );
    }

    return Column(
      children: _healthRecords
          .map(
            (HealthRecordSnapshot record) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _HealthRecordCard(
                record: record,
                onTap: () => _openHealthRecordDetailPage(record),
              ),
            ),
          )
          .toList(),
    );
  }
}

class _HealthHeroSection extends StatelessWidget {
  const _HealthHeroSection({
    required this.petName,
    required this.recordCount,
    required this.onCreate,
  });

  final String petName;
  final int recordCount;
  final VoidCallback onCreate;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFEADA),
          Color(0xFFFFFAF3),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const CompanionPill(
            label: '健康档案',
            icon: Icons.health_and_safety_outlined,
            backgroundColor: Color(0xFFFFE0CD),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(petName, style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 10),
          Text(
            '把体检、疫苗、驱虫和异常观察慢慢整理好，照顾时会更安心。',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 16),
          CompanionPill(
            label: '已记录 $recordCount 条',
            backgroundColor: AppThemePalette.surface,
          ),
          const SizedBox(height: 18),
          FilledButton(
            onPressed: onCreate,
            child: const Text('新增健康记录'),
          ),
        ],
      ),
    );
  }
}

class _HealthRecordCard extends StatelessWidget {
  const _HealthRecordCard({
    required this.record,
    required this.onTap,
  });

  final HealthRecordSnapshot record;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(24),
      child: Ink(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppThemePalette.surfaceRaised,
          borderRadius: BorderRadius.circular(24),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(record.title, style: textTheme.titleMedium),
                ),
                CompanionPill(
                  label: _toLocalizedRecordType(record.recordType),
                  backgroundColor: AppThemePalette.sky.withValues(alpha: 0.22),
                  foregroundColor: const Color(0xFF5D8794),
                ),
                const SizedBox(width: 8),
                const Icon(
                  Icons.chevron_right,
                  size: 20,
                  color: AppThemePalette.muted,
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              _buildDescription(record),
              style: textTheme.bodyMedium?.copyWith(
                color: AppThemePalette.muted,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

String _toLocalizedRecordType(String recordType) {
  switch (recordType) {
    case 'vaccine':
      return '疫苗';
    case 'deworming':
      return '驱虫';
    case 'examination':
      return '体检';
    case 'medication':
      return '用药';
    case 'observation':
      return '异常观察';
    case 'weight':
      return '体重';
    default:
      return recordType;
  }
}

String _buildDescription(HealthRecordSnapshot record) {
  final List<String> parts = <String>[
    _formatDateTimeLabel(record.occurredAt),
    if (record.value != null && record.unit != null)
      '${record.value} ${record.unit}',
    if (record.notes != null && record.notes!.trim().isNotEmpty) record.notes!,
  ];
  return parts.join(' · ');
}

String _formatDateTimeLabel(DateTime value) {
  final String month = value.month.toString().padLeft(2, '0');
  final String day = value.day.toString().padLeft(2, '0');
  final String hour = value.hour.toString().padLeft(2, '0');
  final String minute = value.minute.toString().padLeft(2, '0');
  return '${value.year}-$month-$day $hour:$minute';
}
