import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/pet/presentation/pages/pet_editor_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_detail_snapshot.dart';

/// 宠物详情完整页。
class PetDetailPage extends StatefulWidget {
  const PetDetailPage({
    super.key,
    required this.petId,
    required this.initialPetName,
  });

  final String petId;
  final String initialPetName;

  @override
  State<PetDetailPage> createState() => _PetDetailPageState();
}

class _PetDetailPageState extends State<PetDetailPage> {
  bool _didLoad = false;
  bool _isLoading = false;
  bool _isActing = false;
  String? _errorMessage;
  PetDetailSnapshot? _pet;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadPet();
  }

  Future<void> _loadPet() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final PetDetailSnapshot pet = await repository.getPet(widget.petId);
      if (!mounted) {
        return;
      }

      setState(() {
        _pet = pet;
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
    final PetDetailSnapshot? pet = _pet;
    if (pet == null) {
      return;
    }

    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => PetEditorPage.edit(pet: pet),
      ),
    );
    if (!mounted || changed != true) {
      return;
    }
    await _loadPet();
  }

  Future<void> _archivePet(String archiveStatus) async {
    if (_isActing) {
      return;
    }

    final bool confirmed = await _showActionSheet(
          title: archiveStatus == 'memorial' ? '纪念归档这只宠物吗' : '将这只宠物标记为送养吗',
          description: archiveStatus == 'memorial'
              ? '归档后，它会从当前宠物列表中移出，但这份成长记录仍然会被妥善保留。'
              : '送养归档后，它会从当前宠物列表中移出，家庭成员的当前宠物也会自动重建。',
          confirmLabel: archiveStatus == 'memorial' ? '确认纪念归档' : '确认送养归档',
          confirmColor: AppThemePalette.primaryDeep,
        ) ??
        false;
    if (!confirmed) {
      return;
    }
    if (!mounted) {
      return;
    }

    setState(() {
      _isActing = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      await repository.archivePet(
        petId: widget.petId,
        archiveStatus: archiveStatus,
      );
      if (!mounted) {
        return;
      }

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(archiveStatus == 'memorial' ? '已纪念归档' : '已标记为送养'),
        ),
      );
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
          _isActing = false;
        });
      }
    }
  }

  Future<void> _deletePet() async {
    if (_isActing) {
      return;
    }

    final bool confirmed = await _showActionSheet(
          title: '删除这只宠物吗',
          description: '删除后会把它从当前列表中移出，家庭成员的当前宠物会自动重建。这个动作不可恢复，请再次确认。',
          confirmLabel: '确认删除',
          confirmColor: const Color(0xFFC75B56),
        ) ??
        false;
    if (!confirmed) {
      return;
    }
    if (!mounted) {
      return;
    }

    setState(() {
      _isActing = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      await repository.deletePet(widget.petId);
      if (!mounted) {
        return;
      }

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('宠物已删除')),
      );
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
          _isActing = false;
        });
      }
    }
  }

  Future<bool?> _showActionSheet({
    required String title,
    required String description,
    required String confirmLabel,
    required Color confirmColor,
  }) {
    return showModalBottomSheet<bool>(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (BuildContext context) {
        return SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            child: CompanionCard(
              padding: const EdgeInsets.all(20),
              radius: 28,
              color: AppThemePalette.surface,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: Theme.of(context).textTheme.titleLarge),
                  const SizedBox(height: 10),
                  Text(description,
                      style: Theme.of(context).textTheme.bodyMedium),
                  const SizedBox(height: 18),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      style: FilledButton.styleFrom(
                        backgroundColor: confirmColor,
                      ),
                      onPressed: () => Navigator.of(context).pop(true),
                      child: Text(confirmLabel),
                    ),
                  ),
                  const SizedBox(height: 10),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton(
                      onPressed: () => Navigator.of(context).pop(false),
                      child: const Text('先取消'),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final PetDetailSnapshot? pet = _pet;

    return Scaffold(
      appBar: AppBar(
        title: Text(pet?.petName ?? widget.initialPetName),
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
        child: _buildBody(pet),
      ),
    );
  }

  Widget _buildBody(PetDetailSnapshot? pet) {
    if (_isLoading && pet == null) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null && pet == null) {
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          CompanionEmptyState(
            title: '宠物详情暂时没有加载出来',
            description: _errorMessage!,
            icon: Icons.cloud_off_outlined,
            actionLabel: '重新加载',
            onAction: _loadPet,
          ),
        ],
      );
    }

    if (pet == null) {
      return const SizedBox.shrink();
    }

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _PetDetailHeroCard(pet: pet),
        const SizedBox(height: 16),
        PageSection(
          title: '基础资料',
          description: '这是这只毛孩子当前最稳定的主档信息，后面的记录和提醒都会围绕它展开。',
          child: _DetailInfoSection(
            items: <_DetailItem>[
              _DetailItem(
                  label: '宠物类型', value: _toLocalizedPetType(pet.petType)),
              _DetailItem(label: '品种', value: pet.breed),
              _DetailItem(label: '性别', value: _toLocalizedGender(pet.gender)),
              _DetailItem(
                label: '生日',
                value: pet.birthday == null
                    ? '待补充'
                    : _formatDateLabel(pet.birthday),
              ),
              _DetailItem(
                label: '到家日期',
                value: pet.adoptDate == null
                    ? '待补充'
                    : _formatDateLabel(pet.adoptDate),
              ),
              _DetailItem(
                  label: '绝育状态',
                  value: _toLocalizedNeuterStatus(pet.neuterStatus)),
              _DetailItem(
                  label: '当前状态', value: _toLocalizedPetStatus(pet.status)),
            ],
          ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '照护备注',
          description: '把体重、过敏和病史留在这里，后面做提醒、服务和协作时会更安心。',
          child: Column(
            children: [
              _DetailInfoSection(
                items: <_DetailItem>[
                  _DetailItem(
                    label: '当前体重',
                    value: pet.weightKg == null ? '待补充' : '${pet.weightKg} kg',
                  ),
                ],
              ),
              const SizedBox(height: 12),
              _LongTextCard(
                title: '过敏信息',
                value: pet.allergyNotes ?? '还没有记录过敏信息',
              ),
              const SizedBox(height: 12),
              _LongTextCard(
                title: '重要病史',
                value: pet.medicalHistory ?? '还没有补充病史记录',
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '主档动作',
          description: '涉及归档和删除的动作会影响家庭成员的当前宠物上下文，所以这里会做二次确认。',
          child: Column(
            children: [
              SizedBox(
                width: double.infinity,
                child: FilledButton.tonal(
                  onPressed: _isActing ? null : _openEditPage,
                  child: const Text('编辑这份主档'),
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  onPressed: _isActing ? null : () => _archivePet('memorial'),
                  child: const Text('纪念归档'),
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  onPressed: _isActing ? null : () => _archivePet('rehomed'),
                  child: const Text('标记为送养'),
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFFC75B56),
                  ),
                  onPressed: _isActing ? null : _deletePet,
                  child: Text(_isActing ? '处理中...' : '删除宠物'),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _PetDetailHeroCard extends StatelessWidget {
  const _PetDetailHeroCard({required this.pet});

  final PetDetailSnapshot pet;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFE9DC),
          Color(0xFFFFFBF7),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CompanionPill(
            label: _toLocalizedPetStatus(pet.status),
            icon: Icons.favorite_outline_rounded,
            backgroundColor: const Color(0xFFFFE1D2),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Container(
                width: 72,
                height: 72,
                decoration: BoxDecoration(
                  color: AppThemePalette.surface,
                  borderRadius: BorderRadius.circular(24),
                ),
                child: Icon(
                  pet.petType == 'dog'
                      ? Icons.pets_rounded
                      : Icons.cruelty_free_outlined,
                  size: 34,
                  color: AppThemePalette.primaryDeep,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(pet.petName,
                        style: Theme.of(context).textTheme.headlineSmall),
                    const SizedBox(height: 8),
                    Text(
                      '${_toLocalizedPetType(pet.petType)} · ${pet.breed} · ${_toLocalizedGender(pet.gender)}',
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              CompanionPill(
                label: _toLocalizedNeuterStatus(pet.neuterStatus),
                backgroundColor: AppThemePalette.surface,
              ),
              if (pet.weightKg != null)
                CompanionPill(
                  label: '体重 ${pet.weightKg} kg',
                  backgroundColor: AppThemePalette.surface,
                ),
              if (pet.birthday != null)
                CompanionPill(
                  label: '生日 ${_formatDateLabel(pet.birthday)}',
                  backgroundColor: AppThemePalette.surface,
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _DetailInfoSection extends StatelessWidget {
  const _DetailInfoSection({required this.items});

  final List<_DetailItem> items;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: items
          .map(
            (_DetailItem item) => Padding(
              padding: EdgeInsets.only(bottom: item == items.last ? 0 : 12),
              child: CompanionCard(
                radius: 22,
                color: AppThemePalette.surfaceRaised,
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        item.label,
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                              color: AppThemePalette.muted,
                            ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        item.value,
                        textAlign: TextAlign.right,
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          )
          .toList(),
    );
  }
}

class _LongTextCard extends StatelessWidget {
  const _LongTextCard({
    required this.title,
    required this.value,
  });

  final String title;
  final String value;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      radius: 22,
      color: AppThemePalette.surfaceRaised,
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          Text(value, style: Theme.of(context).textTheme.bodyMedium),
        ],
      ),
    );
  }
}

class _DetailItem {
  const _DetailItem({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;
}

String _toLocalizedPetType(String petType) {
  switch (petType) {
    case 'cat':
      return '猫咪';
    case 'dog':
      return '狗狗';
    default:
      return '其他';
  }
}

String _toLocalizedGender(String gender) {
  switch (gender) {
    case 'male':
      return '公';
    case 'female':
      return '母';
    default:
      return gender;
  }
}

String _toLocalizedNeuterStatus(String neuterStatus) {
  switch (neuterStatus) {
    case 'completed':
      return '已绝育';
    case 'unknown':
      return '暂不确定';
    default:
      return '未绝育';
  }
}

String _toLocalizedPetStatus(String status) {
  switch (status) {
    case 'memorial':
      return '纪念归档';
    case 'rehomed':
      return '已送养';
    default:
      return '活跃陪伴中';
  }
}

String _formatDateLabel(DateTime? value) {
  if (value == null) {
    return '';
  }
  final String month = value.month.toString().padLeft(2, '0');
  final String day = value.day.toString().padLeft(2, '0');
  return '${value.year}-$month-$day';
}
