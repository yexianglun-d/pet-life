import 'package:flutter/material.dart';
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
          title: const Text('健康记录'),
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
              description: '健康记录先在宠物维度沉淀，后续提醒计划、时间轴和就医辅助都会以这里的数据为准。',
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      '当前已记录 ${_healthRecords.length} 条健康事件',
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ),
                  FilledButton(
                    onPressed: _openCreateHealthRecordPage,
                    child: const Text('新增记录'),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            PageSection(
              title: '记录列表',
              description: '当前按时间倒序展示，先保证用户能稳定回看和补录。',
              child: _buildHealthRecordList(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHealthRecordList() {
    if (_isLoading && _healthRecords.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null && _healthRecords.isEmpty) {
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
            onPressed: _loadHealthRecords,
            child: const Text('重新加载'),
          ),
        ],
      );
    }

    if (_healthRecords.isEmpty) {
      return const Text('还没有健康记录，先新增一条记录吧。');
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
      borderRadius: BorderRadius.circular(18),
      child: Ink(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFFF8FAFC),
          borderRadius: BorderRadius.circular(18),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(record.title, style: textTheme.titleMedium),
                ),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: const Color(0xFFE0F2FE),
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    _toLocalizedRecordType(record.recordType),
                    style: textTheme.bodyMedium
                        ?.copyWith(color: const Color(0xFF0F766E)),
                  ),
                ),
                const SizedBox(width: 8),
                const Icon(
                  Icons.chevron_right,
                  size: 20,
                  color: Color(0xFF94A3B8),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              _buildDescription(record),
              style: textTheme.bodyMedium
                  ?.copyWith(color: const Color(0xFF64748B)),
            ),
          ],
        ),
      ),
    );
  }
}

String _buildDescription(HealthRecordSnapshot record) {
  final String month = record.occurredAt.month.toString().padLeft(2, '0');
  final String day = record.occurredAt.day.toString().padLeft(2, '0');
  final String hour = record.occurredAt.hour.toString().padLeft(2, '0');
  final String minute = record.occurredAt.minute.toString().padLeft(2, '0');

  final List<String> parts = <String>[
    '${record.occurredAt.year}-$month-$day $hour:$minute',
    if (record.value != null)
      record.unit == null ? record.value! : '${record.value} ${record.unit}',
    if (record.notes != null && record.notes!.trim().isNotEmpty) record.notes!,
  ];
  return parts.join(' · ');
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
    default:
      return recordType;
  }
}
