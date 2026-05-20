import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/community/presentation/pages/community_home_page.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_loading.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/home/presentation/pages/home_page.dart';
import 'package:petlife_mobile_app/modules/notification/presentation/pages/message_center_page.dart';
import 'package:petlife_mobile_app/modules/pet/presentation/pages/pet_index_page.dart';
import 'package:petlife_mobile_app/modules/pet/presentation/pages/pet_management_page.dart';
import 'package:petlife_mobile_app/modules/profile/presentation/pages/profile_page.dart';
import 'package:petlife_mobile_app/modules/service/presentation/pages/service_placeholder_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/home_aggregate_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

class AppShellPage extends StatefulWidget {
  const AppShellPage({
    super.key,
    required this.onLogoutCompleted,
  });

  final VoidCallback onLogoutCompleted;

  @override
  State<AppShellPage> createState() => _AppShellPageState();
}

class _AppShellPageState extends State<AppShellPage> {
  int _selectedIndex = 0;
  Future<_ShellViewData>? _shellViewDataFuture;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _shellViewDataFuture ??= _loadShellViewData();
  }

  Future<_ShellViewData> _loadShellViewData() async {
    final repository = PetLifeAppScope.repositoryOf(context);
    final HomeAggregateSnapshot homeAggregate =
        await repository.getHomeAggregate();

    return _ShellViewData(
      currentUser: homeAggregate.currentUser,
      dashboard: homeAggregate.dashboard,
    );
  }

  Future<void> _openPetManagement(CurrentUserSnapshot currentUser) async {
    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => PetManagementPage(
          initialCurrentPetId: currentUser.currentPetId,
        ),
      ),
    );
    if (!mounted || changed != true) {
      return;
    }
    _reloadShellViewData();
  }

  Future<void> _openMessageCenter() async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => const MessageCenterPage(),
      ),
    );
  }

  void _reloadShellViewData() {
    setState(() {
      _shellViewDataFuture = _loadShellViewData();
    });
  }

  @override
  Widget build(BuildContext context) {
    final _ShellDestination currentDestination = _destinations[_selectedIndex];

    return Scaffold(
      appBar: AppBar(
        toolbarHeight: 78,
        titleSpacing: 20,
        title: Text(currentDestination.label),
        actions: [
          IconButton.filledTonal(
            tooltip: '消息中心',
            onPressed: _openMessageCenter,
            icon: const Icon(Icons.notifications_none_rounded),
          ),
          const SizedBox(width: 8),
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: IconButton.filledTonal(
              tooltip: '刷新',
              onPressed: _reloadShellViewData,
              icon: const Icon(Icons.refresh_outlined),
            ),
          ),
        ],
      ),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: <Color>[
              Color(0xFFFFF8F2),
              Color(0xFFFFF2E7),
            ],
          ),
        ),
        child: FutureBuilder<_ShellViewData>(
          future: _shellViewDataFuture,
          builder:
              (BuildContext context, AsyncSnapshot<_ShellViewData> snapshot) {
            if (snapshot.connectionState != ConnectionState.done) {
              return const _ShellLoadingView();
            }

            if (snapshot.hasError || !snapshot.hasData) {
              return _ShellErrorView(
                message: snapshot.error?.toString() ?? '页面加载失败',
                onRetry: _reloadShellViewData,
              );
            }

            return currentDestination.builder(snapshot.data!);
          },
        ),
      ),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(14, 0, 14, 14),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(28),
          child: NavigationBar(
            selectedIndex: _selectedIndex,
            destinations: _destinations
                .map((destination) => NavigationDestination(
                      icon: Icon(destination.icon),
                      selectedIcon: Icon(destination.selectedIcon),
                      label: destination.label,
                    ))
                .toList(),
            onDestinationSelected: (int index) {
              setState(() {
                _selectedIndex = index;
              });
            },
          ),
        ),
      ),
    );
  }

  List<_ShellDestination> get _destinations => <_ShellDestination>[
        _ShellDestination(
          label: '首页',
          icon: Icons.home_outlined,
          selectedIcon: Icons.home_rounded,
          builder: (_ShellViewData data) => data.dashboard == null
              ? _NoPetShellView(
                  title: '暂无宠物档案',
                  actionLabel: '去创建宠物',
                  onAction: () => _openPetManagement(data.currentUser),
                )
              : HomePage(
                  currentUser: data.currentUser,
                  dashboard: data.dashboard!,
                  onHomeDataChanged: _reloadShellViewData,
                ),
        ),
        _ShellDestination(
          label: '宠物',
          icon: Icons.pets_outlined,
          selectedIcon: Icons.pets,
          builder: (_ShellViewData data) => data.dashboard == null
              ? _NoPetShellView(
                  title: '宠物档案还没有开始',
                  actionLabel: '创建宠物档案',
                  onAction: () => _openPetManagement(data.currentUser),
                )
              : PetIndexPage(
                  currentUser: data.currentUser,
                  dashboard: data.dashboard!,
                  onPetDataChanged: _reloadShellViewData,
                ),
        ),
        _ShellDestination(
          label: '社区',
          icon: Icons.forum_outlined,
          selectedIcon: Icons.forum_rounded,
          builder: (_) => const CommunityHomePage(),
        ),
        _ShellDestination(
          label: '服务',
          icon: Icons.medical_services_outlined,
          selectedIcon: Icons.medical_services_rounded,
          builder: (_) => const ServicePlaceholderPage(),
        ),
        _ShellDestination(
          label: '我的',
          icon: Icons.person_outline,
          selectedIcon: Icons.person_rounded,
          builder: (_ShellViewData data) => ProfilePage(
            currentUser: data.currentUser,
            onCurrentUserChanged: _reloadShellViewData,
            onLogoutCompleted: widget.onLogoutCompleted,
          ),
        ),
      ];
}

class _ShellDestination {
  const _ShellDestination({
    required this.label,
    required this.icon,
    required this.selectedIcon,
    required this.builder,
  });

  final String label;
  final IconData icon;
  final IconData selectedIcon;
  final Widget Function(_ShellViewData data) builder;
}

class _ShellViewData {
  const _ShellViewData({
    required this.currentUser,
    required this.dashboard,
  });

  final CurrentUserSnapshot currentUser;
  final PetDashboardSnapshot? dashboard;
}

class _NoPetShellView extends StatelessWidget {
  const _NoPetShellView({
    required this.title,
    required this.actionLabel,
    required this.onAction,
  });

  final String title;
  final String actionLabel;
  final VoidCallback onAction;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        CompanionCard(
          padding: const EdgeInsets.all(24),
          gradient: const LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: <Color>[
              Color(0xFFFFECDD),
              Color(0xFFFFFBF6),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const CompanionPill(
                label: '宠物档案中心',
                icon: Icons.pets_rounded,
                backgroundColor: Color(0xFFFFE0CE),
                foregroundColor: AppThemePalette.primaryDeep,
              ),
              const SizedBox(height: 14),
              Text(title, style: Theme.of(context).textTheme.headlineSmall),
              const SizedBox(height: 18),
              FilledButton(
                onPressed: onAction,
                child: Text(actionLabel),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        const CompanionEmptyState(
          title: '暂无宠物档案',
          description: '提醒、健康记录、日常片段和成长时间轴都会围绕当前宠物慢慢整理出来。',
          icon: Icons.auto_awesome_outlined,
        ),
      ],
    );
  }
}

class _ShellLoadingView extends StatelessWidget {
  const _ShellLoadingView();

  @override
  Widget build(BuildContext context) {
    return const CompanionPageLoading(
      title: '正在整理今天的宠物生活',
      description: '首页、提醒和最近记录会一起准备好，尽量保持页面结构稳定。',
      icon: Icons.pets_rounded,
      layout: CompanionLoadingLayout.detail,
    );
  }
}

class _ShellErrorView extends StatelessWidget {
  const _ShellErrorView({
    required this.message,
    required this.onRetry,
  });

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Container(
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            color: AppThemePalette.surface,
            borderRadius: BorderRadius.circular(28),
            border: Border.all(color: AppThemePalette.line),
            boxShadow: AppThemePalette.softShadow,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.cloud_off_outlined,
                size: 34,
                color: AppThemePalette.primaryDeep,
              ),
              const SizedBox(height: 14),
              Text('暂时没连上宠物生活管家', style: textTheme.titleLarge),
              const SizedBox(height: 12),
              Text(
                message,
                textAlign: TextAlign.center,
                style: textTheme.bodyMedium,
              ),
              const SizedBox(height: 18),
              FilledButton(
                onPressed: onRetry,
                child: const Text('重新整理页面'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
