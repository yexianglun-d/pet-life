import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/health/presentation/pages/health_record_editor_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

/// 健康记录详情页。
class HealthRecordDetailPage extends StatefulWidget {
  const HealthRecordDetailPage({
    super.key,
    required this.petId,
    required this.petName,
    required this.healthRecordId,
  });

  final String petId;
  final String petName;
  final String healthRecordId;

  @override
  State<HealthRecordDetailPage> createState() => _HealthRecordDetailPageState();
}

class _HealthRecordDetailPageState extends State<HealthRecordDetailPage> {
  bool _didLoad = false;
  bool _isLoading = false;
  bool _isDeleting = false;
  bool _hasChanges = false;
  String? _errorMessage;
  HealthRecordSnapshot? _healthRecord;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadHealthRecord();
  }

  Future<void> _loadHealthRecord() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final HealthRecordSnapshot healthRecord =
          await repository.getHealthRecord(
        petId: widget.petId,
        healthRecordId: widget.healthRecordId,
      );
      if (!mounted) {
        return;
      }
      setState(() {
        _healthRecord = healthRecord;
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
    final HealthRecordSnapshot? healthRecord = _healthRecord;
    if (healthRecord == null) {
      return;
    }

    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => HealthRecordEditorPage(
          petId: widget.petId,
          initialRecord: healthRecord,
        ),
      ),
    );
    if (!mounted || changed != true) {
      return;
    }

    _hasChanges = true;
    await _loadHealthRecord();
  }

  Future<void> _deleteHealthRecord() async {
    if (_isDeleting) {
      return;
    }

    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('删除健康记录'),
          content: const Text('删除后该记录将不再出现在健康档案中，确认继续吗？'),
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
      await repository.deleteHealthRecord(
        petId: widget.petId,
        healthRecordId: widget.healthRecordId,
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
    final HealthRecordSnapshot? healthRecord = _healthRecord;

    return WillPopScope(
      onWillPop: _handleWillPop,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('健康记录详情'),
          leading: IconButton(
            onPressed: () => Navigator.of(context).pop(_hasChanges),
            icon: const Icon(Icons.arrow_back),
          ),
          actions: [
            if (healthRecord != null)
              TextButton(
                onPressed: _openEditPage,
                child: const Text('编辑'),
              ),
          ],
        ),
        body: _buildBody(healthRecord),
        bottomNavigationBar: healthRecord == null
            ? null
            : SafeArea(
                minimum: const EdgeInsets.fromLTRB(16, 12, 16, 16),
                child: OutlinedButton(
                  onPressed: _isDeleting ? null : _deleteHealthRecord,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFFB91C1C),
                  ),
                  child: Text(_isDeleting ? '删除中...' : '删除记录'),
                ),
              ),
      ),
    );
  }

  Widget _buildBody(HealthRecordSnapshot? healthRecord) {
    if (_isLoading && healthRecord == null) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null && healthRecord == null) {
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
            onPressed: _loadHealthRecord,
            child: const Text('重新加载'),
          ),
        ],
      );
    }

    if (healthRecord == null) {
      return const SizedBox.shrink();
    }

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        PageSection(
          title: healthRecord.title,
          description: '健康记录详情页需要承接回看、编辑和删除三类动作，避免用户在列表里盲改。',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _InfoRow(label: '宠物', value: widget.petName),
              const SizedBox(height: 12),
              _InfoRow(
                label: '类型',
                value: _toLocalizedRecordType(healthRecord.recordType),
              ),
              const SizedBox(height: 12),
              _InfoRow(
                label: '发生时间',
                value: _formatDateTimeLabel(healthRecord.occurredAt),
              ),
              if (healthRecord.value != null) ...[
                const SizedBox(height: 12),
                _InfoRow(
                  label: '数值',
                  value: healthRecord.unit == null
                      ? healthRecord.value!
                      : '${healthRecord.value} ${healthRecord.unit}',
                ),
              ],
              if (healthRecord.createdAt != null) ...[
                const SizedBox(height: 12),
                _InfoRow(
                  label: '创建时间',
                  value: _formatDateTimeLabel(healthRecord.createdAt!),
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '备注',
          description: '把医院、药量、检查结果和异常表现写清楚，后面回看时才不会只剩标题。',
          child: Text(
            healthRecord.notes == null || healthRecord.notes!.trim().isEmpty
                ? '当前没有补充备注。'
                : healthRecord.notes!,
            style: Theme.of(context).textTheme.bodyLarge,
          ),
        ),
      ],
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({
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
          width: 84,
          child: Text(
            label,
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: const Color(0xFF64748B)),
          ),
        ),
        Expanded(
          child: Text(
            value,
            style: Theme.of(context).textTheme.titleMedium,
          ),
        ),
      ],
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
    default:
      return recordType;
  }
}

String _formatDateTimeLabel(DateTime dateTime) {
  final String month = dateTime.month.toString().padLeft(2, '0');
  final String day = dateTime.day.toString().padLeft(2, '0');
  final String hour = dateTime.hour.toString().padLeft(2, '0');
  final String minute = dateTime.minute.toString().padLeft(2, '0');
  return '${dateTime.year}-$month-$day $hour:$minute';
}
