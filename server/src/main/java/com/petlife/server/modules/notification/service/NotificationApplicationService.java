package com.petlife.server.modules.notification.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.notification.converter.NotificationConverter;
import com.petlife.server.modules.notification.domain.entity.NotificationEntity;
import com.petlife.server.modules.notification.dto.request.MarkNotificationsReadRequest;
import com.petlife.server.modules.notification.dto.response.NotificationListResponse;
import com.petlife.server.modules.notification.dto.response.NotificationResponse;
import com.petlife.server.modules.notification.persistence.MessageTemplatePersistenceMapper;
import com.petlife.server.modules.notification.persistence.NotificationPersistenceMapper;
import com.petlife.server.modules.notification.persistence.command.CreateNotificationCommand;
import com.petlife.server.modules.notification.persistence.command.MarkNotificationReadCommand;
import com.petlife.server.modules.notification.persistence.command.MarkNotificationsReadCommand;
import com.petlife.server.modules.notification.persistence.dataobject.MessageTemplateDataObject;
import com.petlife.server.modules.reminder.domain.entity.ReminderEntity;
import com.petlife.server.modules.service.domain.entity.ServiceAppointmentEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final String CHANNEL_INBOX = "inbox";
    private static final String READ_ALL = "all";
    private static final String READ_UNREAD = "unread";
    private static final String READ_READ = "read";
    private static final String ACTION_COMPLETED = "completed";
    private static final String ACTION_SKIPPED = "skipped";
    private static final String TEMPLATE_USER_WELCOME = "user_welcome";
    private static final String TEMPLATE_REMINDER_COMPLETED = "reminder_completed";
    private static final String TEMPLATE_REMINDER_SKIPPED = "reminder_skipped";
    private static final String TEMPLATE_MODERATION_CONFIRMED = "moderation_report_confirm_violation";
    private static final String TEMPLATE_MODERATION_DISMISSED = "moderation_report_dismiss_report";
    private static final String TEMPLATE_APPOINTMENT_CREATED = "appointment_created";
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_CONTENT_LENGTH = 500;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");
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
    private static final Map<String, DefaultNotificationTemplate> DEFAULT_INBOX_TEMPLATES = Map.of(
        TEMPLATE_USER_WELCOME,
        new DefaultNotificationTemplate(
            "欢迎来到宠物生活管家",
            "我们会把宠物档案、提醒、日常和重要消息整理在这里，方便你随时回看。"
        ),
        TEMPLATE_REMINDER_COMPLETED,
        new DefaultNotificationTemplate(
            "提醒已完成",
            "${pet_name} 的「${reminder_title}」已完成。${family_suffix}"
        ),
        TEMPLATE_REMINDER_SKIPPED,
        new DefaultNotificationTemplate(
            "提醒已跳过",
            "${pet_name} 的「${reminder_title}」已跳过。${family_suffix}"
        ),
        TEMPLATE_MODERATION_CONFIRMED,
        new DefaultNotificationTemplate(
            "举报已处理",
            "你提交的举报已确认违规，相关内容已被处理。"
        ),
        TEMPLATE_MODERATION_DISMISSED,
        new DefaultNotificationTemplate(
            "举报已关闭",
            "你提交的举报经审核暂未认定违规，感谢你的反馈。"
        ),
        TEMPLATE_APPOINTMENT_CREATED,
        new DefaultNotificationTemplate(
            "预约已提交",
            "${pet_name} 的「${provider_name}」预约已提交：${appointment_date} ${appointment_slot}，等待服务方确认。${family_suffix}"
        )
    );

    private final NotificationPersistenceMapper notificationPersistenceMapper;
    private final MessageTemplatePersistenceMapper messageTemplatePersistenceMapper;
    private final NotificationConverter notificationConverter;
    private final PushNotificationApplicationService pushNotificationApplicationService;

    public NotificationApplicationService(
        NotificationPersistenceMapper notificationPersistenceMapper,
        MessageTemplatePersistenceMapper messageTemplatePersistenceMapper,
        NotificationConverter notificationConverter,
        PushNotificationApplicationService pushNotificationApplicationService
    ) {
        this.notificationPersistenceMapper = notificationPersistenceMapper;
        this.messageTemplatePersistenceMapper = messageTemplatePersistenceMapper;
        this.notificationConverter = notificationConverter;
        this.pushNotificationApplicationService = pushNotificationApplicationService;
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
        RenderedNotificationTemplate template = renderInboxTemplate(TEMPLATE_USER_WELCOME, Map.of());
        CreateNotificationCommand command = buildCreateCommand(
            userId,
            TYPE_SYSTEM,
            TEMPLATE_USER_WELCOME,
            userId,
            template.title(),
            template.content()
        );
        insertNotificationAndCreatePush(command);
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
        String templateCode = ACTION_COMPLETED.equals(normalizedAction)
            ? TEMPLATE_REMINDER_COMPLETED
            : TEMPLATE_REMINDER_SKIPPED;
        String normalizedPetName = petName == null || petName.isBlank() ? "当前宠物" : petName;

        /*
         * 家庭共养场景下，提醒处理结果要同步给所有仍有该宠物访问权且开启通知的成员。
         * 接收人范围在 Mapper 中复用宠物访问约束，避免通知泄露给未共享该宠物的家庭成员。
         */
        for (Long recipientUserId : notificationPersistenceMapper.listNotificationRecipientUserIdsByPetId(petId)) {
            RenderedNotificationTemplate template = renderInboxTemplate(
                templateCode,
                Map.of(
                    "pet_name", normalizedPetName,
                    "reminder_title", reminder.getTitle(),
                    "action_text", actionText,
                    "family_suffix", actorUserId.equals(recipientUserId) ? "" : " 家庭成员已经同步更新。"
                )
            );
            CreateNotificationCommand command = buildCreateCommand(
                recipientUserId,
                TYPE_REMINDER,
                templateCode,
                reminder.getReminderId(),
                template.title(),
                template.content()
            );
            insertNotificationAndCreatePush(command);
        }
    }

    @Transactional
    public void createModerationResultNotification(Long reporterUserId, Long reportId, String action) {
        boolean confirmedViolation = "confirm_violation".equals(action);
        RenderedNotificationTemplate template = renderInboxTemplate(
            confirmedViolation ? TEMPLATE_MODERATION_CONFIRMED : TEMPLATE_MODERATION_DISMISSED,
            Map.of()
        );
        CreateNotificationCommand command = buildCreateCommand(
            reporterUserId,
            TYPE_SYSTEM,
            "moderation_report",
            reportId,
            template.title(),
            template.content()
        );
        insertNotificationAndCreatePush(command);
    }

    @Transactional
    public void createAppointmentCreatedNotification(
        Long actorUserId,
        ServiceAppointmentEntity appointment
    ) {
        String petName = appointment.getPetName() == null || appointment.getPetName().isBlank()
            ? "当前宠物"
            : appointment.getPetName();
        for (Long recipientUserId : notificationPersistenceMapper.listNotificationRecipientUserIdsByPetId(appointment.getPetId())) {
            RenderedNotificationTemplate template = renderInboxTemplate(
                TEMPLATE_APPOINTMENT_CREATED,
                Map.of(
                    "pet_name", petName,
                    "provider_name", appointment.getProviderName(),
                    "appointment_date", String.valueOf(appointment.getAppointmentDate()),
                    "appointment_slot", appointment.getAppointmentSlot(),
                    "family_suffix", actorUserId.equals(recipientUserId) ? "" : " 家庭成员已经同步收到这条预约。"
                )
            );
            CreateNotificationCommand command = buildCreateCommand(
                recipientUserId,
                TYPE_APPOINTMENT,
                "appointment_created",
                appointment.getAppointmentId(),
                template.title(),
                template.content()
            );
            insertNotificationAndCreatePush(command);
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

    private void insertNotificationAndCreatePush(CreateNotificationCommand command) {
        int insertedRows = notificationPersistenceMapper.insertNotificationIfAbsent(command);
        if (insertedRows > 0) {
            pushNotificationApplicationService.createPushTaskForNotification(
                notificationPersistenceMapper.findNotificationByBusinessKey(
                    command.getUserId(),
                    command.getNotifyType(),
                    command.getBizType(),
                    command.getBizId()
                )
            );
            return;
        }
        /*
         * 站内通知开关是用户级总开关。关闭时不写 notifications，同时沉淀 skipped Push 任务，
         * 便于后台排查“业务已触发但用户关闭通知”的真实原因。
         */
        if (!notificationPersistenceMapper.isNotificationSwitchEnabled(command.getUserId())) {
            pushNotificationApplicationService.recordNotificationSwitchSkipped(command);
        }
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

    private RenderedNotificationTemplate renderInboxTemplate(String templateCode, Map<String, String> variables) {
        MessageTemplateDataObject configuredTemplate =
            messageTemplatePersistenceMapper.findActiveTemplateByCodeAndChannel(templateCode, CHANNEL_INBOX);
        String titleTemplate;
        String contentTemplate;
        if (configuredTemplate == null) {
            DefaultNotificationTemplate defaultTemplate = DEFAULT_INBOX_TEMPLATES.get(templateCode);
            if (defaultTemplate == null) {
                throw new BusinessException(ResponseCode.MESSAGE_TEMPLATE_NOT_FOUND);
            }
            /*
             * 内置通知属于现有用户主链路，后台未配置模板时不能阻断登录、提醒处理或预约提交。
             * 这里仅对白名单内置模板使用默认文案；任意未知模板缺失仍会抛出业务异常。
             */
            titleTemplate = defaultTemplate.title();
            contentTemplate = defaultTemplate.content();
        } else {
            titleTemplate = configuredTemplate.titleTemplate();
            contentTemplate = configuredTemplate.contentTemplate();
        }

        if (titleTemplate == null || titleTemplate.isBlank()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "站内消息模板标题不能为空");
        }
        String title = renderTemplateText(titleTemplate, variables, MAX_TITLE_LENGTH, "站内消息标题不能超过 100 个字符");
        String content = renderTemplateText(contentTemplate, variables, MAX_CONTENT_LENGTH, "站内消息内容不能超过 500 个字符");
        return new RenderedNotificationTemplate(title, content);
    }

    private String renderTemplateText(
        String templateText,
        Map<String, String> variables,
        int maxLength,
        String maxLengthMessage
    ) {
        if (templateText == null || templateText.isBlank()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "消息模板内容不能为空");
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateText);
        StringBuffer renderedText = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!variables.containsKey(variableName)) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "消息模板占位符不支持：" + variableName);
            }
            matcher.appendReplacement(renderedText, Matcher.quoteReplacement(variables.get(variableName)));
        }
        matcher.appendTail(renderedText);
        String normalizedText = renderedText.toString().trim();
        if (normalizedText.isEmpty()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "消息模板渲染结果不能为空");
        }
        if (normalizedText.length() > maxLength) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, maxLengthMessage);
        }
        return normalizedText;
    }

    private String normalizeReminderAction(String action) {
        if (ACTION_COMPLETED.equals(action) || ACTION_SKIPPED.equals(action)) {
            return action;
        }
        throw new BusinessException(ResponseCode.BAD_REQUEST, "提醒通知动作不支持");
    }

    private record DefaultNotificationTemplate(String title, String content) {
    }

    private record RenderedNotificationTemplate(String title, String content) {
    }
}
