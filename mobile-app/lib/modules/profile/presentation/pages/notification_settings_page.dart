import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/user_settings_snapshot.dart';

/// 通知与权限设置页。
class NotificationSettingsPage extends StatefulWidget {
  const NotificationSettingsPage({
    super.key,
    required this.initialSettings,
  });

  final UserSettingsSnapshot initialSettings;

  @override
  State<NotificationSettingsPage> createState() =>
      _NotificationSettingsPageState();
}

class _NotificationSettingsPageState extends State<NotificationSettingsPage> {
  late bool _notificationEnabled;
  late String _privacyLevel;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    _notificationEnabled = widget.initialSettings.notificationEnabled;
    _privacyLevel = widget.initialSettings.privacyLevel;
  }

  Future<void> _submit() async {
    if (_isSubmitting) {
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      await repository.updateNotificationSettings(
        notificationEnabled: _notificationEnabled,
        privacyLevel: _privacyLevel,
      );
      if (!mounted) {
        return;
      }
      showCompanionSuccessFeedback(context, '通知与权限已保存');
      Navigator.of(context).pop(true);
    } catch (error) {
      if (!mounted) {
        return;
      }
      showCompanionErrorFeedback(context, error.toString());
    } finally {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('通知与权限'),
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
            CompanionCard(
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
                    label: '通知偏好',
                    icon: Icons.notifications_active_outlined,
                    backgroundColor: Color(0xFFFFE1CF),
                    foregroundColor: AppThemePalette.primaryDeep,
                  ),
                  const SizedBox(height: 12),
                  Text('把提醒方式调成你舒服的节奏',
                      style: Theme.of(context).textTheme.headlineSmall),
                  const SizedBox(height: 10),
                  Text(
                    '这里的设置会作为后续消息中心、提醒通知和服务预约通知的统一偏好。',
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            PageSection(
              title: '通知开关',
              description: '先决定是否接收 App 内的消息与提醒。系统层通知权限仍需要在设备设置中开启。',
              child: SwitchListTile.adaptive(
                value: _notificationEnabled,
                activeColor: AppThemePalette.primaryDeep,
                contentPadding: EdgeInsets.zero,
                title: const Text('接收消息与提醒'),
                subtitle: Text(
                  _notificationEnabled
                      ? '后续消息中心和提醒通知会按你的偏好送达。'
                      : '关闭后将不再接收新的 App 内通知提醒。',
                ),
                onChanged: (bool value) {
                  setState(() {
                    _notificationEnabled = value;
                  });
                },
              ),
            ),
            const SizedBox(height: 16),
            PageSection(
              title: '默认隐私偏好',
              description: '这会作为后续记录与社区内容默认可见范围的偏好基线。',
              child: DropdownButtonFormField<String>(
                value: _privacyLevel,
                decoration: const InputDecoration(labelText: '隐私偏好'),
                items: const [
                  DropdownMenuItem(
                    value: 'normal',
                    child: Text('标准模式'),
                  ),
                  DropdownMenuItem(
                    value: 'private',
                    child: Text('更注重隐私'),
                  ),
                ],
                onChanged: (String? value) {
                  if (value == null) {
                    return;
                  }
                  setState(() {
                    _privacyLevel = value;
                  });
                },
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(16, 12, 16, 16),
        child: FilledButton(
          onPressed: _isSubmitting ? null : _submit,
          child: Text(_isSubmitting ? '保存中...' : '保存设置'),
        ),
      ),
    );
  }
}
