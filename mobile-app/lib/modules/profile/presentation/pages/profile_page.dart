import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/family/presentation/pages/family_join_page.dart';
import 'package:petlife_mobile_app/modules/family/presentation/pages/family_management_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';

/// 我的页。
class ProfilePage extends StatefulWidget {
  const ProfilePage({
    super.key,
    required this.currentUser,
    required this.onCurrentUserChanged,
    required this.onLogoutCompleted,
  });

  final CurrentUserSnapshot currentUser;
  final VoidCallback onCurrentUserChanged;
  final VoidCallback onLogoutCompleted;

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  bool _isLoggingOut = false;

  Future<void> _openFamilyManagement() async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => FamilyManagementPage(currentUser: widget.currentUser),
      ),
    );
  }

  Future<void> _openFamilyJoinPage() async {
    final bool? joined = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => const FamilyJoinPage(),
      ),
    );
    if (joined == true) {
      widget.onCurrentUserChanged();
    }
  }

  Future<void> _logout() async {
    if (_isLoggingOut) {
      return;
    }

    setState(() {
      _isLoggingOut = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      await repository.logout();
      if (!mounted) {
        return;
      }
      widget.onLogoutCompleted();
    } finally {
      if (mounted) {
        setState(() {
          _isLoggingOut = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _ProfileHeader(currentUser: widget.currentUser),
        const SizedBox(height: 16),
        PageSection(
          title: '我的陪伴资料',
          description: '这里会整理你、家庭和当前宠物之间的陪伴关系。',
          child: Column(
            children: [
              _AccountSummary(currentUser: widget.currentUser),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: FilledButton.tonal(
                  onPressed: _openFamilyManagement,
                  child: const Text('家庭共养管理'),
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  onPressed: _openFamilyJoinPage,
                  child: const Text('通过邀请码加入家庭'),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '账号与安全',
          description: '如果需要切换账号，可以从这里安全退出。',
          child: SizedBox(
            width: double.infinity,
            child: OutlinedButton(
              onPressed: _isLoggingOut ? null : _logout,
              child: Text(_isLoggingOut ? '退出中...' : '退出登录'),
            ),
          ),
        ),
      ],
    );
  }
}

class _ProfileHeader extends StatelessWidget {
  const _ProfileHeader({required this.currentUser});

  final CurrentUserSnapshot currentUser;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;
    final String avatarText = currentUser.nickname.isEmpty
        ? '宠'
        : currentUser.nickname.substring(0, 1);

    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFECDD),
          Color(0xFFFFFAF5),
        ],
      ),
      child: Row(
        children: [
          Container(
            width: 62,
            height: 62,
            decoration: BoxDecoration(
              color: AppThemePalette.surface,
              borderRadius: BorderRadius.circular(22),
            ),
            child: Center(
              child: Text(
                avatarText,
                style: textTheme.titleLarge?.copyWith(
                  color: AppThemePalette.primaryDeep,
                ),
              ),
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(currentUser.nickname, style: textTheme.titleLarge),
                const SizedBox(height: 6),
                Text(
                  currentUser.familyName,
                  style: textTheme.bodyMedium?.copyWith(
                    color: AppThemePalette.body,
                  ),
                ),
                const SizedBox(height: 10),
                CompanionPill(
                  label: '正在陪伴 ${currentUser.currentPet.petName}',
                  icon: Icons.favorite_border_rounded,
                  backgroundColor: AppThemePalette.surface,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _AccountSummary extends StatelessWidget {
  const _AccountSummary({required this.currentUser});

  final CurrentUserSnapshot currentUser;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _SummaryCard(
          label: '家庭',
          value: currentUser.familyName,
          icon: Icons.home_outlined,
        ),
        const SizedBox(height: 12),
        _SummaryCard(
          label: '当前宠物',
          value: currentUser.currentPet.petName,
          icon: Icons.pets_outlined,
        ),
        const SizedBox(height: 12),
        _SummaryCard(
          label: '宠物品种',
          value: currentUser.currentPet.breed,
          icon: Icons.bookmark_border_rounded,
        ),
        const SizedBox(height: 12),
        _SummaryCard(
          label: '账号编号',
          value: currentUser.userId,
          icon: Icons.perm_identity_outlined,
        ),
      ],
    );
  }
}

class _SummaryCard extends StatelessWidget {
  const _SummaryCard({
    required this.label,
    required this.value,
    required this.icon,
  });

  final String label;
  final String value;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return CompanionCard(
      radius: 22,
      color: AppThemePalette.surfaceRaised,
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: AppThemePalette.warmTint,
              borderRadius: BorderRadius.circular(14),
            ),
            child: Icon(icon, color: AppThemePalette.primaryDeep),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: textTheme.bodySmall?.copyWith(
                    color: AppThemePalette.muted,
                  ),
                ),
                const SizedBox(height: 4),
                Text(value, style: textTheme.titleMedium),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
