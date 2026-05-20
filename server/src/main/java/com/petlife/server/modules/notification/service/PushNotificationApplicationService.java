package com.petlife.server.modules.notification.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.notification.converter.NotificationConverter;
import com.petlife.server.modules.notification.converter.PushNotificationConverter;
import com.petlife.server.modules.notification.domain.entity.NotificationEntity;
import com.petlife.server.modules.notification.domain.entity.PushDeviceTokenEntity;
import com.petlife.server.modules.notification.dto.request.RegisterPushDeviceTokenRequest;
import com.petlife.server.modules.notification.dto.response.PushDeliveryRecordResponse;
import com.petlife.server.modules.notification.dto.response.PushDeviceTokenResponse;
import com.petlife.server.modules.notification.dto.response.PushTaskResponse;
import com.petlife.server.modules.notification.persistence.PushNotificationPersistenceMapper;
import com.petlife.server.modules.notification.persistence.command.CreateNotificationCommand;
import com.petlife.server.modules.notification.persistence.command.CreatePushDeliveryRecordCommand;
import com.petlife.server.modules.notification.persistence.command.CreatePushTaskCommand;
import com.petlife.server.modules.notification.persistence.command.UpsertPushDeviceTokenCommand;
import com.petlife.server.modules.notification.persistence.dataobject.NotificationDataObject;
import com.petlife.server.modules.notification.persistence.dataobject.PushDeviceTokenDataObject;
import com.petlife.server.modules.notification.service.provider.DevelopmentNoopPushProvider;
import com.petlife.server.modules.notification.service.provider.PushProvider;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Push 通知应用服务。
 */
@Service
public class PushNotificationApplicationService {

    private static final String TASK_STATUS_PENDING = "pending";
    private static final String TASK_STATUS_SKIPPED = "skipped";
    private static final String DELIVERY_STATUS_PENDING = "pending";
    private static final String REASON_NO_ACTIVE_TOKEN = "no_active_device_token";
    private static final String REASON_NOTIFICATION_SWITCH_OFF = "notification_switch_off";
    private static final int MAX_PROVIDER_CODE_LENGTH = 64;
    private static final int MAX_DEVICE_ID_LENGTH = 128;
    private static final int MAX_APP_VERSION_LENGTH = 40;
    private static final Pattern BUSINESS_CODE_PATTERN = Pattern.compile("^[a-z0-9_\\-]+$");
    private static final Set<String> SUPPORTED_PLATFORMS = Set.of("ios", "android");
    private static final Set<String> SUPPORTED_TASK_STATUSES = Set.of("pending", "skipped", "failed", "sent");
    private static final Set<String> SUPPORTED_DELIVERY_STATUSES = Set.of("pending", "skipped", "failed", "sent");

    private final PushNotificationPersistenceMapper pushNotificationPersistenceMapper;
    private final PushNotificationConverter pushNotificationConverter;
    private final NotificationConverter notificationConverter;
    private final Map<String, PushProvider> providers;

    public PushNotificationApplicationService(
        PushNotificationPersistenceMapper pushNotificationPersistenceMapper,
        PushNotificationConverter pushNotificationConverter,
        NotificationConverter notificationConverter,
        List<PushProvider> providers
    ) {
        this.pushNotificationPersistenceMapper = pushNotificationPersistenceMapper;
        this.pushNotificationConverter = pushNotificationConverter;
        this.notificationConverter = notificationConverter;
        this.providers = providers.stream()
            .collect(Collectors.toUnmodifiableMap(PushProvider::providerCode, Function.identity()));
    }

    @Transactional
    public PushDeviceTokenResponse registerDeviceToken(RegisterPushDeviceTokenRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        UpsertPushDeviceTokenCommand command = new UpsertPushDeviceTokenCommand();
        command.setUserId(currentUserId);
        command.setPlatform(normalizePlatform(request.platform()));
        command.setProviderCode(normalizeProviderCodeOrDefault(request.providerCode()));
        command.setDeviceToken(normalizeDeviceToken(request.deviceToken()));
        command.setDeviceId(normalizeOptionalText(request.deviceId(), MAX_DEVICE_ID_LENGTH, "设备标识不能超过 128 字符"));
        command.setAppVersion(normalizeOptionalText(request.appVersion(), MAX_APP_VERSION_LENGTH, "App 版本不能超过 40 字符"));
        pushNotificationPersistenceMapper.upsertDeviceToken(command);
        return pushNotificationConverter.toResponse(requireDeviceToken(command.getDeviceTokenId()));
    }

    @Transactional
    public PushDeviceTokenResponse unregisterDeviceToken(Long deviceTokenId) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireUserDeviceToken(currentUserId, deviceTokenId);
        pushNotificationPersistenceMapper.disableUserDeviceToken(currentUserId, deviceTokenId);
        return pushNotificationConverter.toResponse(requireDeviceToken(deviceTokenId));
    }

    public List<PushTaskResponse> listAdminPushTasks(
        Long userId,
        Long notificationId,
        String taskStatus,
        String providerCode
    ) {
        return pushNotificationPersistenceMapper
            .listPushTasks(
                userId,
                notificationId,
                normalizeOptionalTaskStatus(taskStatus),
                normalizeOptionalProviderCode(providerCode)
            )
            .stream()
            .map(pushNotificationConverter::toEntity)
            .map(pushNotificationConverter::toResponse)
            .toList();
    }

    public List<PushDeliveryRecordResponse> listAdminDeliveryRecords(
        Long pushTaskId,
        Long userId,
        String deliveryStatus,
        String providerCode
    ) {
        return pushNotificationPersistenceMapper
            .listDeliveryRecords(
                pushTaskId,
                userId,
                normalizeOptionalDeliveryStatus(deliveryStatus),
                normalizeOptionalProviderCode(providerCode)
            )
            .stream()
            .map(pushNotificationConverter::toEntity)
            .map(pushNotificationConverter::toResponse)
            .toList();
    }

    @Transactional
    public void createPushTaskForNotification(NotificationDataObject notificationDataObject) {
        NotificationEntity notification = notificationConverter.toEntity(notificationDataObject);
        if (notification == null) {
            return;
        }
        createPushTask(
            notification.getUserId(),
            notification.getNotificationId(),
            notification.getNotifyType(),
            notification.getBizType(),
            notification.getBizId(),
            notification.getTitle(),
            notification.getContent(),
            null
        );
    }

    @Transactional
    public void recordNotificationSwitchSkipped(CreateNotificationCommand notificationCommand) {
        if (notificationCommand == null) {
            return;
        }
        int existingRows = pushNotificationPersistenceMapper.countPushTasksByBusinessAndReason(
            notificationCommand.getUserId(),
            notificationCommand.getBizType(),
            notificationCommand.getBizId(),
            REASON_NOTIFICATION_SWITCH_OFF
        );
        if (existingRows > 0) {
            return;
        }
        createPushTask(
            notificationCommand.getUserId(),
            null,
            notificationCommand.getNotifyType(),
            notificationCommand.getBizType(),
            notificationCommand.getBizId(),
            notificationCommand.getTitle(),
            notificationCommand.getContent(),
            REASON_NOTIFICATION_SWITCH_OFF
        );
    }

    private void createPushTask(
        Long userId,
        Long notificationId,
        String notifyType,
        String bizType,
        Long bizId,
        String title,
        String content,
        String forceSkipReason
    ) {
        PushProvider provider = requireProvider(DevelopmentNoopPushProvider.PROVIDER_CODE);
        List<PushDeviceTokenDataObject> activeTokens = pushNotificationPersistenceMapper.listActiveDeviceTokensByUserId(userId);
        String failureReason = forceSkipReason;
        String taskStatus = TASK_STATUS_PENDING;
        if (failureReason != null) {
            taskStatus = TASK_STATUS_SKIPPED;
        } else if (activeTokens.isEmpty()) {
            taskStatus = TASK_STATUS_SKIPPED;
            failureReason = REASON_NO_ACTIVE_TOKEN;
        }

        CreatePushTaskCommand taskCommand = new CreatePushTaskCommand();
        taskCommand.setUserId(userId);
        taskCommand.setNotificationId(notificationId);
        taskCommand.setNotifyType(notifyType);
        taskCommand.setBizType(bizType);
        taskCommand.setBizId(bizId);
        taskCommand.setTitle(title);
        taskCommand.setContent(content);
        taskCommand.setProviderCode(provider.providerCode());
        taskCommand.setTaskStatus(taskStatus);
        taskCommand.setFailureReason(failureReason);
        pushNotificationPersistenceMapper.insertPushTask(taskCommand);

        /*
         * 未接入真实 Push 通道时，不能把投递记录写成成功。
         * 有可用 token 的任务保持 pending，后续接入供应商后再由调度器推进状态。
         */
        if (TASK_STATUS_PENDING.equals(taskStatus) && !provider.dispatchEnabled()) {
            for (PushDeviceTokenDataObject token : activeTokens) {
                CreatePushDeliveryRecordCommand deliveryCommand = new CreatePushDeliveryRecordCommand();
                deliveryCommand.setPushTaskId(taskCommand.getPushTaskId());
                deliveryCommand.setDeviceTokenId(token.deviceTokenId());
                deliveryCommand.setUserId(userId);
                deliveryCommand.setProviderCode(provider.providerCode());
                deliveryCommand.setDeliveryStatus(DELIVERY_STATUS_PENDING);
                deliveryCommand.setFailureReason(null);
                pushNotificationPersistenceMapper.insertDeliveryRecord(deliveryCommand);
            }
        }
    }

    private PushProvider requireProvider(String providerCode) {
        PushProvider provider = providers.get(providerCode);
        if (provider == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "Push 供应商未配置");
        }
        return provider;
    }

    private PushDeviceTokenEntity requireDeviceToken(Long deviceTokenId) {
        PushDeviceTokenEntity deviceToken = pushNotificationConverter.toEntity(
            pushNotificationPersistenceMapper.findDeviceTokenById(deviceTokenId)
        );
        if (deviceToken == null) {
            throw new BusinessException(ResponseCode.PUSH_DEVICE_TOKEN_NOT_FOUND);
        }
        return deviceToken;
    }

    private PushDeviceTokenEntity requireUserDeviceToken(Long userId, Long deviceTokenId) {
        PushDeviceTokenEntity deviceToken = pushNotificationConverter.toEntity(
            pushNotificationPersistenceMapper.findUserDeviceTokenById(userId, deviceTokenId)
        );
        if (deviceToken == null) {
            throw new BusinessException(ResponseCode.PUSH_DEVICE_TOKEN_NOT_FOUND);
        }
        return deviceToken;
    }

    private String normalizePlatform(String platform) {
        String normalizedPlatform = normalizeRequiredText(platform, "客户端平台不能为空", 20, "客户端平台不能超过 20 字符")
            .toLowerCase();
        if (!SUPPORTED_PLATFORMS.contains(normalizedPlatform)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "客户端平台仅支持 ios、android");
        }
        return normalizedPlatform;
    }

    private String normalizeProviderCodeOrDefault(String providerCode) {
        String normalizedProviderCode = normalizeOptionalProviderCode(providerCode);
        if (normalizedProviderCode == null) {
            return DevelopmentNoopPushProvider.PROVIDER_CODE;
        }
        if (!providers.containsKey(normalizedProviderCode)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "Push 供应商未配置");
        }
        return normalizedProviderCode;
    }

    private String normalizeOptionalProviderCode(String providerCode) {
        String normalizedProviderCode = normalizeOptionalText(providerCode, MAX_PROVIDER_CODE_LENGTH, "Push 供应商编码不能超过 64 字符");
        if (normalizedProviderCode == null) {
            return null;
        }
        normalizedProviderCode = normalizedProviderCode.toLowerCase();
        if (!BUSINESS_CODE_PATTERN.matcher(normalizedProviderCode).matches()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "Push 供应商编码格式不支持");
        }
        return normalizedProviderCode;
    }

    private String normalizeDeviceToken(String deviceToken) {
        return normalizeRequiredText(deviceToken, "设备 Token 不能为空", 512, "设备 Token 不能超过 512 字符");
    }

    private String normalizeOptionalTaskStatus(String taskStatus) {
        String normalizedTaskStatus = normalizeOptionalText(taskStatus, 20, "Push 任务状态不能超过 20 字符");
        if (normalizedTaskStatus == null) {
            return null;
        }
        normalizedTaskStatus = normalizedTaskStatus.toLowerCase();
        if (!SUPPORTED_TASK_STATUSES.contains(normalizedTaskStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "Push 任务状态不支持");
        }
        return normalizedTaskStatus;
    }

    private String normalizeOptionalDeliveryStatus(String deliveryStatus) {
        String normalizedDeliveryStatus = normalizeOptionalText(deliveryStatus, 20, "Push 投递状态不能超过 20 字符");
        if (normalizedDeliveryStatus == null) {
            return null;
        }
        normalizedDeliveryStatus = normalizedDeliveryStatus.toLowerCase();
        if (!SUPPORTED_DELIVERY_STATUSES.contains(normalizedDeliveryStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "Push 投递状态不支持");
        }
        return normalizedDeliveryStatus;
    }

    private String normalizeRequiredText(String text, String emptyMessage, int maxLength, String maxLengthMessage) {
        String normalizedText = normalizeOptionalText(text, maxLength, maxLengthMessage);
        if (normalizedText == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, emptyMessage);
        }
        return normalizedText;
    }

    private String normalizeOptionalText(String text, int maxLength, String maxLengthMessage) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String normalizedText = text.trim();
        if (normalizedText.length() > maxLength) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, maxLengthMessage);
        }
        return normalizedText;
    }
}
