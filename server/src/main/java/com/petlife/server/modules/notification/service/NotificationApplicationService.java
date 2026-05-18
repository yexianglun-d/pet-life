package com.petlife.server.modules.notification.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.notification.converter.NotificationConverter;
import com.petlife.server.modules.notification.domain.entity.NotificationEntity;
import com.petlife.server.modules.notification.dto.request.MarkNotificationsReadRequest;
import com.petlife.server.modules.notification.dto.response.NotificationListResponse;
import com.petlife.server.modules.notification.dto.response.NotificationResponse;
import com.petlife.server.modules.notification.persistence.NotificationPersistenceMapper;
import com.petlife.server.modules.notification.persistence.command.CreateNotificationCommand;
import com.petlife.server.modules.notification.persistence.command.MarkNotificationReadCommand;
import com.petlife.server.modules.notification.persistence.command.MarkNotificationsReadCommand;
import com.petlife.server.modules.reminder.domain.entity.ReminderEntity;
import com.petlife.server.modules.service.domain.entity.ServiceAppointmentEntity;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 站内通知应用服务。
 *
 * <p>通知中心属于事实派生层：业务模块只提交明确的业务结果，这里统一处理通知去重、
 * 用户通知开关、列表筛选与已读状态，避免各业务模块直接写通知表。</p>
 */
@Service
public class NotificationApplicationService {

    private static final String TYPE_ALL = "all";
    private static final String TYPE_SYSTEM = "system";
    private static final String TYPE_REMINDER = "reminder";
    private static final String TYPE_APPOINTMENT = "appointment";
    private static final String READ_ALL = "all";
    private static final String READ_UNREAD = "unread";
    private static final String READ_READ = "read";
    private static final String ACTION_COMPLETED = "completed";
    private static final String ACTION_SKIPPED = "skipped";
    private static final Set<String> SUPPORTED_NOTIFY_TYPES = Set.of(
        TYPE_ALL,
        TYPE_SYSTEM,
        TYPE_REMINDER,
        "interaction",
        TYPE_APPOINTMENT
    );
    private static final Set<String> SUPPORTED_READ_STATUS = Set.of(
        READ_ALL,
        READ_UNREAD,
        READ_READ
    );

    private final NotificationPersistenceMapper notificationPersistenceMapper;
    private final NotificationConverter notificationConverter;

    public NotificationApplicationService(
        NotificationPersistenceMapper notificationPersistenceMapper,
        NotificationConverter notificationConverter
    ) {
        this.notificationPersistenceMapper = notificationPersistenceMapper;
        this.notificationConverter = notificationConverter;
    }

    public NotificationListResponse listNotifications(String notifyType, String readStatus) {
        Long currentUserId = CurrentUserContext.requireUserId();
        String normalizedNotifyType = normalizeNotifyType(notifyType);
        Integer normalizedReadStatus = normalizeReadStatus(readStatus);
        String persistenceNotifyType = TYPE_ALL.equals(normalizedNotifyType) ? null : normalizedNotifyType;

        List<NotificationResponse> items = notificationPersistenceMapper
            .listNotifications(currentUserId, persistenceNotifyType, normalizedReadStatus)
            .stream()
            .map(notificationConverter::toEntity)
            .map(notificationConverter::toResponse)
            .toList();

        return buildListResponse(currentUserId, items);
    }

    @Transactional
    public NotificationResponse markNotificationRead(Long notificationId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        NotificationEntity notification = requireNotification(currentUserId, notificationId);
        if (!notification.isRead()) {
            MarkNotificationReadCommand command = new MarkNotificationReadCommand();
            command.setUserId(currentUserId);
            command.setNotificationId(notificationId);
            notificationPersistenceMapper.markNotificationRead(command);
        }
        return notificationConverter.toResponse(requireNotification(currentUserId, notificationId));
    }

    @Transactional
    public NotificationListResponse markNotificationsRead(MarkNotificationsReadRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        String normalizedNotifyType = normalizeNotifyType(request == null ? null : request.notifyType());
        String persistenceNotifyType = TYPE_ALL.equals(normalizedNotifyType) ? null : normalizedNotifyType;

        MarkNotificationsReadCommand command = new MarkNotificationsReadCommand();
        command.setUserId(currentUserId);
        command.setNotifyType(persistenceNotifyType);
        notificationPersistenceMapper.markNotificationsRead(command);
        return listNotifications(normalizedNotifyType, READ_ALL);
    }

    @Transactional
    public void createWelcomeNotificationIfAbsent(Long userId) {
        CreateNotificationCommand command = buildCreateCommand(
            userId,
            TYPE_SYSTEM,
            "user_welcome",
            userId,
            "欢迎来到宠物生活管家",
            "我们会把宠物档案、提醒、日常和重要消息整理在这里，方便你随时回看。"
        );
        notificationPersistenceMapper.insertNotificationIfAbsent(command);
    }

    @Transactional
    public void createReminderHandledNotification(
        Long actorUserId,
        Long petId,
        ReminderEntity reminder,
        String action
    ) {
        String normalizedAction = normalizeReminderAction(action);
        String petName = notificationPersistenceMapper.findPetNameById(petId);
        String actionText = ACTION_COMPLETED.equals(normalizedAction) ? "已完成" : "已跳过";
        String title = "提醒" + actionText;
        String content = "%s 的「%s」%s。".formatted(
            petName == null || petName.isBlank() ? "当前宠物" : petName,
            reminder.getTitle(),
            actionText
        );

        /**
         * 家庭共养场景下，提醒处理结果要同步给所有仍有该宠物访问权且开启通知的成员。
         * 接收人范围在 Mapper 中复用宠物访问约束，避免通知泄露给未共享该宠物的家庭成员。
         */
        for (Long recipientUserId : notificationPersistenceMapper.listNotificationRecipientUserIdsByPetId(petId)) {
            CreateNotificationCommand command = buildCreateCommand(
                recipientUserId,
                TYPE_REMINDER,
                "reminder_" + normalizedAction,
                reminder.getReminderId(),
                title,
                actorUserId.equals(recipientUserId) ? content : content + " 家庭成员已经同步更新。"
            );
            notificationPersistenceMapper.insertNotificationIfAbsent(command);
        }
    }

    @Transactional
    public void createModerationResultNotification(Long reporterUserId, Long reportId, String action) {
        boolean confirmedViolation = "confirm_violation".equals(action);
        CreateNotificationCommand command = buildCreateCommand(
            reporterUserId,
            TYPE_SYSTEM,
            "moderation_report",
            reportId,
            confirmedViolation ? "举报已处理" : "举报已关闭",
            confirmedViolation
                ? "你提交的举报已确认违规，相关内容已被处理。"
                : "你提交的举报经审核暂未认定违规，感谢你的反馈。"
        );
        notificationPersistenceMapper.insertNotificationIfAbsent(command);
    }

    @Transactional
    public void createAppointmentCreatedNotification(
        Long actorUserId,
        ServiceAppointmentEntity appointment
    ) {
        String content = "%s 的「%s」预约已提交：%s %s，等待服务方确认。".formatted(
            appointment.getPetName() == null || appointment.getPetName().isBlank() ? "当前宠物" : appointment.getPetName(),
            appointment.getProviderName(),
            appointment.getAppointmentDate(),
            appointment.getAppointmentSlot()
        );
        for (Long recipientUserId : notificationPersistenceMapper.listNotificationRecipientUserIdsByPetId(appointment.getPetId())) {
            CreateNotificationCommand command = buildCreateCommand(
                recipientUserId,
                TYPE_APPOINTMENT,
                "appointment_created",
                appointment.getAppointmentId(),
                "预约已提交",
                actorUserId.equals(recipientUserId) ? content : content + " 家庭成员已经同步收到这条预约。"
            );
            notificationPersistenceMapper.insertNotificationIfAbsent(command);
        }
    }

    private NotificationListResponse buildListResponse(Long currentUserId, List<NotificationResponse> items) {
        return new NotificationListResponse(
            items,
            notificationPersistenceMapper.countUnreadNotifications(currentUserId, null),
            notificationPersistenceMapper.countUnreadNotifications(currentUserId, TYPE_SYSTEM),
            notificationPersistenceMapper.countUnreadNotifications(currentUserId, TYPE_REMINDER)
        );
    }

    private NotificationEntity requireNotification(Long currentUserId, Long notificationId) {
        NotificationEntity notification = notificationConverter.toEntity(
            notificationPersistenceMapper.findNotificationByUserIdAndId(currentUserId, notificationId)
        );
        if (notification == null) {
            throw new BusinessException(ResponseCode.NOTIFICATION_NOT_FOUND);
        }
        return notification;
    }

    private CreateNotificationCommand buildCreateCommand(
        Long userId,
        String notifyType,
        String bizType,
        Long bizId,
        String title,
        String content
    ) {
        CreateNotificationCommand command = new CreateNotificationCommand();
        command.setUserId(userId);
        command.setNotifyType(notifyType);
        command.setBizType(bizType);
        command.setBizId(bizId);
        command.setTitle(title);
        command.setContent(content);
        return command;
    }

    private String normalizeNotifyType(String notifyType) {
        if (notifyType == null || notifyType.isBlank()) {
            return TYPE_ALL;
        }
        String normalizedNotifyType = notifyType.trim().toLowerCase();
        if (!SUPPORTED_NOTIFY_TYPES.contains(normalizedNotifyType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "通知类型不支持");
        }
        return normalizedNotifyType;
    }

    private Integer normalizeReadStatus(String readStatus) {
        if (readStatus == null || readStatus.isBlank()) {
            return null;
        }
        String normalizedReadStatus = readStatus.trim().toLowerCase();
        if (!SUPPORTED_READ_STATUS.contains(normalizedReadStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "通知已读筛选不支持");
        }
        if (READ_UNREAD.equals(normalizedReadStatus)) {
            return 0;
        }
        if (READ_READ.equals(normalizedReadStatus)) {
            return 1;
        }
        return null;
    }

    private String normalizeReminderAction(String action) {
        if (ACTION_COMPLETED.equals(action) || ACTION_SKIPPED.equals(action)) {
            return action;
        }
        throw new BusinessException(ResponseCode.BAD_REQUEST, "提醒通知动作不支持");
    }
}
