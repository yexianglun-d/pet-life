package com.petlife.server.modules.auth.persistence;

import com.petlife.server.modules.auth.persistence.command.CreateSmsSendRecordCommand;
import com.petlife.server.modules.auth.persistence.command.CreateSmsVerificationCodeCommand;
import com.petlife.server.modules.auth.persistence.command.IncrementSmsVerificationAttemptCommand;
import com.petlife.server.modules.auth.persistence.command.UpdateSmsVerificationStatusCommand;
import com.petlife.server.modules.auth.persistence.dataobject.SmsSendRecordDataObject;
import com.petlife.server.modules.auth.persistence.dataobject.SmsVerificationCodeDataObject;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 短信验证码持久化接口。
 */
@Mapper
public interface SmsVerificationPersistenceMapper {

    @Insert("""
        INSERT INTO sms_verification_codes (
          mobile, scene, code_hash, salt, expires_at, attempt_count,
          max_attempt_count, status, request_ip, user_agent, created_at, updated_at
        ) VALUES (
          #{mobile}, #{scene}, #{codeHash}, #{salt}, #{expiresAt}, 0,
          #{maxAttemptCount}, #{status}, #{requestIp}, #{userAgent}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "verificationId")
    int insertVerificationCode(CreateSmsVerificationCodeCommand command);

    @Insert("""
        INSERT INTO sms_send_records (
          verification_id, mobile, scene, provider_code, send_status,
          failure_reason, request_ip, user_agent, created_at
        ) VALUES (
          #{verificationId}, #{mobile}, #{scene}, #{providerCode}, #{sendStatus},
          #{failureReason}, #{requestIp}, #{userAgent}, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "sendRecordId")
    int insertSendRecord(CreateSmsSendRecordCommand command);

    @Update("""
        UPDATE sms_verification_codes
        SET status = 'expired',
            updated_at = CURRENT_TIMESTAMP
        WHERE mobile = #{mobile}
          AND scene = #{scene}
          AND status = 'active'
        """)
    int expireActiveCodesByMobileAndScene(
        @Param("mobile") String mobile,
        @Param("scene") String scene
    );

    @Select("""
        SELECT
          id AS verificationId,
          mobile AS mobile,
          scene AS scene,
          code_hash AS codeHash,
          salt AS salt,
          expires_at AS expiresAt,
          verified_at AS verifiedAt,
          attempt_count AS attemptCount,
          max_attempt_count AS maxAttemptCount,
          status AS status,
          request_ip AS requestIp,
          user_agent AS userAgent,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM sms_verification_codes
        WHERE mobile = #{mobile}
          AND scene = #{scene}
          AND status = 'active'
        ORDER BY created_at DESC, id DESC
        LIMIT 1
        FOR UPDATE
        """)
    SmsVerificationCodeDataObject lockLatestActiveCode(
        @Param("mobile") String mobile,
        @Param("scene") String scene
    );

    @Select("""
        SELECT
          id AS verificationId,
          mobile AS mobile,
          scene AS scene,
          code_hash AS codeHash,
          salt AS salt,
          expires_at AS expiresAt,
          verified_at AS verifiedAt,
          attempt_count AS attemptCount,
          max_attempt_count AS maxAttemptCount,
          status AS status,
          request_ip AS requestIp,
          user_agent AS userAgent,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM sms_verification_codes
        WHERE mobile = #{mobile}
          AND scene = #{scene}
        ORDER BY created_at DESC, id DESC
        LIMIT 1
        """)
    SmsVerificationCodeDataObject findLatestCodeByMobileAndScene(
        @Param("mobile") String mobile,
        @Param("scene") String scene
    );

    @Update("""
        UPDATE sms_verification_codes
        SET status = #{status},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{verificationId}
        """)
    int updateVerificationStatus(UpdateSmsVerificationStatusCommand command);

    @Update("""
        UPDATE sms_verification_codes
        SET status = 'verified',
            verified_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{verificationId}
          AND status = 'active'
        """)
    int markVerificationCodeVerified(@Param("verificationId") Long verificationId);

    @Update("""
        UPDATE sms_verification_codes
        SET attempt_count = attempt_count + 1,
            status = #{status},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{verificationId}
          AND status = 'active'
        """)
    int incrementAttemptCount(IncrementSmsVerificationAttemptCommand command);

    @Select("""
        SELECT
          id AS sendRecordId,
          verification_id AS verificationId,
          mobile AS mobile,
          scene AS scene,
          provider_code AS providerCode,
          send_status AS sendStatus,
          failure_reason AS failureReason,
          request_ip AS requestIp,
          user_agent AS userAgent,
          created_at AS createdAt
        FROM sms_send_records
        WHERE mobile = #{mobile}
          AND scene = #{scene}
        ORDER BY created_at DESC, id DESC
        LIMIT 1
        """)
    SmsSendRecordDataObject findLatestSendRecord(
        @Param("mobile") String mobile,
        @Param("scene") String scene
    );

    @Select("""
        SELECT COUNT(1)
        FROM sms_send_records
        WHERE mobile = #{mobile}
          AND scene = #{scene}
          AND created_at >= #{createdAfter}
        """)
    int countSendRecordsByMobileAndSceneSince(
        @Param("mobile") String mobile,
        @Param("scene") String scene,
        @Param("createdAfter") LocalDateTime createdAfter
    );

    @Select("""
        SELECT COUNT(1)
        FROM sms_send_records
        WHERE request_ip = #{requestIp}
          AND scene = #{scene}
          AND created_at >= #{createdAfter}
        """)
    int countSendRecordsByIpAndSceneSince(
        @Param("requestIp") String requestIp,
        @Param("scene") String scene,
        @Param("createdAfter") LocalDateTime createdAfter
    );

    @Select("""
        SELECT
          id AS verificationId,
          mobile AS mobile,
          scene AS scene,
          code_hash AS codeHash,
          salt AS salt,
          expires_at AS expiresAt,
          verified_at AS verifiedAt,
          attempt_count AS attemptCount,
          max_attempt_count AS maxAttemptCount,
          status AS status,
          request_ip AS requestIp,
          user_agent AS userAgent,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM sms_verification_codes
        WHERE (#{mobile} IS NULL OR mobile = #{mobile})
          AND (#{scene} IS NULL OR scene = #{scene})
          AND (#{status} IS NULL OR status = #{status})
        ORDER BY created_at DESC, id DESC
        LIMIT 200
        """)
    List<SmsVerificationCodeDataObject> listVerificationCodes(
        @Param("mobile") String mobile,
        @Param("scene") String scene,
        @Param("status") String status
    );

    @Select("""
        SELECT
          id AS sendRecordId,
          verification_id AS verificationId,
          mobile AS mobile,
          scene AS scene,
          provider_code AS providerCode,
          send_status AS sendStatus,
          failure_reason AS failureReason,
          request_ip AS requestIp,
          user_agent AS userAgent,
          created_at AS createdAt
        FROM sms_send_records
        WHERE (#{mobile} IS NULL OR mobile = #{mobile})
          AND (#{scene} IS NULL OR scene = #{scene})
          AND (#{providerCode} IS NULL OR provider_code = #{providerCode})
          AND (#{sendStatus} IS NULL OR send_status = #{sendStatus})
        ORDER BY created_at DESC, id DESC
        LIMIT 200
        """)
    List<SmsSendRecordDataObject> listSendRecords(
        @Param("mobile") String mobile,
        @Param("scene") String scene,
        @Param("providerCode") String providerCode,
        @Param("sendStatus") String sendStatus
    );
}
