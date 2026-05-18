import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
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

    final bool confirmed = await showCompanionConfirmSheet(
      context,
      title: '删除健康记录',
      description: '删除后这条记录会从健康档案里移除，确认继续吗？',
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
      await repository.deleteHealthRecord(
        petId: widget.petId,
        healthRecordId: widget.healthRecordId,
      );
      if (!mounted) {
        return;
      }
      showCompanionSuccessFeedback(context, '健康记录已删除');
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
    final HealthRecordSnapshot? healthRecord = _healthRecord;

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
          child: _buildBody(healthRecord),
        ),
        bottomNavigationBar: healthRecord == null
            ? null
            : SafeArea(
                minimum: const EdgeInsets.fromLTRB(16, 12, 16, 16),
                child: OutlinedButton(
                  onPressed: _isDeleting ? null : _deleteHealthRecord,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppThemePalette.danger,
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
          CompanionEmptyState(
            title: '详情暂时没有加载出来',
            description: _errorMessage!,
            icon: Icons.cloud_off_outlined,
            actionLabel: '重新加载',
            onAction: _loadHealthRecord,
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
        _HealthDetailHeroCard(
          petName: widget.petName,
          healthRecord: healthRecord,
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '记录信息',
          description: '把关键时间和数值整理清楚，后面回看时会更有参考价值。',
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
              if (healthRecord.hospitalName != null) ...[
                const SizedBox(height: 12),
                _InfoRow(label: '医院', value: healthRecord.hospitalName!),
              ],
              if (healthRecord.doctorName != null) ...[
                const SizedBox(height: 12),
                _InfoRow(label: '医生', value: healthRecord.doctorName!),
              ],
              if (healthRecord.severityLevel != null) ...[
                const SizedBox(height: 12),
                _InfoRow(
                  label: '严重程度',
                  value: _toLocalizedSeverityLevel(healthRecord.severityLevel!),
                ),
              ],
              if (healthRecord.resultSummary != null) ...[
                const SizedBox(height: 12),
                _InfoRow(label: '结果', value: healthRecord.resultSummary!),
              ],
              if (healthRecord.nextReminderAt != null) ...[
                const SizedBox(height: 12),
                _InfoRow(
                  label: '下次提醒',
                  value: _formatDateTimeLabel(healthRecord.nextReminderAt!),
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
          description: '把医院、药量、检查结果和异常表现写清楚，以后回看时就不会只剩一个标题。',
          child: Text(
            healthRecord.notes == null || healthRecord.notes!.trim().isEmpty
                ? '这次还没有补充备注。'
                : healthRecord.notes!,
            style: Theme.of(context).textTheme.bodyLarge,
          ),
        ),
      ],
    );
  }
}

class _HealthDetailHeroCard extends StatelessWidget {
  const _HealthDetailHeroCard({
    required this.petName,
    required this.healthRecord,
  });

  final String petName;
  final HealthRecordSnapshot healthRecord;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFEBDD),
          Color(0xFFFFF9F2),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CompanionPill(
            label: _toLocalizedRecordType(healthRecord.recordType),
            icon: Icons.favorite_border_rounded,
            backgroundColor: const Color(0xFFFFE2D2),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(
            healthRecord.title,
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 10),
          Text(
            '$petName · ${_formatDateTimeLabel(healthRecord.occurredAt)}',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ),
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
    return CompanionCard(
      radius: 22,
      color: AppThemePalette.surfaceRaised,
      padding: const EdgeInsets.all(14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
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

String _toLocalizedSeverityLevel(String severityLevel) {
  switch (severityLevel) {
    case 'mild':
      return '轻微';
    case 'medium':
      return '中等';
    case 'severe':
      return '严重';
    default:
      return severityLevel;
  }
}

String _formatDateTimeLabel(DateTime value) {
  final String month = value.month.toString().padLeft(2, '0');
  final String day = value.day.toString().padLeft(2, '0');
  final String hour = value.hour.toString().padLeft(2, '0');
  final String minute = value.minute.toString().padLeft(2, '0');
  return '${value.year}-$month-$day $hour:$minute';
}
