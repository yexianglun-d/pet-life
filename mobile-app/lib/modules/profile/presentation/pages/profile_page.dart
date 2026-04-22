import 'package:flutter/material.dart';
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
          title: '账号与家庭',
          description: '当前先展示登录用户、家庭和当前宠物摘要，后续继续接家庭共养管理。',
          child: Column(
            children: [
              _AccountSummary(currentUser: widget.currentUser),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
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
          title: '设置',
          description: '本批次先补齐退出登录闭环，其他设置项后续按模块展开。',
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

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Row(
        children: [
          Container(
            width: 58,
            height: 58,
            decoration: BoxDecoration(
              color: const Color(0xFFDCFCE7),
              borderRadius: BorderRadius.circular(20),
            ),
            child: const Icon(Icons.person_outline, color: Color(0xFF166534)),
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
                  style: textTheme.bodyMedium
                      ?.copyWith(color: const Color(0xFF64748B)),
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
        _SummaryRow(label: '用户 ID', value: currentUser.userId),
        const SizedBox(height: 12),
        _SummaryRow(label: '当前宠物', value: currentUser.currentPet.petName),
        const SizedBox(height: 12),
        _SummaryRow(label: '宠物品种', value: currentUser.currentPet.breed),
      ],
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Row(
      children: [
        SizedBox(
          width: 88,
          child: Text(
            label,
            style:
                textTheme.bodyMedium?.copyWith(color: const Color(0xFF64748B)),
          ),
        ),
        Expanded(
          child: Text(value, style: textTheme.titleMedium),
        ),
      ],
    );
  }
}
