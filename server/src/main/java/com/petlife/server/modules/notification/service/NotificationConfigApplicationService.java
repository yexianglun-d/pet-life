package com.petlife.server.modules.notification.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.service.AuditLogApplicationService;
import com.petlife.server.modules.notification.converter.MessageTemplateConverter;
import com.petlife.server.modules.notification.converter.NotificationChannelConfigConverter;
import com.petlife.server.modules.notification.domain.entity.MessageTemplateEntity;
import com.petlife.server.modules.notification.domain.entity.NotificationChannelConfigEntity;
import com.petlife.server.modules.notification.dto.request.AdminUpdateMessageTemplateStatusRequest;
import com.petlife.server.modules.notification.dto.request.AdminUpdateNotificationChannelStatusRequest;
import com.petlife.server.modules.notification.dto.request.AdminUpsertMessageTemplateRequest;
import com.petlife.server.modules.notification.dto.request.AdminUpsertNotificationChannelRequest;
import com.petlife.server.modules.notification.dto.response.MessageTemplateResponse;
import com.petlife.server.modules.notification.dto.response.NotificationChannelConfigResponse;
import com.petlife.server.modules.notification.persistence.MessageTemplatePersistenceMapper;
import com.petlife.server.modules.notification.persistence.NotificationChannelConfigPersistenceMapper;
import com.petlife.server.modules.notification.persistence.command.UpdateMessageTemplateStatusCommand;
import com.petlife.server.modules.notification.persistence.command.UpdateNotificationChannelConfigStatusCommand;
import com.petlife.server.modules.notification.persistence.command.UpsertMessageTemplateCommand;
import com.petlife.server.modules.notification.persistence.command.UpsertNotificationChannelConfigCommand;
import com.petlife.server.modules.notification.persistence.dataobject.MessageTemplateDataObject;
import com.petlife.server.modules.notification.persistence.dataobject.NotificationChannelConfigDataObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 后台通知与消息配置应用服务。
 */
@Service
public class NotificationConfigApplicationService {

    private static final String CHANNEL_INBOX = "inbox";
    private static final String CHANNEL_SMS = "sms";
    private static final String CHANNEL_PUSH = "push";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_INACTIVE = "inactive";
    private static final String CONFIG_STATUS_DRAFT = "draft";
    private static final String CONFIG_STATUS_READY = "ready";
    private static final String CONFIG_STATUS_DISABLED = "disabled";
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_TEMPLATE_CODE_LENGTH = 64;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_CONTENT_LENGTH = 500;
    private static final int MAX_PROVIDER_CODE_LENGTH = 64;
    private static final int MAX_PROVIDER_NAME_LENGTH = 100;
    private static final int MAX_REMARK_LENGTH = 500;
    private static final Pattern BUSINESS_CODE_PATTERN = Pattern.compile("^[a-z0-9_\\-]+$");
    private static final Set<String> SUPPORTED_CHANNEL_TYPES = Set.of(CHANNEL_INBOX, CHANNEL_SMS, CHANNEL_PUSH);
    private static final Set<String> SUPPORTED_CONFIG_STATUSES = Set.of(
        CONFIG_STATUS_DRAFT,
        CONFIG_STATUS_READY,
        CONFIG_STATUS_DISABLED
    );
    private static final String AUDIT_TARGET_MESSAGE_TEMPLATE = "message_template";
    private static final String AUDIT_TARGET_NOTIFICATION_CHANNEL = "notification_channel";
    private static final String AUDIT_ACTION_MESSAGE_TEMPLATE_CREATE = "message_template_create";
    private static final String AUDIT_ACTION_MESSAGE_TEMPLATE_UPDATE = "message_template_update";
    private static final String AUDIT_ACTION_MESSAGE_TEMPLATE_STATUS_UPDATE = "message_template_status_update";
    private static final String AUDIT_ACTION_NOTIFICATION_CHANNEL_CREATE = "notification_channel_create";
    private static final String AUDIT_ACTION_NOTIFICATION_CHANNEL_UPDATE = "notification_channel_update";
    private static final String AUDIT_ACTION_NOTIFICATION_CHANNEL_STATUS_UPDATE = "notification_channel_status_update";

    private final MessageTemplatePersistenceMapper messageTemplatePersistenceMapper;
    private final MessageTemplateConverter messageTemplateConverter;
    private final NotificationChannelConfigPersistenceMapper notificationChannelConfigPersistenceMapper;
    private final NotificationChannelConfigConverter notificationChannelConfigConverter;
    private final AuditLogApplicationService auditLogApplicationService;

    public NotificationConfigApplicationService(
        MessageTemplatePersistenceMapper messageTemplatePersistenceMapper,
        MessageTemplateConverter messageTemplateConverter,
        NotificationChannelConfigPersistenceMapper notificationChannelConfigPersistenceMapper,
        NotificationChannelConfigConverter notificationChannelConfigConverter,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.messageTemplatePersistenceMapper = messageTemplatePersistenceMapper;
        this.messageTemplateConverter = messageTemplateConverter;
        this.notificationChannelConfigPersistenceMapper = notificationChannelConfigPersistenceMapper;
        this.notificationChannelConfigConverter = notificationChannelConfigConverter;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    public List<MessageTemplateResponse> listMessageTemplates(
        String keyword,
        String templateCode,
        String channelType,
        Boolean enabled
    ) {
        String normalizedKeyword = normalizeOptionalText(keyword, MAX_KEYWORD_LENGTH, "搜索关键词不能超过 100 个字符");
        String normalizedTemplateCode = normalizeOptionalBusinessCode(templateCode, "模板编码格式不支持", MAX_TEMPLATE_CODE_LENGTH);
        String normalizedChannelType = normalizeOptionalChannelType(channelType);
        String status = enabled == null ? null : toTemplateStatus(enabled);
        return messageTemplatePersistenceMapper
            .listTemplates(normalizedKeyword, normalizedTemplateCode, normalizedChannelType, status)
            .stream()
            .map(messageTemplateConverter::toEntity)
            .map(messageTemplateConverter::toResponse)
            .toList();
    }

    public MessageTemplateResponse getMessageTemplate(Long templateId) {
        return messageTemplateConverter.toResponse(requireMessageTemplate(templateId));
    }

    @Transactional
    public MessageTemplateResponse createMessageTemplate(
        AdminUpsertMessageTemplateRequest request,
        AdminOperationContext operationContext
    ) {
        UpsertMessageTemplateCommand command = buildMessageTemplateCommand(null, request);
        assertMessageTemplateUnique(null, command.getTemplateCode(), command.getChannelType());
        messageTemplatePersistenceMapper.insertTemplate(command);
        MessageTemplateEntity template = requireMessageTemplate(command.getTemplateId());
        auditMessageTemplate(operationContext, template, AUDIT_ACTION_MESSAGE_TEMPLATE_CREATE);
        return messageTemplateConverter.toResponse(template);
    }

    @Transactional
    public MessageTemplateResponse updateMessageTemplate(
        Long templateId,
        AdminUpsertMessageTemplateRequest request,
        AdminOperationContext operationContext
    ) {
        requireMessageTemplate(templateId);
        UpsertMessageTemplateCommand command = buildMessageTemplateCommand(templateId, request);
        assertMessageTemplateUnique(templateId, command.getTemplateCode(), command.getChannelType());
        int updatedRows = messageTemplatePersistenceMapper.updateTemplate(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.MESSAGE_TEMPLATE_NOT_FOUND);
        }
        MessageTemplateEntity template = requireMessageTemplate(templateId);
        auditMessageTemplate(operationContext, template, AUDIT_ACTION_MESSAGE_TEMPLATE_UPDATE);
        return messageTemplateConverter.toResponse(template);
    }

    @Transactional
    public MessageTemplateResponse updateMessageTemplateStatus(
        Long templateId,
        AdminUpdateMessageTemplateStatusRequest request,
        AdminOperationContext operationContext
    ) {
        requireMessageTemplate(templateId);
        UpdateMessageTemplateStatusCommand command = new UpdateMessageTemplateStatusCommand();
        command.setTemplateId(templateId);
        command.setStatus(toTemplateStatus(Boolean.TRUE.equals(request.enabled())));
        int updatedRows = messageTemplatePersistenceMapper.updateTemplateStatus(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.MESSAGE_TEMPLATE_NOT_FOUND);
        }
        MessageTemplateEntity template = requireMessageTemplate(templateId);
        auditMessageTemplate(operationContext, template, AUDIT_ACTION_MESSAGE_TEMPLATE_STATUS_UPDATE);
        return messageTemplateConverter.toResponse(template);
    }

    public List<NotificationChannelConfigResponse> listNotificationChannels(
        String channelType,
        String providerCode,
        Boolean enabled,
        String configStatus
    ) {
        String normalizedChannelType = normalizeOptionalChannelType(channelType);
        String normalizedProviderCode = normalizeOptionalBusinessCode(providerCode, "供应商编码格式不支持", MAX_PROVIDER_CODE_LENGTH);
        String normalizedConfigStatus = normalizeOptionalConfigStatus(configStatus);
        return notificationChannelConfigPersistenceMapper
            .listChannelConfigs(normalizedChannelType, normalizedProviderCode, enabled, normalizedConfigStatus)
            .stream()
            .map(notificationChannelConfigConverter::toEntity)
            .map(notificationChannelConfigConverter::toResponse)
            .toList();
    }

    public NotificationChannelConfigResponse getNotificationChannel(Long channelConfigId) {
        return notificationChannelConfigConverter.toResponse(requireNotificationChannel(channelConfigId));
    }

    @Transactional
    public NotificationChannelConfigResponse createNotificationChannel(
        AdminUpsertNotificationChannelRequest request,
        AdminOperationContext operationContext
    ) {
        UpsertNotificationChannelConfigCommand command = buildNotificationChannelCommand(null, request);
        assertNotificationChannelUnique(null, command.getChannelType(), command.getProviderCode());
        notificationChannelConfigPersistenceMapper.insertChannelConfig(command);
        NotificationChannelConfigEntity channel = requireNotificationChannel(command.getChannelConfigId());
        auditNotificationChannel(operationContext, channel, AUDIT_ACTION_NOTIFICATION_CHANNEL_CREATE);
        return notificationChannelConfigConverter.toResponse(channel);
    }

    @Transactional
    public NotificationChannelConfigResponse updateNotificationChannel(
        Long channelConfigId,
        AdminUpsertNotificationChannelRequest request,
        AdminOperationContext operationContext
    ) {
        requireNotificationChannel(channelConfigId);
        UpsertNotificationChannelConfigCommand command = buildNotificationChannelCommand(channelConfigId, request);
        assertNotificationChannelUnique(channelConfigId, command.getChannelType(), command.getProviderCode());
        int updatedRows = notificationChannelConfigPersistenceMapper.updateChannelConfig(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.NOTIFICATION_CHANNEL_NOT_FOUND);
        }
        NotificationChannelConfigEntity channel = requireNotificationChannel(channelConfigId);
        auditNotificationChannel(operationContext, channel, AUDIT_ACTION_NOTIFICATION_CHANNEL_UPDATE);
        return notificationChannelConfigConverter.toResponse(channel);
    }

    @Transactional
    public NotificationChannelConfigResponse updateNotificationChannelStatus(
        Long channelConfigId,
        AdminUpdateNotificationChannelStatusRequest request,
        AdminOperationContext operationContext
    ) {
        requireNotificationChannel(channelConfigId);
        UpdateNotificationChannelConfigStatusCommand command = new UpdateNotificationChannelConfigStatusCommand();
        command.setChannelConfigId(channelConfigId);
        command.setEnabled(Boolean.TRUE.equals(request.enabled()));
        command.setConfigStatus(Boolean.TRUE.equals(request.enabled()) ? CONFIG_STATUS_READY : CONFIG_STATUS_DISABLED);
        int updatedRows = notificationChannelConfigPersistenceMapper.updateChannelConfigStatus(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.NOTIFICATION_CHANNEL_NOT_FOUND);
        }
        NotificationChannelConfigEntity channel = requireNotificationChannel(channelConfigId);
        auditNotificationChannel(operationContext, channel, AUDIT_ACTION_NOTIFICATION_CHANNEL_STATUS_UPDATE);
        return notificationChannelConfigConverter.toResponse(channel);
    }

    private MessageTemplateEntity requireMessageTemplate(Long templateId) {
        MessageTemplateEntity template = messageTemplateConverter.toEntity(
            messageTemplatePersistenceMapper.findTemplateById(templateId)
        );
        if (template == null) {
            throw new BusinessException(ResponseCode.MESSAGE_TEMPLATE_NOT_FOUND);
        }
        return template;
    }

    private NotificationChannelConfigEntity requireNotificationChannel(Long channelConfigId) {
        NotificationChannelConfigEntity channel = notificationChannelConfigConverter.toEntity(
            notificationChannelConfigPersistenceMapper.findChannelConfigById(channelConfigId)
        );
        if (channel == null) {
            throw new BusinessException(ResponseCode.NOTIFICATION_CHANNEL_NOT_FOUND);
        }
        return channel;
    }

    private UpsertMessageTemplateCommand buildMessageTemplateCommand(
        Long templateId,
        AdminUpsertMessageTemplateRequest request
    ) {
        String channelType = normalizeRequiredChannelType(request.channelType());
        String titleTemplate = normalizeOptionalText(request.titleTemplate(), MAX_TITLE_LENGTH, "标题模板不能超过 100 个字符");
        if ((CHANNEL_INBOX.equals(channelType) || CHANNEL_PUSH.equals(channelType)) && titleTemplate == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "站内和 Push 模板标题不能为空");
        }

        UpsertMessageTemplateCommand command = new UpsertMessageTemplateCommand();
        command.setTemplateId(templateId);
        command.setTemplateCode(normalizeRequiredBusinessCode(
            request.templateCode(),
            "模板编码不能为空",
            "模板编码格式不支持",
            MAX_TEMPLATE_CODE_LENGTH
        ));
        command.setChannelType(channelType);
        command.setTitleTemplate(titleTemplate);
        command.setContentTemplate(normalizeRequiredText(
            request.contentTemplate(),
            "内容模板不能为空",
            "内容模板不能超过 500 个字符",
            MAX_CONTENT_LENGTH
        ));
        command.setStatus(toTemplateStatus(Boolean.TRUE.equals(request.enabled())));
        return command;
    }

    private UpsertNotificationChannelConfigCommand buildNotificationChannelCommand(
        Long channelConfigId,
        AdminUpsertNotificationChannelRequest request
    ) {
        boolean enabled = Boolean.TRUE.equals(request.enabled());
        String configStatus = normalizeRequiredConfigStatus(request.configStatus(), enabled);

        UpsertNotificationChannelConfigCommand command = new UpsertNotificationChannelConfigCommand();
        command.setChannelConfigId(channelConfigId);
        command.setChannelType(normalizeRequiredChannelType(request.channelType()));
        command.setProviderCode(normalizeRequiredBusinessCode(
            request.providerCode(),
            "供应商编码不能为空",
            "供应商编码格式不支持",
            MAX_PROVIDER_CODE_LENGTH
        ));
        command.setProviderName(normalizeRequiredText(
            request.providerName(),
            "供应商名称不能为空",
            "供应商名称不能超过 100 个字符",
            MAX_PROVIDER_NAME_LENGTH
        ));
        command.setEnabled(enabled);
        command.setConfigStatus(configStatus);
        command.setRemark(normalizeOptionalText(request.remark(), MAX_REMARK_LENGTH, "备注不能超过 500 个字符"));
        return command;
    }

    private void assertMessageTemplateUnique(Long currentTemplateId, String templateCode, String channelType) {
        MessageTemplateDataObject existingTemplate =
            messageTemplatePersistenceMapper.findTemplateByCodeAndChannel(templateCode, channelType);
        if (existingTemplate != null && !existingTemplate.templateId().equals(currentTemplateId)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "消息模板编码和渠道已存在");
        }
    }

    private void assertNotificationChannelUnique(Long currentChannelConfigId, String channelType, String providerCode) {
        NotificationChannelConfigDataObject existingChannel =
            notificationChannelConfigPersistenceMapper.findChannelConfigByChannelAndProvider(channelType, providerCode);
        if (existingChannel != null && !existingChannel.channelConfigId().equals(currentChannelConfigId)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "通知渠道和供应商编码已存在");
        }
    }

    private String normalizeRequiredChannelType(String channelType) {
        String normalizedChannelType = normalizeRequiredText(channelType, "渠道类型不能为空", "渠道类型不能超过 20 个字符", 20)
            .toLowerCase();
        if (!SUPPORTED_CHANNEL_TYPES.contains(normalizedChannelType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "渠道类型仅支持 inbox、sms、push");
        }
        return normalizedChannelType;
    }

    private String normalizeOptionalChannelType(String channelType) {
        String normalizedChannelType = normalizeOptionalText(channelType, 20, "渠道类型不能超过 20 个字符");
        if (normalizedChannelType == null) {
            return null;
        }
        normalizedChannelType = normalizedChannelType.toLowerCase();
        if (!SUPPORTED_CHANNEL_TYPES.contains(normalizedChannelType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "渠道类型仅支持 inbox、sms、push");
        }
        return normalizedChannelType;
    }

    private String normalizeRequiredConfigStatus(String configStatus, boolean enabled) {
        String normalizedStatus = normalizeRequiredText(configStatus, "配置状态不能为空", "配置状态不能超过 20 个字符", 20)
            .toLowerCase();
        if (!SUPPORTED_CONFIG_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "配置状态仅支持 draft、ready、disabled");
        }
        // 渠道启停和配置状态共同表达发送可用性，避免出现“启用但未就绪”的歧义配置。
        if (enabled && !CONFIG_STATUS_READY.equals(normalizedStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "启用渠道必须处于 ready 状态");
        }
        if (!enabled && CONFIG_STATUS_READY.equals(normalizedStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "停用渠道不能处于 ready 状态");
        }
        return normalizedStatus;
    }

    private String normalizeOptionalConfigStatus(String configStatus) {
        String normalizedStatus = normalizeOptionalText(configStatus, 20, "配置状态不能超过 20 个字符");
        if (normalizedStatus == null) {
            return null;
        }
        normalizedStatus = normalizedStatus.toLowerCase();
        if (!SUPPORTED_CONFIG_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "配置状态仅支持 draft、ready、disabled");
        }
        return normalizedStatus;
    }

    private String normalizeRequiredBusinessCode(
        String value,
        String emptyMessage,
        String formatMessage,
        int maxLength
    ) {
        String normalizedValue = normalizeRequiredText(value, emptyMessage, formatMessage, maxLength).toLowerCase();
        if (!BUSINESS_CODE_PATTERN.matcher(normalizedValue).matches()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, formatMessage);
        }
        return normalizedValue;
    }

    private String normalizeOptionalBusinessCode(String value, String formatMessage, int maxLength) {
        String normalizedValue = normalizeOptionalText(value, maxLength, formatMessage);
        if (normalizedValue == null) {
            return null;
        }
        normalizedValue = normalizedValue.toLowerCase();
        if (!BUSINESS_CODE_PATTERN.matcher(normalizedValue).matches()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, formatMessage);
        }
        return normalizedValue;
    }

    private String normalizeRequiredText(String value, String emptyMessage, String maxLengthMessage, int maxLength) {
        String normalizedValue = normalizeOptionalText(value, maxLength, maxLengthMessage);
        if (normalizedValue == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, emptyMessage);
        }
        return normalizedValue;
    }

    private String normalizeOptionalText(String value, int maxLength, String maxLengthMessage) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalizedValue = value.trim();
        if (normalizedValue.length() > maxLength) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, maxLengthMessage);
        }
        return normalizedValue;
    }

    private String toTemplateStatus(boolean enabled) {
        return enabled ? STATUS_ACTIVE : STATUS_INACTIVE;
    }

    private void auditMessageTemplate(
        AdminOperationContext operationContext,
        MessageTemplateEntity template,
        String action
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("template_code", template.getTemplateCode());
        detail.put("channel_type", template.getChannelType());
        detail.put("enabled", template.isEnabled());
        auditLogApplicationService.recordAdminOperation(
            operationContext,
            AUDIT_TARGET_MESSAGE_TEMPLATE,
            template.getTemplateId().toString(),
            action,
            detail
        );
    }

    private void auditNotificationChannel(
        AdminOperationContext operationContext,
        NotificationChannelConfigEntity channel,
        String action
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("channel_type", channel.getChannelType());
        detail.put("provider_code", channel.getProviderCode());
        detail.put("provider_name", channel.getProviderName());
        detail.put("enabled", channel.isEnabled());
        detail.put("config_status", channel.getConfigStatus());
        auditLogApplicationService.recordAdminOperation(
            operationContext,
            AUDIT_TARGET_NOTIFICATION_CHANNEL,
            channel.getChannelConfigId().toString(),
            action,
            detail
        );
    }
}
