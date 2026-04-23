import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/pet/presentation/pages/pet_detail_page.dart';
import 'package:petlife_mobile_app/modules/pet/presentation/pages/pet_editor_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_detail_snapshot.dart';

/// 宠物管理页。
class PetManagementPage extends StatefulWidget {
  const PetManagementPage({
    super.key,
    required this.initialCurrentPetId,
  });

  final String? initialCurrentPetId;

  @override
  State<PetManagementPage> createState() => _PetManagementPageState();
}

class _PetManagementPageState extends State<PetManagementPage> {
  bool _didLoad = false;
  bool _isLoading = false;
  bool _isSwitchingPet = false;
  bool _hasChanges = false;
  String? _errorMessage;
  String? _currentPetId;
  List<PetDetailSnapshot> _pets = const <PetDetailSnapshot>[];

  @override
  void initState() {
    super.initState();
    _currentPetId = widget.initialCurrentPetId;
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadPets();
  }

  Future<void> _loadPets() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final List<Object> results = await Future.wait<Object>(<Future<Object>>[
        repository.listPets(),
        repository.getCurrentUser(),
      ]);
      final List<PetDetailSnapshot> pets =
          results[0] as List<PetDetailSnapshot>;
      final CurrentUserSnapshot currentUser = results[1] as CurrentUserSnapshot;
      if (!mounted) {
        return;
      }

      setState(() {
        _pets = pets;
        _currentPetId = currentUser.currentPetId;
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

  Future<void> _openCreatePetPage() async {
    final bool? created = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => const PetEditorPage.create(),
      ),
    );
    if (!mounted || created != true) {
      return;
    }

    _hasChanges = true;
    await _loadPets();
  }

  Future<void> _openEditPetPage(PetDetailSnapshot pet) async {
    final bool? updated = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => PetEditorPage.edit(pet: pet),
      ),
    );
    if (!mounted || updated != true) {
      return;
    }

    _hasChanges = true;
    await _loadPets();
  }

  Future<void> _openPetDetailPage(PetDetailSnapshot pet) async {
    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => PetDetailPage(
          petId: pet.petId,
          initialPetName: pet.petName,
        ),
      ),
    );
    if (!mounted || changed != true) {
      return;
    }

    _hasChanges = true;
    await _loadPets();
  }

  Future<void> _switchCurrentPet(PetDetailSnapshot pet) async {
    if (_isSwitchingPet || pet.petId == _currentPetId) {
      return;
    }

    setState(() {
      _isSwitchingPet = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final currentUser = await repository.updateCurrentPet(pet.petId);
      if (!mounted) {
        return;
      }

      setState(() {
        _currentPetId = currentUser.currentPetId;
      });
      _hasChanges = true;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('已切换到 ${pet.petName}')),
      );
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
          _isSwitchingPet = false;
        });
      }
    }
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
          title: const Text('宠物管理'),
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
              _PetManagementHero(
                currentPet: _findCurrentPet(),
                petCount: _pets.length,
                onCreatePetPressed: _openCreatePetPage,
              ),
              const SizedBox(height: 16),
              PageSection(
                title: '陪伴档案',
                description: '每只毛孩子都有自己的主档，切换当前宠物后，后面的提醒和记录也会跟着切过去。',
                child: _buildPetList(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPetList() {
    if (_isLoading && _pets.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 20),
        child: Center(child: CircularProgressIndicator()),
      );
    }

    if (_errorMessage != null && _pets.isEmpty) {
      return CompanionEmptyState(
        title: '宠物列表暂时没有加载出来',
        description: _errorMessage!,
        icon: Icons.cloud_off_outlined,
        actionLabel: '重新加载',
        onAction: _loadPets,
      );
    }

    if (_pets.isEmpty) {
      return CompanionEmptyState(
        title: '还没有宠物主档',
        description: '先把第一只毛孩子的资料建好，健康、提醒、日常和时间轴都会慢慢长出来。',
        icon: Icons.pets_outlined,
        actionLabel: '创建宠物',
        onAction: _openCreatePetPage,
      );
    }

    return Column(
      children: _pets
          .map(
            (PetDetailSnapshot pet) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _PetListCard(
                pet: pet,
                isCurrentPet: pet.petId == _currentPetId,
                isSwitchingPet: _isSwitchingPet,
                onDetailPressed: () => _openPetDetailPage(pet),
                onSwitchPressed: () => _switchCurrentPet(pet),
                onEditPressed: () => _openEditPetPage(pet),
              ),
            ),
          )
          .toList(),
    );
  }

  PetDetailSnapshot? _findCurrentPet() {
    for (final PetDetailSnapshot pet in _pets) {
      if (pet.petId == _currentPetId) {
        return pet;
      }
    }
    return null;
  }
}

class _PetManagementHero extends StatelessWidget {
  const _PetManagementHero({
    required this.currentPet,
    required this.petCount,
    required this.onCreatePetPressed,
  });

  final PetDetailSnapshot? currentPet;
  final int petCount;
  final VoidCallback onCreatePetPressed;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFECDC),
          Color(0xFFFFFAF4),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const CompanionPill(
            label: '宠物主档',
            icon: Icons.pets_rounded,
            backgroundColor: Color(0xFFFFE1D0),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text(
            currentPet == null
                ? '先建立第一只宠物档案'
                : '现在陪在你身边的是 ${currentPet!.petName}',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 10),
          Text(
            currentPet == null
                ? '主档整理好之后，后面的健康记录、提醒和成长时间轴才会有明确归属。'
                : '${_toLocalizedPetType(currentPet!.petType)} · ${currentPet!.breed} · ${_toLocalizedGender(currentPet!.gender)}',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              CompanionPill(
                label: '一共 $petCount 只',
                backgroundColor: AppThemePalette.surface,
              ),
              if (currentPet?.birthday != null)
                CompanionPill(
                  label: '生日 ${_formatDateLabel(currentPet!.birthday)}',
                  backgroundColor: AppThemePalette.surface,
                ),
            ],
          ),
          const SizedBox(height: 18),
          FilledButton(
            onPressed: onCreatePetPressed,
            child: Text(currentPet == null ? '创建宠物' : '新增宠物'),
          ),
        ],
      ),
    );
  }
}

class _PetListCard extends StatelessWidget {
  const _PetListCard({
    required this.pet,
    required this.isCurrentPet,
    required this.isSwitchingPet,
    required this.onDetailPressed,
    required this.onSwitchPressed,
    required this.onEditPressed,
  });

  final PetDetailSnapshot pet;
  final bool isCurrentPet;
  final bool isSwitchingPet;
  final VoidCallback onDetailPressed;
  final VoidCallback onSwitchPressed;
  final VoidCallback onEditPressed;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return CompanionCard(
      padding: const EdgeInsets.all(16),
      color: AppThemePalette.surfaceRaised,
      radius: 24,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 46,
                height: 46,
                decoration: BoxDecoration(
                  color: AppThemePalette.surface,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Icon(
                  pet.petType == 'dog'
                      ? Icons.pets_rounded
                      : Icons.cruelty_free_outlined,
                  color: AppThemePalette.primaryDeep,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(pet.petName, style: textTheme.titleMedium),
                    const SizedBox(height: 4),
                    Text(
                      '${_toLocalizedPetType(pet.petType)} · ${pet.breed} · ${_toLocalizedGender(pet.gender)}',
                      style: textTheme.bodyMedium?.copyWith(
                        color: AppThemePalette.muted,
                      ),
                    ),
                  ],
                ),
              ),
              if (isCurrentPet)
                const CompanionPill(
                  label: '当前宠物',
                  backgroundColor: Color(0xFFE8F3E7),
                  foregroundColor: Color(0xFF65846D),
                ),
            ],
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              CompanionPill(
                label: _toLocalizedNeuterStatus(pet.neuterStatus),
                backgroundColor: AppThemePalette.surface,
              ),
              if (pet.birthday != null)
                CompanionPill(
                  label: '生日 ${_formatDateLabel(pet.birthday)}',
                  backgroundColor: AppThemePalette.surface,
                ),
              if (pet.adoptDate != null)
                CompanionPill(
                  label: '到家 ${_formatDateLabel(pet.adoptDate)}',
                  backgroundColor: AppThemePalette.surface,
                ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: onDetailPressed,
                  child: const Text('查看详情'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton(
                  onPressed: onEditPressed,
                  child: const Text('编辑资料'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: FilledButton.tonal(
                  onPressed:
                      isCurrentPet || isSwitchingPet ? null : onSwitchPressed,
                  child: Text(isCurrentPet ? '正在使用' : '切换为当前'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

String _toLocalizedPetType(String petType) {
  switch (petType) {
    case 'cat':
      return '猫咪';
    case 'dog':
      return '狗狗';
    default:
      return petType;
  }
}

String _toLocalizedGender(String? gender) {
  switch (gender) {
    case 'female':
      return '母';
    case 'male':
      return '公';
    default:
      return gender ?? '未知';
  }
}

String _toLocalizedNeuterStatus(String? neuterStatus) {
  switch (neuterStatus) {
    case 'completed':
      return '已绝育';
    case 'unknown':
      return '暂不确定';
    default:
      return '未绝育';
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
