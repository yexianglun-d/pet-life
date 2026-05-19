package com.petlife.server.modules.auth.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.auth.converter.SmsVerificationConverter;
import com.petlife.server.modules.auth.dto.request.AuthSmsSendRequest;
import com.petlife.server.modules.auth.dto.response.AuthSmsSendResponse;
import com.petlife.server.modules.auth.dto.response.SmsSendRecordResponse;
import com.petlife.server.modules.auth.dto.response.SmsVerificationRecordResponse;
import com.petlife.server.modules.auth.persistence.SmsVerificationPersistenceMapper;
import com.petlife.server.modules.auth.persistence.command.CreateSmsSendRecordCommand;
import com.petlife.server.modules.auth.persistence.command.CreateSmsVerificationCodeCommand;
import com.petlife.server.modules.auth.persistence.command.IncrementSmsVerificationAttemptCommand;
import com.petlife.server.modules.auth.persistence.command.UpdateSmsVerificationStatusCommand;
import com.petlife.server.modules.auth.persistence.dataobject.SmsSendRecordDataObject;
import com.petlife.server.modules.auth.persistence.dataobject.SmsVerificationCodeDataObject;
import com.petlife.server.modules.auth.service.sms.SmsProvider;
import com.petlife.server.modules.auth.service.sms.SmsSendResult;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 短信验证码应用服务。
 */
@Service
public class SmsVerificationApplicationService {

    private static final String DEFAULT_SCENE = "login";
    private static final Set<String> SUPPORTED_SCENES = Set.of(DEFAULT_SCENE);
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_VERIFIED = "verified";
    private static final String STATUS_EXPIRED = "expired";
    private static final String STATUS_LOCKED = "locked";
    private static final String STATUS_SEND_FAILED = "send_failed";
    private static final String SEND_STATUS_ACCEPTED = "accepted";
    private static final String SEND_STATUS_FAILED = "failed";
    private static final String SEND_STATUS_BLOCKED = "blocked";
    private static final int CODE_EXPIRE_SECONDS = 300;
    private static final int RESEND_INTERVAL_SECONDS = 60;
    private static final int MAX_ATTEMPT_COUNT = 5;
    private static final int MAX_MOBILE_SCENE_SENDS_PER_HOUR = 5;
    private static final int MAX_IP_SCENE_SENDS_PER_HOUR = 20;
    private static final int CODE_BOUND = 1_000_000;
    private static final int SALT_BYTES = 16;
    private static final String UNKNOWN_IP = "unknown";

    private final SmsVerificationPersistenceMapper smsVerificationPersistenceMapper;
    private final SmsVerificationConverter smsVerificationConverter;
    private final SmsProvider smsProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    public SmsVerificationApplicationService(
        SmsVerificationPersistenceMapper smsVerificationPersistenceMapper,
        SmsVerificationConverter smsVerificationConverter,
        SmsProvider smsProvider
    ) {
        this.smsVerificationPersistenceMapper = smsVerificationPersistenceMapper;
        this.smsVerificationConverter = smsVerificationConverter;
        this.smsProvider = smsProvider;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthSmsSendResponse sendSmsCode(AuthSmsSendRequest request, HttpServletRequest httpServletRequest) {
        String mobile = normalizeRequired(request.mobile());
        String scene = normalizeScene(request.scene());
        LocalDateTime now = LocalDateTime.now();
        String requestIp = resolveClientIp(httpServletRequest);
        String userAgent = normalizeUserAgent(httpServletRequest.getHeader("User-Agent"));

        ensureSendAllowed(mobile, scene, requestIp, now, userAgent);
        smsVerificationPersistenceMapper.expireActiveCodesByMobileAndScene(mobile, scene);

        String code = generateCode();
        String salt = generateSalt();
        CreateSmsVerificationCodeCommand verificationCommand = new CreateSmsVerificationCodeCommand();
        verificationCommand.setMobile(mobile);
        verificationCommand.setScene(scene);
        verificationCommand.setCodeHash(hashCode(salt, mobile, scene, code));
        verificationCommand.setSalt(salt);
        verificationCommand.setExpiresAt(now.plusSeconds(CODE_EXPIRE_SECONDS));
        verificationCommand.setMaxAttemptCount(MAX_ATTEMPT_COUNT);
        verificationCommand.setStatus(STATUS_ACTIVE);
        verificationCommand.setRequestIp(requestIp);
        verificationCommand.setUserAgent(userAgent);
        smsVerificationPersistenceMapper.insertVerificationCode(verificationCommand);

        SmsSendResult sendResult = sendViaProvider(mobile, scene, code);
        createSendRecord(
            verificationCommand.getVerificationId(),
            mobile,
            scene,
            smsProvider.providerCode(),
            sendResult.sendStatus(),
            normalizeFailureReason(sendResult.failureReason()),
            requestIp,
            userAgent
        );

        if (!SEND_STATUS_ACCEPTED.equals(sendResult.sendStatus())) {
            updateStatus(verificationCommand.getVerificationId(), STATUS_SEND_FAILED);
            throw new BusinessException(ResponseCode.AUTH_SMS_SEND_FAILED, "短信验证码发送失败");
        }

        return new AuthSmsSendResponse(
            mobile,
            scene,
            true,
            CODE_EXPIRE_SECONDS,
            RESEND_INTERVAL_SECONDS,
            smsProvider.providerCode()
        );
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void verifyLoginCode(String mobile, String code) {
        String normalizedMobile = normalizeRequired(mobile);
        String normalizedCode = normalizeRequired(code);
        String scene = DEFAULT_SCENE;
        LocalDateTime now = LocalDateTime.now();

        SmsVerificationCodeDataObject verificationCode =
            smsVerificationPersistenceMapper.lockLatestActiveCode(normalizedMobile, scene);
        if (verificationCode == null) {
            throwLatestCodeFailure(normalizedMobile, scene);
        }

        if (!verificationCode.expiresAt().isAfter(now)) {
            updateStatus(verificationCode.verificationId(), STATUS_EXPIRED);
            throw new BusinessException(ResponseCode.AUTH_SMS_CODE_EXPIRED, "验证码已过期，请重新获取");
        }
        if (verificationCode.attemptCount() >= verificationCode.maxAttemptCount()) {
            updateStatus(verificationCode.verificationId(), STATUS_LOCKED);
            throw new BusinessException(ResponseCode.AUTH_SMS_CODE_ATTEMPT_LIMITED, "验证码错误次数过多，请重新获取");
        }

        String inputCodeHash = hashCode(
            verificationCode.salt(),
            normalizedMobile,
            scene,
            normalizedCode
        );
        if (!MessageDigest.isEqual(
            verificationCode.codeHash().getBytes(StandardCharsets.UTF_8),
            inputCodeHash.getBytes(StandardCharsets.UTF_8)
        )) {
            handleWrongCode(verificationCode);
        }

        smsVerificationPersistenceMapper.markVerificationCodeVerified(verificationCode.verificationId());
    }

    public List<SmsVerificationRecordResponse> listVerificationRecords(
        String mobile,
        String scene,
        String status
    ) {
        return smsVerificationPersistenceMapper
            .listVerificationCodes(normalizeOptional(mobile), normalizeOptional(scene), normalizeOptional(status))
            .stream()
            .map(smsVerificationConverter::toEntity)
            .map(smsVerificationConverter::toResponse)
            .toList();
    }

    public List<SmsSendRecordResponse> listSendRecords(
        String mobile,
        String scene,
        String providerCode,
        String sendStatus
    ) {
        return smsVerificationPersistenceMapper
            .listSendRecords(
                normalizeOptional(mobile),
                normalizeOptional(scene),
                normalizeOptional(providerCode),
                normalizeOptional(sendStatus)
            )
            .stream()
            .map(smsVerificationConverter::toEntity)
            .map(smsVerificationConverter::toResponse)
            .toList();
    }

    private void ensureSendAllowed(
        String mobile,
        String scene,
        String requestIp,
        LocalDateTime now,
        String userAgent
    ) {
        SmsSendRecordDataObject latestSendRecord =
            smsVerificationPersistenceMapper.findLatestSendRecord(mobile, scene);
        if (latestSendRecord != null
            && latestSendRecord.createdAt().isAfter(now.minusSeconds(RESEND_INTERVAL_SECONDS))) {
            createBlockedSendRecord(mobile, scene, requestIp, userAgent, "同手机号同场景发送过于频繁");
            throw new BusinessException(ResponseCode.AUTH_SMS_SEND_RATE_LIMITED, "验证码发送过于频繁，请稍后再试");
        }

        LocalDateTime oneHourAgo = now.minusHours(1);
        int mobileSceneSendCount =
            smsVerificationPersistenceMapper.countSendRecordsByMobileAndSceneSince(mobile, scene, oneHourAgo);
        if (mobileSceneSendCount >= MAX_MOBILE_SCENE_SENDS_PER_HOUR) {
            createBlockedSendRecord(mobile, scene, requestIp, userAgent, "同手机号同场景小时发送次数超限");
            throw new BusinessException(ResponseCode.AUTH_SMS_SEND_RATE_LIMITED, "验证码发送次数过多，请稍后再试");
        }

        int ipSceneSendCount =
            smsVerificationPersistenceMapper.countSendRecordsByIpAndSceneSince(requestIp, scene, oneHourAgo);
        if (ipSceneSendCount >= MAX_IP_SCENE_SENDS_PER_HOUR) {
            createBlockedSendRecord(mobile, scene, requestIp, userAgent, "同 IP 同场景小时发送次数超限");
            throw new BusinessException(ResponseCode.AUTH_SMS_SEND_RATE_LIMITED, "验证码发送请求受限，请稍后再试");
        }
    }

    private void handleWrongCode(SmsVerificationCodeDataObject verificationCode) {
        int nextAttemptCount = verificationCode.attemptCount() + 1;
        String nextStatus = nextAttemptCount >= verificationCode.maxAttemptCount() ? STATUS_LOCKED : STATUS_ACTIVE;
        IncrementSmsVerificationAttemptCommand command = new IncrementSmsVerificationAttemptCommand();
        command.setVerificationId(verificationCode.verificationId());
        command.setStatus(nextStatus);
        smsVerificationPersistenceMapper.incrementAttemptCount(command);
        if (STATUS_LOCKED.equals(nextStatus)) {
            throw new BusinessException(ResponseCode.AUTH_SMS_CODE_ATTEMPT_LIMITED, "验证码错误次数过多，请重新获取");
        }
        throw new BusinessException(ResponseCode.AUTH_SMS_CODE_INVALID, "验证码错误，请重新输入");
    }

    private void throwLatestCodeFailure(String mobile, String scene) {
        SmsVerificationCodeDataObject latestCode =
            smsVerificationPersistenceMapper.findLatestCodeByMobileAndScene(mobile, scene);
        if (latestCode == null) {
            throw new BusinessException(ResponseCode.AUTH_SMS_CODE_INVALID, "请先获取验证码");
        }
        if (STATUS_VERIFIED.equals(latestCode.status())) {
            throw new BusinessException(ResponseCode.AUTH_SMS_CODE_USED, "验证码已使用，请重新获取");
        }
        if (STATUS_EXPIRED.equals(latestCode.status()) || latestCode.expiresAt().isBefore(LocalDateTime.now())) {
            updateStatus(latestCode.verificationId(), STATUS_EXPIRED);
            throw new BusinessException(ResponseCode.AUTH_SMS_CODE_EXPIRED, "验证码已过期，请重新获取");
        }
        if (STATUS_LOCKED.equals(latestCode.status())) {
            throw new BusinessException(ResponseCode.AUTH_SMS_CODE_ATTEMPT_LIMITED, "验证码错误次数过多，请重新获取");
        }
        throw new BusinessException(ResponseCode.AUTH_SMS_CODE_INVALID, "验证码无效，请重新获取");
    }

    private void createBlockedSendRecord(
        String mobile,
        String scene,
        String requestIp,
        String userAgent,
        String failureReason
    ) {
        createSendRecord(
            null,
            mobile,
            scene,
            smsProvider.providerCode(),
            SEND_STATUS_BLOCKED,
            failureReason,
            requestIp,
            userAgent
        );
    }

    private SmsSendResult sendViaProvider(String mobile, String scene, String code) {
        try {
            SmsSendResult sendResult = smsProvider.sendVerificationCode(mobile, scene, code);
            if (sendResult == null || sendResult.sendStatus() == null || sendResult.sendStatus().isBlank()) {
                return new SmsSendResult(SEND_STATUS_FAILED, "短信供应商返回空发送状态");
            }
            return sendResult;
        } catch (RuntimeException exception) {
            // 供应商异常也必须沉淀为发送记录，避免后台排查时缺少失败轨迹。
            return new SmsSendResult(SEND_STATUS_FAILED, exception.getMessage());
        }
    }

    private void createSendRecord(
        Long verificationId,
        String mobile,
        String scene,
        String providerCode,
        String sendStatus,
        String failureReason,
        String requestIp,
        String userAgent
    ) {
        CreateSmsSendRecordCommand command = new CreateSmsSendRecordCommand();
        command.setVerificationId(verificationId);
        command.setMobile(mobile);
        command.setScene(scene);
        command.setProviderCode(providerCode);
        command.setSendStatus(sendStatus);
        command.setFailureReason(failureReason);
        command.setRequestIp(requestIp);
        command.setUserAgent(userAgent);
        smsVerificationPersistenceMapper.insertSendRecord(command);
    }

    private void updateStatus(Long verificationId, String status) {
        UpdateSmsVerificationStatusCommand command = new UpdateSmsVerificationStatusCommand();
        command.setVerificationId(verificationId);
        command.setStatus(status);
        smsVerificationPersistenceMapper.updateVerificationStatus(command);
    }

    private String normalizeScene(String scene) {
        String normalizedScene = normalizeRequired(scene);
        if (!SUPPORTED_SCENES.contains(normalizedScene)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "暂不支持该短信验证码场景");
        }
        return normalizedScene;
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String trimmedUserAgent = userAgent.trim();
        return trimmedUserAgent.length() <= 255 ? trimmedUserAgent : trimmedUserAgent.substring(0, 255);
    }

    private String normalizeFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return null;
        }
        String trimmedFailureReason = failureReason.trim();
        return trimmedFailureReason.length() <= 500 ? trimmedFailureReason : trimmedFailureReason.substring(0, 500);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_IP;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return truncateIp(forwardedFor.split(",")[0].trim());
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return truncateIp(realIp.trim());
        }
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return UNKNOWN_IP;
        }
        return truncateIp(remoteAddr.trim());
    }

    private String truncateIp(String requestIp) {
        return requestIp.length() <= 64 ? requestIp : requestIp.substring(0, 64);
    }

    /**
     * 验证码只允许短时间内用于校验，数据库仅保存随机盐和摘要，避免明文验证码进入持久化或接口响应。
     */
    private String hashCode(String salt, String mobile, String scene, String code) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(
                "%s:%s:%s:%s".formatted(salt, mobile, scene, code).getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private String generateCode() {
        return "%06d".formatted(secureRandom.nextInt(CODE_BOUND));
    }

    private String generateSalt() {
        byte[] saltBytes = new byte[SALT_BYTES];
        secureRandom.nextBytes(saltBytes);
        return HexFormat.of().formatHex(saltBytes);
    }
}
