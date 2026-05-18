import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/modules/profile/presentation/pages/city_selection_page.dart';
import 'package:petlife_mobile_app/modules/profile/presentation/pages/notification_settings_page.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/user_settings_snapshot.dart';

/// 设置页。
class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  late final TextEditingController _nicknameController;
  bool _didLoad = false;
  bool _isLoading = false;
  bool _isSaving = false;
  bool _hasChanges = false;
  String? _errorMessage;
  UserSettingsSnapshot? _settings;
  ({String? code, String? name})? _selectedCity;

  @override
  void initState() {
    super.initState();
    _nicknameController = TextEditingController();
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    super.dispose();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final UserSettingsSnapshot settings = await repository.getUserSettings();
      if (!mounted) {
        return;
      }
      _nicknameController.text = settings.nickname;
      setState(() {
        _settings = settings;
        _selectedCity = (
          code: settings.cityCode,
          name: settings.cityName,
        );
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

  Future<void> _openCitySelection() async {
    final CitySelectionResult? selected =
        await Navigator.of(context).push<CitySelectionResult>(
      MaterialPageRoute<CitySelectionResult>(
        builder: (_) => CitySelectionPage(
          initialCityCode: _selectedCity?.code,
        ),
      ),
    );
    if (!mounted || selected == null) {
      return;
    }

    setState(() {
      _selectedCity = selected;
    });
  }

  Future<void> _openNotificationSettings() async {
    final UserSettingsSnapshot? settings = _settings;
    if (settings == null) {
      return;
    }

    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => NotificationSettingsPage(initialSettings: settings),
      ),
    );
    if (!mounted || changed != true) {
      return;
    }

    _hasChanges = true;
    await _loadSettings();
  }

  Future<void> _submit() async {
    if (_isSaving || !_formKey.currentState!.validate()) {
      return;
    }

    final UserSettingsSnapshot? settings = _settings;
    if (settings == null) {
      return;
    }

    final String normalizedNickname = _nicknameController.text.trim();
    final bool nicknameChanged = normalizedNickname != settings.nickname;
    final bool cityChanged = _selectedCity?.code != settings.cityCode ||
        _selectedCity?.name != settings.cityName;

    if (!nicknameChanged && !cityChanged) {
      Navigator.of(context).pop(_hasChanges);
      return;
    }

    setState(() {
      _isSaving = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      UserSettingsSnapshot nextSettings = settings;
      if (nicknameChanged) {
        nextSettings = await repository.updateUserProfile(
          nickname: normalizedNickname,
        );
      }
      if (cityChanged) {
        nextSettings = await repository.updateUserCity(
          cityCode: _selectedCity?.code ?? '',
          cityName: _selectedCity?.name ?? '',
        );
      }
      if (!mounted) {
        return;
      }
      _settings = nextSettings;
      _selectedCity = (
        code: nextSettings.cityCode,
        name: nextSettings.cityName,
      );
      _hasChanges = true;
      showCompanionSuccessFeedback(context, '设置已保存');
      Navigator.of(context).pop(true);
    } catch (error) {
      if (!mounted) {
        return;
      }
      showCompanionErrorFeedback(context, error.toString());
    } finally {
      if (mounted) {
        setState(() {
          _isSaving = false;
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
          title: const Text('设置'),
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
          child: _buildBody(),
        ),
        bottomNavigationBar: SafeArea(
          minimum: const EdgeInsets.fromLTRB(16, 12, 16, 16),
          child: FilledButton(
            onPressed: _isSaving ? null : _submit,
            child: Text(_isSaving ? '保存中...' : '保存设置'),
          ),
        ),
      ),
    );
  }

  Widget _buildBody() {
    if (_isLoading && _settings == null) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null && _settings == null) {
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          CompanionEmptyState(
            title: '设置暂时没有加载出来',
            description: _errorMessage!,
            icon: Icons.cloud_off_outlined,
            actionLabel: '重新加载',
            onAction: _loadSettings,
          ),
        ],
      );
    }

    final UserSettingsSnapshot? settings = _settings;
    if (settings == null) {
      return const SizedBox.shrink();
    }

    return Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          CompanionCard(
            padding: const EdgeInsets.all(22),
            gradient: const LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: <Color>[
                Color(0xFFFFEBDC),
                Color(0xFFFFFAF4),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const CompanionPill(
                  label: '账户设置',
                  icon: Icons.manage_accounts_outlined,
                  backgroundColor: Color(0xFFFFE1CF),
                  foregroundColor: AppThemePalette.primaryDeep,
                ),
                const SizedBox(height: 12),
                Text('把你的陪伴资料整理好',
                    style: Theme.of(context).textTheme.headlineSmall),
                const SizedBox(height: 10),
                Text(
                  '昵称、城市和通知偏好都会在这里统一管理，后续社区同城与服务中心也会直接用到这些设置。',
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          PageSection(
            title: '个人资料',
            description: '先把最基础的账户资料整理好，后续社区互动和家庭协作都更清楚。',
            child: Column(
              children: [
                TextFormField(
                  controller: _nicknameController,
                  decoration: const InputDecoration(
                    labelText: '昵称',
                    hintText: '请输入昵称',
                  ),
                  validator: (String? value) {
                    final String text = value?.trim() ?? '';
                    if (text.isEmpty) {
                      return '请输入昵称';
                    }
                    if (text.length > 50) {
                      return '昵称长度不能超过 50 个字符';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 16),
                TextFormField(
                  initialValue: settings.mobile,
                  readOnly: true,
                  decoration: const InputDecoration(
                    labelText: '手机号',
                  ),
                ),
                const SizedBox(height: 16),
                TextFormField(
                  initialValue: settings.userId,
                  readOnly: true,
                  decoration: const InputDecoration(
                    labelText: '账号编号',
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          PageSection(
            title: '城市设置',
            description: '社区同城内容和后续服务入口都会以这里的城市为默认值。',
            child: Column(
              children: [
                CompanionCard(
                  color: AppThemePalette.surfaceRaised,
                  radius: 22,
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('当前城市',
                                style: Theme.of(context).textTheme.titleMedium),
                            const SizedBox(height: 6),
                            Text(
                              _selectedCity?.name ?? '暂未选择',
                              style: Theme.of(context)
                                  .textTheme
                                  .bodyMedium
                                  ?.copyWith(
                                    color: AppThemePalette.muted,
                                  ),
                            ),
                          ],
                        ),
                      ),
                      FilledButton.tonal(
                        onPressed: _openCitySelection,
                        child: const Text('重新选择'),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          PageSection(
            title: '通知与权限',
            description: '把消息与隐私偏好整理好，后面消息中心和提醒通知都会按这里的设置工作。',
            child: CompanionCard(
              color: AppThemePalette.surfaceRaised,
              radius: 22,
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('通知与权限设置',
                            style: Theme.of(context).textTheme.titleMedium),
                        const SizedBox(height: 6),
                        Text(
                          settings.notificationEnabled
                              ? '当前已开启消息与提醒'
                              : '当前已关闭消息与提醒',
                          style:
                              Theme.of(context).textTheme.bodyMedium?.copyWith(
                                    color: AppThemePalette.muted,
                                  ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 12),
                  OutlinedButton(
                    onPressed: _openNotificationSettings,
                    child: const Text('进入设置'),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
