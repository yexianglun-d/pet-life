class NotificationInboxSnapshot {
  const NotificationInboxSnapshot({
    required this.items,
    required this.unreadCount,
    required this.systemUnreadCount,
    required this.reminderUnreadCount,
  });

  final List<NotificationMessageSnapshot> items;
  final int unreadCount;
  final int systemUnreadCount;
  final int reminderUnreadCount;
}

class NotificationMessageSnapshot {
  const NotificationMessageSnapshot({
    required this.notificationId,
    required this.notifyType,
    required this.title,
    required this.content,
    required this.readStatus,
    required this.sentAt,
    this.bizType,
    this.bizId,
    this.readAt,
  });

  final String notificationId;
  final String notifyType;
  final String? bizType;
  final String? bizId;
  final String title;
  final String content;
  final String readStatus;
  final DateTime sentAt;
  final DateTime? readAt;

  bool get read => readStatus == 'read';
}
