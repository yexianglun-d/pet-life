package com.petlife.server.modules.auth.converter;

import com.petlife.server.modules.auth.domain.entity.SmsSendRecordEntity;
import com.petlife.server.modules.auth.domain.entity.SmsVerificationCodeEntity;
import com.petlife.server.modules.auth.dto.response.SmsSendRecordResponse;
import com.petlife.server.modules.auth.dto.response.SmsVerificationRecordResponse;
import com.petlife.server.modules.auth.persistence.dataobject.SmsSendRecordDataObject;
import com.petlife.server.modules.auth.persistence.dataobject.SmsVerificationCodeDataObject;
import org.springframework.stereotype.Component;

/**
 * 短信验证码记录转换器。
 */
@Component
public class SmsVerificationConverter {

    public SmsVerificationCodeEntity toEntity(SmsVerificationCodeDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new SmsVerificationCodeEntity(
            dataObject.verificationId(),
            dataObject.mobile(),
            dataObject.scene(),
            dataObject.expiresAt(),
            dataObject.verifiedAt(),
            dataObject.attemptCount(),
            dataObject.maxAttemptCount(),
            dataObject.status(),
            dataObject.requestIp(),
            dataObject.userAgent(),
            dataObject.createdAt(),
            dataObject.updatedAt()
        );
    }

    public SmsSendRecordEntity toEntity(SmsSendRecordDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new SmsSendRecordEntity(
            dataObject.sendRecordId(),
            dataObject.verificationId(),
            dataObject.mobile(),
            dataObject.scene(),
            dataObject.providerCode(),
            dataObject.sendStatus(),
            dataObject.failureReason(),
            dataObject.requestIp(),
            dataObject.userAgent(),
            dataObject.createdAt()
        );
    }

    public SmsVerificationRecordResponse toResponse(SmsVerificationCodeEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SmsVerificationRecordResponse(
            entity.getVerificationId().toString(),
            entity.getMobile(),
            entity.getScene(),
            entity.getExpiresAt(),
            entity.getVerifiedAt(),
            entity.getAttemptCount(),
            entity.getMaxAttemptCount(),
            entity.getStatus(),
            entity.getRequestIp(),
            entity.getUserAgent(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public SmsSendRecordResponse toResponse(SmsSendRecordEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SmsSendRecordResponse(
            entity.getSendRecordId().toString(),
            entity.getVerificationId() == null ? null : entity.getVerificationId().toString(),
            entity.getMobile(),
            entity.getScene(),
            entity.getProviderCode(),
            entity.getSendStatus(),
            entity.getFailureReason(),
            entity.getRequestIp(),
            entity.getUserAgent(),
            entity.getCreatedAt()
        );
    }
}
