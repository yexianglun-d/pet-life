import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/modules/community/presentation/pages/community_placeholder_page.dart';
import 'package:petlife_mobile_app/modules/home/presentation/pages/home_page.dart';
import 'package:petlife_mobile_app/modules/pet/presentation/pages/pet_index_page.dart';
import 'package:petlife_mobile_app/modules/profile/presentation/pages/profile_placeholder_page.dart';
import 'package:petlife_mobile_app/modules/service/presentation/pages/service_placeholder_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/pet_dashboard_snapshot.dart';

class AppShellPage extends StatefulWidget {
  const AppShellPage({super.key});

  @override
  State<AppShellPage> createState() => _AppShellPageState();
}

class _AppShellPageState extends State<AppShellPage> {
  int _selectedIndex = 0;
  Future<_ShellViewData>? _shellViewDataFuture;

  static final List<_ShellDestination> _destinations = <_ShellDestination>[
    _ShellDestination(
      label: '首页',
      icon: Icons.home_outlined,
      builder: (_ShellViewData data) => HomePage(
        currentUser: data.currentUser,
        dashboard: data.dashboard,
      ),
    ),
    _ShellDestination(
      label: '宠物',
      icon: Icons.pets_outlined,
      builder: (_ShellViewData data) => PetIndexPage(
        currentUser: data.currentUser,
        dashboard: data.dashboard,
      ),
    ),
    _ShellDestination(
      label: '社区',
      icon: Icons.forum_outlined,
      builder: (_) => const CommunityPlaceholderPage(),
    ),
    _ShellDestination(
      label: '服务',
      icon: Icons.medical_services_outlined,
      builder: (_) => const ServicePlaceholderPage(),
    ),
    _ShellDestination(
      label: '我的',
      icon: Icons.person_outline,
      builder: (_) => const ProfilePlaceholderPage(),
    ),
  ];

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _shellViewDataFuture ??= _loadShellViewData();
  }

  Future<_ShellViewData> _loadShellViewData() async {
    final repository = PetLifeAppScope.repositoryOf(context);
    final CurrentUserSnapshot currentUser = await repository.getCurrentUser();
    final PetDashboardSnapshot dashboard =
        await repository.getPetDashboard(currentUser.currentPetId);

    return _ShellViewData(
      currentUser: currentUser,
      dashboard: dashboard,
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
        title: Text(currentDestination.label),
        actions: [
          IconButton(
            tooltip: '刷新',
            onPressed: _reloadShellViewData,
            icon: const Icon(Icons.refresh_outlined),
          ),
        ],
      ),
      body: FutureBuilder<_ShellViewData>(
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
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        destinations: _destinations
            .map((destination) => NavigationDestination(
                  icon: Icon(destination.icon),
                  label: destination.label,
                ))
            .toList(),
        onDestinationSelected: (int index) {
          setState(() {
            _selectedIndex = index;
          });
        },
      ),
    );
  }
}

class _ShellDestination {
  const _ShellDestination({
    required this.label,
    required this.icon,
    required this.builder,
  });

  final String label;
  final IconData icon;
  final Widget Function(_ShellViewData data) builder;
}

class _ShellViewData {
  const _ShellViewData({
    required this.currentUser,
    required this.dashboard,
  });

  final CurrentUserSnapshot currentUser;
  final PetDashboardSnapshot dashboard;
}

class _ShellLoadingView extends StatelessWidget {
  const _ShellLoadingView();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: CircularProgressIndicator(),
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
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('接口暂不可用', style: textTheme.titleLarge),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: textTheme.bodyMedium,
            ),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: onRetry,
              child: const Text('重试'),
            ),
          ],
        ),
      ),
    );
  }
}
