import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/pet/presentation/pages/pet_editor_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_detail_snapshot.dart';

/// 宠物管理页。
///
/// 当前阶段优先补齐多宠列表、当前宠物切换和主档编辑能力，
/// 让用户端后续的健康、提醒、日常都能建立在稳定的宠物上下文上。
class PetManagementPage extends StatefulWidget {
  const PetManagementPage({
    super.key,
    required this.initialCurrentPetId,
  });

  final String initialCurrentPetId;

  @override
  State<PetManagementPage> createState() => _PetManagementPageState();
}

class _PetManagementPageState extends State<PetManagementPage> {
  bool _didLoad = false;
  bool _isLoading = false;
  bool _isSwitchingPet = false;
  bool _hasChanges = false;
  String? _errorMessage;
  late String _currentPetId;
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
      final List<PetDetailSnapshot> pets = await repository.listPets();
      if (!mounted) {
        return;
      }

      setState(() {
        _pets = pets;
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
          title: const Text('宠物管理'),
          leading: IconButton(
            onPressed: () => Navigator.of(context).pop(_hasChanges),
            icon: const Icon(Icons.arrow_back),
          ),
        ),
        body: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            PageSection(
              title: '当前宠物上下文',
              description: '首页、提醒、健康记录和萌宠日常默认都基于当前宠物展示，因此切换必须明确且可追踪。',
              child: _CurrentPetSummary(
                currentPet: _findCurrentPet(),
                onCreatePetPressed: _openCreatePetPage,
              ),
            ),
            const SizedBox(height: 16),
            PageSection(
              title: '我的宠物',
              description: '先完成多宠查看、切换和主档编辑，后续再接更细的档案页与媒体头像能力。',
              child: _buildPetList(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPetList() {
    if (_isLoading && _pets.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null && _pets.isEmpty) {
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
            onPressed: _loadPets,
            child: const Text('重新加载'),
          ),
        ],
      );
    }

    if (_pets.isEmpty) {
      return const Text('当前还没有宠物，请先创建第一只宠物。');
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

class _CurrentPetSummary extends StatelessWidget {
  const _CurrentPetSummary({
    required this.currentPet,
    required this.onCreatePetPressed,
  });

  final PetDetailSnapshot? currentPet;
  final VoidCallback onCreatePetPressed;

  @override
  Widget build(BuildContext context) {
    if (currentPet == null) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '还没有可用的当前宠物。',
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: const Color(0xFF64748B)),
          ),
          const SizedBox(height: 12),
          FilledButton(
            onPressed: onCreatePetPressed,
            child: const Text('创建宠物'),
          ),
        ],
      );
    }

    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(currentPet!.petName,
                  style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 6),
              Text(
                '${_toLocalizedPetType(currentPet!.petType)} · ${currentPet!.breed} · ${_toLocalizedGender(currentPet!.gender)}',
                style: Theme.of(context)
                    .textTheme
                    .bodyMedium
                    ?.copyWith(color: const Color(0xFF64748B)),
              ),
            ],
          ),
        ),
        FilledButton(
          onPressed: onCreatePetPressed,
          child: const Text('新增宠物'),
        ),
      ],
    );
  }
}

class _PetListCard extends StatelessWidget {
  const _PetListCard({
    required this.pet,
    required this.isCurrentPet,
    required this.isSwitchingPet,
    required this.onSwitchPressed,
    required this.onEditPressed,
  });

  final PetDetailSnapshot pet;
  final bool isCurrentPet;
  final bool isSwitchingPet;
  final VoidCallback onSwitchPressed;
  final VoidCallback onEditPressed;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Container(
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
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(pet.petName, style: textTheme.titleMedium),
                    const SizedBox(height: 4),
                    Text(
                      '${_toLocalizedPetType(pet.petType)} · ${pet.breed} · ${_toLocalizedGender(pet.gender)}',
                      style: textTheme.bodyMedium
                          ?.copyWith(color: const Color(0xFF64748B)),
                    ),
                  ],
                ),
              ),
              if (isCurrentPet)
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: const Color(0xFFDCFCE7),
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    '当前宠物',
                    style: textTheme.bodyMedium
                        ?.copyWith(color: const Color(0xFF166534)),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            _buildPetMeta(pet),
            style:
                textTheme.bodyMedium?.copyWith(color: const Color(0xFF64748B)),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              OutlinedButton(
                onPressed:
                    isCurrentPet || isSwitchingPet ? null : onSwitchPressed,
                child: Text(isCurrentPet ? '已在使用' : '设为当前'),
              ),
              const SizedBox(width: 12),
              TextButton(
                onPressed: onEditPressed,
                child: const Text('编辑资料'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

String _buildPetMeta(PetDetailSnapshot pet) {
  final List<String> parts = <String>[
    '绝育状态：${_toLocalizedNeuterStatus(pet.neuterStatus)}',
    if (pet.birthday != null) '生日：${_formatDateLabel(pet.birthday)}',
    if (pet.adoptDate != null) '到家：${_formatDateLabel(pet.adoptDate)}',
  ];
  return parts.join(' · ');
}

String _toLocalizedPetType(String petType) {
  switch (petType) {
    case 'cat':
      return '猫';
    case 'dog':
      return '狗';
    case 'other':
      return '其他';
    default:
      return petType;
  }
}

String _toLocalizedGender(String gender) {
  switch (gender) {
    case 'female':
      return '母';
    case 'male':
      return '公';
    default:
      return gender;
  }
}

String _toLocalizedNeuterStatus(String neuterStatus) {
  switch (neuterStatus) {
    case 'completed':
      return '已完成';
    case 'pending':
      return '未完成';
    default:
      return '暂不确定';
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
