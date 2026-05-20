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
                  Text('通知设置',
                      style: Theme.of(context).textTheme.headlineSmall),
                ],
              ),
            ),
            const SizedBox(height: 16),
            PageSection(
              title: '通知开关',
              description: '当前只影响站内消息与提醒偏好；短信和系统推送暂不在 App 内配置。',
              child: SwitchListTile.adaptive(
                value: _notificationEnabled,
                activeColor: AppThemePalette.primaryDeep,
                contentPadding: EdgeInsets.zero,
                title: const Text('接收消息与提醒'),
                onChanged: (bool value) {
                  setState(() {
                    _notificationEnabled = value;
                  });
                },
              ),
            ),
            const SizedBox(height: 16),
            PageSection(
              title: '通知渠道',
              description:
                  '后台可维护站内信、短信和 Push 渠道配置；当前 App 只展示站内消息，不提供短信或系统推送配置入口。',
              child: const Column(
                children: [
                  _ChannelNoticeTile(
                    icon: Icons.mark_email_unread_outlined,
                    title: '站内消息',
                    description: '通过消息中心查看提醒、系统和预约通知。',
                  ),
                  SizedBox(height: 10),
                  _ChannelNoticeTile(
                    icon: Icons.sms_outlined,
                    title: '功能未完成：缺少真实短信供应商',
                    description: '真实短信通道尚未接入，本端不展示短信开关。',
                  ),
                  SizedBox(height: 10),
                  _ChannelNoticeTile(
                    icon: Icons.notifications_none_rounded,
                    title: '功能未完成：缺少 Push SDK',
                    description: '暂未接入 Push SDK 和设备推送授权配置。',
                  ),
                ],
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

class _ChannelNoticeTile extends StatelessWidget {
  const _ChannelNoticeTile({
    required this.icon,
    required this.title,
    required this.description,
  });

  final IconData icon;
  final String title;
  final String description;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      color: AppThemePalette.surfaceRaised,
      radius: 22,
      padding: const EdgeInsets.all(14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 42,
            height: 42,
            decoration: BoxDecoration(
              color: AppThemePalette.surface,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Icon(icon, color: AppThemePalette.primaryDeep),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: Theme.of(context).textTheme.titleMedium),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
