import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_loading.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/notification_inbox_snapshot.dart';

class MessageCenterPage extends StatefulWidget {
  const MessageCenterPage({super.key});

  @override
  State<MessageCenterPage> createState() => _MessageCenterPageState();
}

class _MessageCenterPageState extends State<MessageCenterPage> {
  String _notifyType = 'all';
  String _readStatus = 'all';
  Future<NotificationInboxSnapshot>? _inboxFuture;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _inboxFuture ??= _loadInbox();
  }

  Future<NotificationInboxSnapshot> _loadInbox() {
    return PetLifeAppScope.repositoryOf(context).listNotifications(
      notifyType: _notifyType,
      readStatus: _readStatus,
    );
  }

  void _reloadInbox() {
    setState(() {
      _inboxFuture = _loadInbox();
    });
  }

  Future<void> _markAllRead() async {
    await PetLifeAppScope.repositoryOf(context).markNotificationsRead(
      notifyType: _notifyType,
    );
    if (!mounted) {
      return;
    }
    _reloadInbox();
  }

  Future<void> _markOneRead(String notificationId) async {
    await PetLifeAppScope.repositoryOf(context).markNotificationRead(
      notificationId,
    );
    if (!mounted) {
      return;
    }
    _reloadInbox();
  }

  void _changeNotifyType(String notifyType) {
    setState(() {
      _notifyType = notifyType;
      _inboxFuture = _loadInbox();
    });
  }

  void _changeReadStatus(String readStatus) {
    setState(() {
      _readStatus = readStatus;
      _inboxFuture = _loadInbox();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('消息中心'),
        actions: [
          IconButton(
            tooltip: '刷新',
            onPressed: _reloadInbox,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
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
        child: FutureBuilder<NotificationInboxSnapshot>(
          future: _inboxFuture,
          builder: (BuildContext context,
              AsyncSnapshot<NotificationInboxSnapshot> snapshot) {
            if (snapshot.connectionState != ConnectionState.done) {
              return const CompanionPageLoading(
                title: '正在整理消息中心',
                description: '系统消息、提醒和预约通知会按当前筛选条件排好。',
                icon: Icons.notifications_active_outlined,
                layout: CompanionLoadingLayout.list,
              );
            }

            if (snapshot.hasError || !snapshot.hasData) {
              return ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  CompanionEmptyState(
                    title: '消息暂时没有加载出来',
                    description: snapshot.error?.toString() ?? '请稍后再试一次。',
                    icon: Icons.cloud_off_outlined,
                    actionLabel: '重新加载',
                    onAction: _reloadInbox,
                  ),
                ],
              );
            }

            return _MessageInboxView(
              inbox: snapshot.data!,
              notifyType: _notifyType,
              readStatus: _readStatus,
              onNotifyTypeChanged: _changeNotifyType,
              onReadStatusChanged: _changeReadStatus,
              onMarkAllRead: _markAllRead,
              onMarkOneRead: _markOneRead,
            );
          },
        ),
      ),
    );
  }
}

class _MessageInboxView extends StatelessWidget {
  const _MessageInboxView({
    required this.inbox,
    required this.notifyType,
    required this.readStatus,
    required this.onNotifyTypeChanged,
    required this.onReadStatusChanged,
    required this.onMarkAllRead,
    required this.onMarkOneRead,
  });

  final NotificationInboxSnapshot inbox;
  final String notifyType;
  final String readStatus;
  final ValueChanged<String> onNotifyTypeChanged;
  final ValueChanged<String> onReadStatusChanged;
  final VoidCallback onMarkAllRead;
  final ValueChanged<String> onMarkOneRead;

  @override
  Widget build(BuildContext context) {
    return ListView(
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
                label: '消息中心',
                icon: Icons.mark_email_unread_outlined,
                backgroundColor: Color(0xFFFFE1CF),
                foregroundColor: AppThemePalette.primaryDeep,
              ),
              const SizedBox(height: 12),
              Text('重要变化会留在这里',
                  style: Theme.of(context).textTheme.headlineSmall),
              const SizedBox(height: 10),
              Text(
                '提醒处理、系统消息和后续服务通知都会整理成可回看的记录。',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
              const SizedBox(height: 18),
              Row(
                children: [
                  Expanded(
                    child: _UnreadMetric(
                      label: '全部未读',
                      value: inbox.unreadCount,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: _UnreadMetric(
                      label: '系统',
                      value: inbox.systemUnreadCount,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: _UnreadMetric(
                      label: '提醒',
                      value: inbox.reminderUnreadCount,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '消息筛选',
          description: '按类型和已读状态整理消息，方便快速处理。',
          actionLabel: '全部已读',
          onAction: inbox.unreadCount == 0 ? null : onMarkAllRead,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SegmentedButton<String>(
                segments: const [
                  ButtonSegment(
                    value: 'all',
                    label: Text('全部'),
                    icon: Icon(Icons.inbox_outlined),
                  ),
                  ButtonSegment(
                    value: 'system',
                    label: Text('系统'),
                    icon: Icon(Icons.campaign_outlined),
                  ),
                  ButtonSegment(
                    value: 'reminder',
                    label: Text('提醒'),
                    icon: Icon(Icons.notifications_active_outlined),
                  ),
                ],
                selected: <String>{notifyType},
                onSelectionChanged: (Set<String> values) {
                  onNotifyTypeChanged(values.first);
                },
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 10,
                runSpacing: 10,
                children: [
                  ChoiceChip(
                    label: const Text('全部状态'),
                    selected: readStatus == 'all',
                    onSelected: (_) => onReadStatusChanged('all'),
                  ),
                  ChoiceChip(
                    label: const Text('未读'),
                    selected: readStatus == 'unread',
                    onSelected: (_) => onReadStatusChanged('unread'),
                  ),
                  ChoiceChip(
                    label: const Text('已读'),
                    selected: readStatus == 'read',
                    onSelected: (_) => onReadStatusChanged('read'),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        if (inbox.items.isEmpty)
          const CompanionEmptyState(
            title: '这里暂时没有消息',
            description: '有新的提醒、系统处理结果或服务通知时，会自动出现在这里。',
            icon: Icons.notifications_none_rounded,
          )
        else
          ...inbox.items.map(
            (NotificationMessageSnapshot item) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _MessageCard(
                message: item,
                onMarkRead: () => onMarkOneRead(item.notificationId),
              ),
            ),
          ),
      ],
    );
  }
}

class _UnreadMetric extends StatelessWidget {
  const _UnreadMetric({
    required this.label,
    required this.value,
  });

  final String label;
  final int value;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: AppThemePalette.surface,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            value.toString(),
            style: Theme.of(context).textTheme.titleLarge?.copyWith(
                  color: AppThemePalette.primaryDeep,
                ),
          ),
          const SizedBox(height: 2),
          Text(label, style: Theme.of(context).textTheme.bodySmall),
        ],
      ),
    );
  }
}

class _MessageCard extends StatelessWidget {
  const _MessageCard({
    required this.message,
    required this.onMarkRead,
  });

  final NotificationMessageSnapshot message;
  final VoidCallback onMarkRead;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;
    return CompanionCard(
      color: message.read
          ? AppThemePalette.surfaceRaised
          : const Color(0xFFFFF3E9),
      radius: 24,
      padding: const EdgeInsets.all(16),
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
            child: Icon(
              _iconForType(message.notifyType),
              color: AppThemePalette.primaryDeep,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(message.title, style: textTheme.titleMedium),
                    ),
                    const SizedBox(width: 8),
                    CompanionPill(
                      label: _labelForType(message.notifyType),
                      backgroundColor: AppThemePalette.surface,
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  message.content,
                  style: textTheme.bodyMedium?.copyWith(
                    color: AppThemePalette.body,
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        _formatDateTime(message.sentAt),
                        style: textTheme.bodySmall?.copyWith(
                          color: AppThemePalette.muted,
                        ),
                      ),
                    ),
                    if (!message.read)
                      TextButton.icon(
                        onPressed: onMarkRead,
                        icon: const Icon(Icons.check_circle_outline_rounded),
                        label: const Text('标为已读'),
                      )
                    else
                      const CompanionPill(
                        label: '已读',
                        backgroundColor: Color(0xFFEAF3E7),
                        foregroundColor: Color(0xFF527A50),
                      ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  IconData _iconForType(String notifyType) {
    return switch (notifyType) {
      'reminder' => Icons.notifications_active_outlined,
      'system' => Icons.campaign_outlined,
      'interaction' => Icons.forum_outlined,
      _ => Icons.mail_outline_rounded,
    };
  }

  String _labelForType(String notifyType) {
    return switch (notifyType) {
      'reminder' => '提醒',
      'system' => '系统',
      'interaction' => '互动',
      'appointment' => '服务',
      _ => '消息',
    };
  }

  String _formatDateTime(DateTime value) {
    String twoDigits(int input) => input.toString().padLeft(2, '0');
    return '${value.year}-${twoDigits(value.month)}-${twoDigits(value.day)} '
        '${twoDigits(value.hour)}:${twoDigits(value.minute)}';
  }
}
