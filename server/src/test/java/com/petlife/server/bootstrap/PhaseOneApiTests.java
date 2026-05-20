package com.petlife.server.bootstrap;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.petlife.server.modules.family.persistence.FamilyPersistenceMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhaseOneApiTests {

    private static final String TEST_SMS_SCENE = "login";
    private static final String TEST_SMS_CODE = "654321";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FamilyPersistenceMapper familyPersistenceMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeAll
    void ensureSchemaExtensions() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS admin_accounts (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              username VARCHAR(50) NOT NULL COMMENT '后台登录账号',
              password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码摘要',
              display_name VARCHAR(50) NOT NULL COMMENT '管理员展示名称',
              role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
              status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常 2-禁用',
              last_login_at DATETIME DEFAULT NULL COMMENT '最近登录时间',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
              PRIMARY KEY (id),
              UNIQUE KEY uk_admin_accounts_username (username),
              KEY idx_admin_accounts_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台管理员账号表'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS admin_sessions (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              admin_account_id BIGINT UNSIGNED NOT NULL COMMENT '管理员账号 ID',
              refresh_token_hash CHAR(64) NOT NULL COMMENT '刷新令牌 SHA256 摘要',
              expires_at DATETIME NOT NULL COMMENT '会话过期时间',
              revoked_at DATETIME DEFAULT NULL COMMENT '吊销时间',
              last_active_at DATETIME DEFAULT NULL COMMENT '最近活跃时间',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              PRIMARY KEY (id),
              UNIQUE KEY uk_admin_sessions_refresh_hash (refresh_token_hash),
              KEY idx_admin_sessions_account (admin_account_id),
              KEY idx_admin_sessions_active (revoked_at, expires_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台管理员登录会话表'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS sms_verification_codes (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              mobile VARCHAR(20) NOT NULL COMMENT '手机号',
              scene VARCHAR(30) NOT NULL COMMENT '业务场景',
              code_hash CHAR(64) NOT NULL COMMENT '验证码 SHA256 摘要',
              salt VARCHAR(64) NOT NULL COMMENT '验证码摘要随机盐',
              expires_at DATETIME NOT NULL COMMENT '过期时间',
              verified_at DATETIME DEFAULT NULL COMMENT '验证通过时间',
              attempt_count INT NOT NULL DEFAULT 0 COMMENT '已尝试次数',
              max_attempt_count INT NOT NULL DEFAULT 5 COMMENT '最大尝试次数',
              status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/verified/expired/locked/send_failed',
              request_ip VARCHAR(64) DEFAULT NULL COMMENT '请求 IP',
              user_agent VARCHAR(255) DEFAULT NULL COMMENT '客户端标识',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              PRIMARY KEY (id),
              KEY idx_sms_codes_mobile_scene_status (mobile, scene, status, created_at DESC),
              KEY idx_sms_codes_expire (expires_at, status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信验证码记录表'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS sms_send_records (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              verification_id BIGINT UNSIGNED DEFAULT NULL COMMENT '验证码记录 ID',
              mobile VARCHAR(20) NOT NULL COMMENT '手机号',
              scene VARCHAR(30) NOT NULL COMMENT '业务场景',
              provider_code VARCHAR(64) NOT NULL COMMENT '短信供应商编码',
              send_status VARCHAR(20) NOT NULL COMMENT '发送状态：accepted/failed/blocked',
              failure_reason VARCHAR(500) DEFAULT NULL COMMENT '失败或拦截原因',
              request_ip VARCHAR(64) DEFAULT NULL COMMENT '请求 IP',
              user_agent VARCHAR(255) DEFAULT NULL COMMENT '客户端标识',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              PRIMARY KEY (id),
              KEY idx_sms_send_mobile_scene_time (mobile, scene, created_at DESC),
              KEY idx_sms_send_ip_scene_time (request_ip, scene, created_at DESC),
              KEY idx_sms_send_provider_status (provider_code, send_status),
              CONSTRAINT fk_sms_send_verification FOREIGN KEY (verification_id) REFERENCES sms_verification_codes (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信发送记录表'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS media_assets (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              uploader_user_id BIGINT UNSIGNED NOT NULL COMMENT '上传用户 ID',
              biz_type VARCHAR(30) NOT NULL COMMENT '业务类型',
              media_type VARCHAR(20) NOT NULL COMMENT '媒体类型',
              file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
              object_key VARCHAR(255) NOT NULL COMMENT '对象存储路径',
              bucket_name VARCHAR(100) NOT NULL COMMENT '存储桶名称',
              cdn_url VARCHAR(255) DEFAULT NULL COMMENT 'CDN 访问地址',
              content_type VARCHAR(100) DEFAULT NULL COMMENT '文件内容类型',
              file_size BIGINT UNSIGNED DEFAULT NULL COMMENT '文件大小',
              width INT DEFAULT NULL COMMENT '媒体宽度',
              height INT DEFAULT NULL COMMENT '媒体高度',
              duration_ms INT DEFAULT NULL COMMENT '视频时长',
              file_hash CHAR(64) DEFAULT NULL COMMENT '文件哈希值',
              upload_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '上传状态',
              review_status VARCHAR(20) NOT NULL DEFAULT 'pending_review' COMMENT '审核状态',
              completed_at DATETIME DEFAULT NULL COMMENT '上传完成时间',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
              PRIMARY KEY (id),
              UNIQUE KEY uk_media_assets_object (object_key),
              KEY idx_media_assets_user (uploader_user_id),
              KEY idx_media_assets_status (upload_status, review_status),
              KEY idx_media_assets_hash (file_hash)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='媒体资产表'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS service_city_configs (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              city_code VARCHAR(32) NOT NULL COMMENT '城市编码',
              city_name VARCHAR(50) NOT NULL COMMENT '城市名称',
              opened TINYINT NOT NULL DEFAULT 0 COMMENT '是否开通：0-否 1-是',
              unavailable_reason VARCHAR(255) DEFAULT NULL COMMENT '未开通原因',
              sort_order INT NOT NULL DEFAULT 0 COMMENT '展示排序',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
              PRIMARY KEY (id),
              UNIQUE KEY uk_service_city_configs_code (city_code),
              KEY idx_service_city_configs_opened_sort (opened, sort_order)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务城市开通配置表'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS audit_logs (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              operator_type VARCHAR(20) NOT NULL COMMENT '操作者类型：system/user/admin',
              operator_id VARCHAR(64) DEFAULT NULL COMMENT '操作者标识',
              target_type VARCHAR(30) NOT NULL COMMENT '目标类型',
              target_id VARCHAR(64) NOT NULL COMMENT '目标标识',
              action VARCHAR(50) NOT NULL COMMENT '操作动作',
              detail_json JSON DEFAULT NULL COMMENT '操作详情',
              ip_address VARCHAR(64) DEFAULT NULL COMMENT '操作 IP',
              user_agent VARCHAR(255) DEFAULT NULL COMMENT '客户端标识',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              PRIMARY KEY (id),
              KEY idx_audit_logs_target (target_type, target_id, created_at DESC),
              KEY idx_audit_logs_operator (operator_type, operator_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审计日志表'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS reminder_templates (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
              reminder_type VARCHAR(30) NOT NULL COMMENT '提醒类型',
              default_reminder_mode VARCHAR(20) NOT NULL COMMENT '默认提醒模式：single/cycle',
              default_advance_value INT NOT NULL DEFAULT 0 COMMENT '默认提前量',
              default_advance_unit VARCHAR(20) NOT NULL DEFAULT 'day' COMMENT '默认提前单位：day/week/month',
              default_cycle_value INT DEFAULT NULL COMMENT '默认周期值',
              default_cycle_unit VARCHAR(20) DEFAULT NULL COMMENT '默认周期单位：day/week/month',
              applicable_pet_type VARCHAR(20) NOT NULL DEFAULT 'all' COMMENT '适用宠物类型：all/cat/dog/other',
              enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-否 1-是',
              sort_order INT NOT NULL DEFAULT 0 COMMENT '展示排序',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
              PRIMARY KEY (id),
              KEY idx_reminder_templates_type_enabled (reminder_type, enabled),
              KEY idx_reminder_templates_pet_sort (applicable_pet_type, sort_order)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提醒模板表'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS message_templates (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              template_code VARCHAR(64) NOT NULL COMMENT '模板编码',
              channel_type VARCHAR(20) NOT NULL COMMENT '渠道类型：sms/push/inbox',
              title_template VARCHAR(100) DEFAULT NULL COMMENT '标题模板',
              content_template TEXT NOT NULL COMMENT '内容模板',
              status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/inactive',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              PRIMARY KEY (id),
              UNIQUE KEY uk_message_templates_code (template_code, channel_type)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息模板表'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS notification_channel_configs (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              channel_type VARCHAR(20) NOT NULL COMMENT '渠道类型：inbox/sms/push',
              provider_code VARCHAR(64) NOT NULL COMMENT '供应商编码',
              provider_name VARCHAR(100) NOT NULL COMMENT '供应商名称',
              enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用：0-否 1-是',
              config_status VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '配置状态：draft/ready/disabled',
              remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
              PRIMARY KEY (id),
              UNIQUE KEY uk_notification_channel_provider (channel_type, provider_code),
              KEY idx_notification_channel_enabled (channel_type, enabled),
              KEY idx_notification_channel_status (config_status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知渠道配置表'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS moderation_tasks (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              target_type VARCHAR(30) NOT NULL COMMENT '目标类型：community_post/community_question',
              target_id BIGINT UNSIGNED NOT NULL COMMENT '目标 ID',
              content_type VARCHAR(30) NOT NULL COMMENT '内容形态：text/image_text/video/qa',
              content_snapshot JSON DEFAULT NULL COMMENT '审核内容快照',
              provider_code VARCHAR(64) NOT NULL DEFAULT 'dev_noop' COMMENT '审核供应商编码',
              review_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/approved/rejected/failed',
              review_result JSON DEFAULT NULL COMMENT '审核结果',
              risk_labels JSON DEFAULT NULL COMMENT '风险标签',
              failure_reason VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
              callback_payload JSON DEFAULT NULL COMMENT '供应商回调载荷',
              reviewed_at DATETIME DEFAULT NULL COMMENT '审核完成时间',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              PRIMARY KEY (id),
              KEY idx_moderation_tasks_target (target_type, target_id),
              KEY idx_moderation_tasks_status (review_status, created_at),
              KEY idx_moderation_tasks_provider (provider_code, review_status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审核任务表'
            """);
        ensureModerationTaskSchema();
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS user_push_device_tokens (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              user_id BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
              platform VARCHAR(20) NOT NULL COMMENT '客户端平台：ios/android',
              provider_code VARCHAR(64) NOT NULL COMMENT 'Push 供应商编码',
              device_token VARCHAR(512) NOT NULL COMMENT '设备 Token',
              device_id VARCHAR(128) DEFAULT NULL COMMENT '客户端设备标识',
              app_version VARCHAR(40) DEFAULT NULL COMMENT 'App 版本',
              enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-否 1-是',
              last_registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近注册时间',
              unregistered_at DATETIME DEFAULT NULL COMMENT '解绑时间',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              PRIMARY KEY (id),
              UNIQUE KEY uk_push_device_provider_token (provider_code, device_token),
              KEY idx_push_device_user_enabled (user_id, enabled)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户 Push 设备 Token 表'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS push_tasks (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              user_id BIGINT UNSIGNED NOT NULL COMMENT '接收用户 ID',
              notification_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联站内通知 ID',
              notify_type VARCHAR(30) NOT NULL COMMENT '通知类型',
              biz_type VARCHAR(50) DEFAULT NULL COMMENT '业务类型',
              biz_id BIGINT UNSIGNED DEFAULT NULL COMMENT '业务 ID',
              title VARCHAR(100) NOT NULL COMMENT 'Push 标题',
              content VARCHAR(500) NOT NULL COMMENT 'Push 内容',
              provider_code VARCHAR(64) NOT NULL COMMENT 'Push 供应商编码',
              task_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '任务状态：pending/skipped/failed/sent',
              failure_reason VARCHAR(500) DEFAULT NULL COMMENT '失败或跳过原因',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              PRIMARY KEY (id),
              KEY idx_push_tasks_user_status (user_id, task_status, created_at),
              KEY idx_push_tasks_notification (notification_id),
              KEY idx_push_tasks_provider_status (provider_code, task_status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Push 任务表'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS push_delivery_records (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              push_task_id BIGINT UNSIGNED NOT NULL COMMENT 'Push 任务 ID',
              device_token_id BIGINT UNSIGNED NOT NULL COMMENT '设备 Token ID',
              user_id BIGINT UNSIGNED NOT NULL COMMENT '接收用户 ID',
              provider_code VARCHAR(64) NOT NULL COMMENT 'Push 供应商编码',
              delivery_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '投递状态：pending/skipped/failed/sent',
              failure_reason VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
              attempted_at DATETIME DEFAULT NULL COMMENT '投递尝试时间',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              PRIMARY KEY (id),
              KEY idx_push_delivery_task (push_task_id),
              KEY idx_push_delivery_user_status (user_id, delivery_status, created_at),
              KEY idx_push_delivery_provider_status (provider_code, delivery_status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Push 投递记录表'
            """);
        ensureCommunityReportAdminNotesColumn();
        ensureCommunityClosedLoopSchema();
        ensureAdminAccount();
    }

    private void ensureModerationTaskSchema() {
        ensureModerationTaskColumn(
            "content_type",
            "ALTER TABLE moderation_tasks ADD COLUMN content_type VARCHAR(30) NOT NULL DEFAULT 'text' COMMENT '内容形态：text/image_text/video/qa' AFTER target_id"
        );
        ensureModerationTaskColumn(
            "content_snapshot",
            "ALTER TABLE moderation_tasks ADD COLUMN content_snapshot JSON DEFAULT NULL COMMENT '审核内容快照' AFTER content_type"
        );
        ensureModerationTaskColumn(
            "provider_code",
            "ALTER TABLE moderation_tasks ADD COLUMN provider_code VARCHAR(64) NOT NULL DEFAULT 'dev_noop' COMMENT '审核供应商编码' AFTER content_snapshot"
        );
        ensureModerationTaskColumn(
            "risk_labels",
            "ALTER TABLE moderation_tasks ADD COLUMN risk_labels JSON DEFAULT NULL COMMENT '风险标签' AFTER review_result"
        );
        ensureModerationTaskColumn(
            "failure_reason",
            "ALTER TABLE moderation_tasks ADD COLUMN failure_reason VARCHAR(500) DEFAULT NULL COMMENT '失败原因' AFTER risk_labels"
        );
        ensureModerationTaskColumn(
            "callback_payload",
            "ALTER TABLE moderation_tasks ADD COLUMN callback_payload JSON DEFAULT NULL COMMENT '供应商回调载荷' AFTER failure_reason"
        );
        ensureModerationTaskColumn(
            "reviewed_at",
            "ALTER TABLE moderation_tasks ADD COLUMN reviewed_at DATETIME DEFAULT NULL COMMENT '审核完成时间' AFTER callback_payload"
        );
        if (tableColumnExists("moderation_tasks", "review_type")) {
            jdbcTemplate.execute("""
                ALTER TABLE moderation_tasks
                MODIFY COLUMN review_type VARCHAR(20) DEFAULT NULL COMMENT '历史审核类型'
                """);
        }
    }

    private void ensureModerationTaskColumn(String columnName, String alterSql) {
        if (!tableColumnExists("moderation_tasks", columnName)) {
            jdbcTemplate.execute(alterSql);
        }
    }

    private boolean tableColumnExists(String tableName, String columnName) {
        Integer columnCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = ?
              AND COLUMN_NAME = ?
            """, Integer.class, tableName, columnName);
        return columnCount != null && columnCount > 0;
    }

    private void ensureCommunityReportAdminNotesColumn() {
        Integer columnCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'community_reports'
              AND COLUMN_NAME = 'admin_notes'
            """, Integer.class);
        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute("""
                ALTER TABLE community_reports
                ADD COLUMN admin_notes VARCHAR(500) DEFAULT NULL COMMENT '管理员处理备注' AFTER processed_by
                """);
        }
    }

    private void ensureCommunityClosedLoopSchema() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS community_topics (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              topic_name VARCHAR(100) NOT NULL COMMENT '话题名称',
              topic_desc VARCHAR(500) DEFAULT NULL COMMENT '话题说明',
              city_code VARCHAR(32) DEFAULT NULL COMMENT '城市编码',
              status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-停用',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              PRIMARY KEY (id),
              KEY idx_community_topics_city_status (city_code, status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区话题表'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS user_follows (
              id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
              follower_user_id BIGINT UNSIGNED NOT NULL COMMENT '关注者用户 ID',
              followed_user_id BIGINT UNSIGNED NOT NULL COMMENT '被关注用户 ID',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              PRIMARY KEY (id),
              UNIQUE KEY uk_user_follows_pair (follower_user_id, followed_user_id),
              KEY idx_user_follows_followed (followed_user_id, created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户关注关系表'
            """);
        ensureCommunityPostColumn(
            "topic_id",
            "ALTER TABLE community_posts ADD COLUMN topic_id BIGINT UNSIGNED DEFAULT NULL COMMENT '话题 ID' AFTER pet_id"
        );
        ensureCommunityPostColumn(
            "media_list",
            "ALTER TABLE community_posts ADD COLUMN media_list JSON DEFAULT NULL COMMENT '媒体资产 ID 列表' AFTER content"
        );
    }

    private void ensureCommunityPostColumn(String columnName, String alterSql) {
        Integer columnCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'community_posts'
              AND COLUMN_NAME = ?
            """, Integer.class, columnName);
        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }

    private void ensureAdminAccount() {
        jdbcTemplate.update("""
            INSERT INTO admin_accounts (
              username, password_hash, display_name, role_code, status,
              created_at, updated_at
            ) VALUES (
              'admin', ?, '测试管理员', 'super_admin', 1,
              CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON DUPLICATE KEY UPDATE
              password_hash = VALUES(password_hash),
              display_name = VALUES(display_name),
              role_code = VALUES(role_code),
              status = 1,
              deleted_at = NULL,
              updated_at = CURRENT_TIMESTAMP
            """, passwordEncoder.encode("petlife123"));
    }

    @Test
    void shouldLoginBySms() throws Exception {
        sendSmsCodeForLogin("13800000000");

        mockMvc.perform(post("/api/v1/auth/login/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mobile": "13800000000",
                      "code": "%s"
                    }
                    """.formatted(TEST_SMS_CODE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")))
            .andExpect(jsonPath("$.data.access_token").exists())
            .andExpect(jsonPath("$.data.user.nickname", is("Momo")));
    }

    @Test
    void shouldSendSmsCodeWithoutLeakingPlainCode() throws Exception {
        String mobile = uniqueMobile("131");

        String responseBody = mockMvc.perform(post("/api/v1/auth/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mobile": "%s",
                      "scene": "login"
                    }
                    """.formatted(mobile)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sent", is(true)))
            .andExpect(jsonPath("$.data.provider_code", is("dev_noop")))
            .andExpect(jsonPath("$.data.mocked_code").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String codeHash = latestSmsCodeHash(mobile);
        String salt = latestSmsCodeSalt(mobile);
        assertNotNull(codeHash);
        assertNotNull(salt);
        assertFalse(responseBody.contains(codeHash));
        assertFalse(responseBody.contains(salt));
        assertFalse(responseBody.contains("123456"));
    }

    @Test
    void shouldRateLimitSmsSendByMobileSceneAndIp() throws Exception {
        String mobile = uniqueMobile("132");

        mockMvc.perform(post("/api/v1/auth/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mobile": "%s",
                      "scene": "login"
                    }
                    """.formatted(mobile)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(post("/api/v1/auth/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mobile": "%s",
                      "scene": "login"
                    }
                    """.formatted(mobile)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code", is("AUTH_SMS_SEND_RATE_LIMITED")));
    }

    @Test
    void shouldRejectWrongSmsCodeAndLockAfterMaxAttempts() throws Exception {
        String mobile = uniqueMobile("133");
        sendSmsCodeForLogin(mobile);

        for (int index = 0; index < 4; index++) {
            mockMvc.perform(post("/api/v1/auth/login/sms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "mobile": "%s",
                          "code": "000000"
                        }
                        """.formatted(mobile)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("AUTH_SMS_CODE_INVALID")));
        }

        mockMvc.perform(post("/api/v1/auth/login/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mobile": "%s",
                      "code": "000000"
                    }
                    """.formatted(mobile)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code", is("AUTH_SMS_CODE_ATTEMPT_LIMITED")));

        mockMvc.perform(post("/api/v1/auth/login/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mobile": "%s",
                      "code": "%s"
                    }
                    """.formatted(mobile, TEST_SMS_CODE)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code", is("AUTH_SMS_CODE_ATTEMPT_LIMITED")));

        Integer attemptCount = jdbcTemplate.queryForObject("""
            SELECT attempt_count
            FROM sms_verification_codes
            WHERE mobile = ? AND scene = 'login'
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """, Integer.class, mobile);
        String status = jdbcTemplate.queryForObject("""
            SELECT status
            FROM sms_verification_codes
            WHERE mobile = ? AND scene = 'login'
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """, String.class, mobile);
        assertEquals(5, attemptCount);
        assertEquals("locked", status);
    }

    @Test
    void shouldRejectExpiredSmsCode() throws Exception {
        String mobile = uniqueMobile("134");
        sendSmsCodeForLogin(mobile);
        jdbcTemplate.update("""
            UPDATE sms_verification_codes
            SET expires_at = DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 SECOND)
            WHERE mobile = ? AND scene = 'login'
            """, mobile);

        mockMvc.perform(post("/api/v1/auth/login/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mobile": "%s",
                      "code": "%s"
                    }
                    """.formatted(mobile, TEST_SMS_CODE)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("AUTH_SMS_CODE_EXPIRED")));
    }

    @Test
    void shouldInvalidateSmsCodeAfterSuccessfulLogin() throws Exception {
        String mobile = uniqueMobile("135");
        sendSmsCodeForLogin(mobile);

        mockMvc.perform(post("/api/v1/auth/login/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mobile": "%s",
                      "code": "%s"
                    }
                    """.formatted(mobile, TEST_SMS_CODE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(post("/api/v1/auth/login/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mobile": "%s",
                      "code": "%s"
                    }
                    """.formatted(mobile, TEST_SMS_CODE)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("AUTH_SMS_CODE_USED")));
    }

    @Test
    void shouldRejectWrongSmsCodeWithoutSendingCode() throws Exception {
        String mobile = uniqueMobile("136");

        mockMvc.perform(post("/api/v1/auth/login/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mobile": "%s",
                      "code": "000000"
                    }
                    """.formatted(mobile)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("AUTH_SMS_CODE_INVALID")));
    }

    @Test
    void shouldQuerySmsRecordsFromAdminWithoutLeakingPlainCode() throws Exception {
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String mobile = uniqueMobile("137");
        sendSmsCodeForLogin(mobile);

        String verificationResponseBody = mockMvc.perform(get("/api/v1/admin/sms-verifications")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .param("mobile", mobile)
                .param("scene", "login"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].mobile", is(mobile)))
            .andExpect(jsonPath("$.data[0].scene", is("login")))
            .andExpect(jsonPath("$.data[0].attempt_count", is(0)))
            .andExpect(jsonPath("$.data[0].code_hash").doesNotExist())
            .andExpect(jsonPath("$.data[0].salt").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String sendRecordResponseBody = mockMvc.perform(get("/api/v1/admin/sms-send-records")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .param("mobile", mobile)
                .param("scene", "login")
                .param("provider_code", "dev_noop")
                .param("send_status", "accepted"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].mobile", is(mobile)))
            .andExpect(jsonPath("$.data[0].provider_code", is("dev_noop")))
            .andExpect(jsonPath("$.data[0].send_status", is("accepted")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertFalse(verificationResponseBody.contains(TEST_SMS_CODE));
        assertFalse(sendRecordResponseBody.contains(TEST_SMS_CODE));
        assertFalse(verificationResponseBody.contains(latestSmsCodeHash(mobile)));
        assertFalse(verificationResponseBody.contains(latestSmsCodeSalt(mobile)));
    }

    @Test
    void shouldRejectSmsRecordAdminQueryWithoutAdminAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sms-verifications"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/v1/admin/sms-send-records")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader(uniqueMobile("138"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void shouldRefreshAndLogoutSession() throws Exception {
        LoginTokensFixture loginTokens = loginTokens("13800000000");

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refresh_token": "%s"
                    }
                    """.formatted(loginTokens.refreshToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.access_token").exists())
            .andExpect(jsonPath("$.data.refresh_token").exists())
            .andReturn();

        String refreshedResponseBody = refreshResult.getResponse().getContentAsString();
        String nextAccessToken = JsonPath.read(refreshedResponseBody, "$.data.access_token");
        String nextRefreshToken = JsonPath.read(refreshedResponseBody, "$.data.refresh_token");

        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, loginTokens.authorizationHeader()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + nextAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_pet.pet_name", is("Momo")));

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refresh_token": "%s"
                    }
                    """.formatted(nextRefreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + nextAccessToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void shouldLoginRefreshAndLogoutAdminSession() throws Exception {
        LoginTokensFixture loginTokens = adminLoginTokens();

        mockMvc.perform(get("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, loginTokens.authorizationHeader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/admin/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refresh_token": "%s"
                    }
                    """.formatted(loginTokens.refreshToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.access_token").exists())
            .andExpect(jsonPath("$.data.refresh_token").exists())
            .andReturn();

        String refreshedResponseBody = refreshResult.getResponse().getContentAsString();
        String nextAccessToken = JsonPath.read(refreshedResponseBody, "$.data.access_token");
        String nextRefreshToken = JsonPath.read(refreshedResponseBody, "$.data.refresh_token");

        mockMvc.perform(get("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, loginTokens.authorizationHeader()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(post("/api/v1/admin/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refresh_token": "%s"
                    }
                    """.formatted(nextRefreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(get("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + nextAccessToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void shouldRejectAdminEndpointWithUserToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void shouldReturnCurrentUser() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_pet.pet_name", is("Momo")))
            .andExpect(jsonPath("$.data.family_summary.family_name", is("Momo的家庭")));
    }

    @Test
    void shouldReturnUserSettings() throws Exception {
        mockMvc.perform(get("/api/v1/me/settings")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user_id").exists())
            .andExpect(jsonPath("$.data.mobile", is("13800000000")))
            .andExpect(jsonPath("$.data.nickname", is("Momo")))
            .andExpect(jsonPath("$.data.notification_enabled", is(true)))
            .andExpect(jsonPath("$.data.privacy_level", is("normal")));
    }

    @Test
    void shouldUpdateUserProfileAndCity() throws Exception {
        String authorizationHeader = authorizationHeader();

        mockMvc.perform(patch("/api/v1/me/profile")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nickname": "陪伴中的Momo"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nickname", is("陪伴中的Momo")));

        mockMvc.perform(patch("/api/v1/me/settings/city")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "city_code": "330100",
                      "city_name": "杭州"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.city_code", is("330100")))
            .andExpect(jsonPath("$.data.city_name", is("杭州")));

        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.nickname", is("陪伴中的Momo")))
            .andExpect(jsonPath("$.data.user.city_code", is("330100")))
            .andExpect(jsonPath("$.data.user.city_name", is("杭州")));
    }

    @Test
    void shouldUpdateNotificationSettings() throws Exception {
        String authorizationHeader = authorizationHeader();

        mockMvc.perform(patch("/api/v1/me/settings/notifications")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "notification_enabled": false,
                      "privacy_level": "private"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.notification_enabled", is(false)))
            .andExpect(jsonPath("$.data.privacy_level", is("private")));

        mockMvc.perform(get("/api/v1/me/settings")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.notification_enabled", is(false)))
            .andExpect(jsonPath("$.data.privacy_level", is("private")));
    }

    @Test
    void shouldQueryUsersFromAdminEndpoints() throws Exception {
        String authorizationHeader = authorizationHeader();
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String userId = currentUserId(authorizationHeader);
        String petId = currentPetId(authorizationHeader);

        mockMvc.perform(patch("/api/v1/me/profile")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nickname": "后台查询用户Momo"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/me/settings/city")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "city_code": "330100",
                      "city_name": "杭州"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/me/settings/notifications")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "notification_enabled": false,
                      "privacy_level": "private"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get(
                "/api/v1/admin/users?keyword=后台查询&mobile=13800000000&nickname=后台查询用户&city_code=330100&notification_enabled=false&privacy_level=private")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.user_id == '%s')].mobile".formatted(userId), is(List.of("13800000000"))))
            .andExpect(jsonPath("$.data[?(@.user_id == '%s')].settings.notification_enabled"
                .formatted(userId), is(List.of(false))))
            .andExpect(jsonPath("$.data[?(@.user_id == '%s')].settings.privacy_level"
                .formatted(userId), is(List.of("private"))))
            .andExpect(jsonPath("$.data[?(@.user_id == '%s')].primary_family.family_name"
                .formatted(userId), is(List.of("Momo的家庭"))))
            .andExpect(jsonPath("$.data[?(@.user_id == '%s')].current_pet.pet_id"
                .formatted(userId), is(List.of(petId))));

        mockMvc.perform(get("/api/v1/admin/users/%s".formatted(userId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user_id", is(userId)))
            .andExpect(jsonPath("$.data.nickname", is("后台查询用户Momo")))
            .andExpect(jsonPath("$.data.city_code", is("330100")))
            .andExpect(jsonPath("$.data.settings.notification_enabled", is(false)))
            .andExpect(jsonPath("$.data.primary_family.role", is("owner")))
            .andExpect(jsonPath("$.data.current_pet.pet_name", is("Momo")))
            .andExpect(jsonPath("$.data.pet_count", is(1)));
    }

    @Test
    void shouldUpdateUserStatusFromAdminEndpoint() throws Exception {
        String authorizationHeader = authorizationHeader("13600000000");
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String userId = currentUserId(authorizationHeader);

        mockMvc.perform(patch("/api/v1/admin/users/%s/status".formatted(userId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "user-governance-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": 2,
                      "reason": "测试封禁用户"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user_id", is(userId)))
            .andExpect(jsonPath("$.data.status", is(2)));

        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(patch("/api/v1/admin/users/%s/status".formatted(userId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "user-governance-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": 1,
                      "reason": "测试恢复用户"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is(1)));

        Integer auditCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM audit_logs
            WHERE target_type = 'user'
              AND target_id = ?
              AND action IN ('user_disable', 'user_restore')
            """, Integer.class, userId);
        assertEquals(2, auditCount);
    }

    @Test
    void shouldListWelcomeNotificationAndMarkRead() throws Exception {
        String authorizationHeader = authorizationHeader();

        MvcResult notificationListResult = mockMvc.perform(get("/api/v1/notifications?notify_type=system&read_status=unread")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.unread_count", is(1)))
            .andExpect(jsonPath("$.data.system_unread_count", is(1)))
            .andExpect(jsonPath("$.data.items[?(@.biz_type == 'user_welcome')]").isNotEmpty())
            .andReturn();

        String notificationId = JsonPath.read(
            notificationListResult.getResponse().getContentAsString(),
            "$.data.items[0].notification_id"
        );

        mockMvc.perform(patch("/api/v1/notifications/%s/read".formatted(notificationId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.notification_id", is(notificationId)))
            .andExpect(jsonPath("$.data.read", is(true)))
            .andExpect(jsonPath("$.data.read_at").exists());

        mockMvc.perform(get("/api/v1/notifications?notify_type=system&read_status=unread")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.system_unread_count", is(0)))
            .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void shouldUploadMediaAssetAndReferenceItFromHealthAndDailyLog() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        MockMultipartFile healthAttachment = new MockMultipartFile(
            "file",
            "momo-checkup.png",
            MediaType.IMAGE_PNG_VALUE,
            "momo-health-image".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/media-assets")
                .file(healthAttachment)
                .param("biz_type", "health_report")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.biz_type", is("health_report")))
            .andExpect(jsonPath("$.data.media_type", is("image")))
            .andExpect(jsonPath("$.data.upload_status", is("uploaded")))
            .andExpect(jsonPath("$.data.access_url").exists())
            .andReturn();

        String healthAssetId = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.data.asset_id");

        mockMvc.perform(get("/api/v1/media-assets/%s".formatted(healthAssetId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.asset_id", is(healthAssetId)))
            .andExpect(jsonPath("$.data.file_hash").exists());

        mockMvc.perform(get("/api/v1/media-assets/%s/content".formatted(healthAssetId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/pets/%s/health-records".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "record_type": "examination",
                      "title": "影像检查",
                      "attachment_asset_ids": ["%s"],
                      "occurred_at": "%s",
                      "notes": "已上传检查图片"
                    }
                    """.formatted(healthAssetId, OffsetDateTime.now())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.attachment_asset_ids[0]", is(healthAssetId)))
            .andExpect(jsonPath("$.data.attachment_assets[0].asset_id", is(healthAssetId)))
            .andExpect(jsonPath("$.data.attachment_assets[0].access_url").exists());

        MockMultipartFile dailyImage = new MockMultipartFile(
            "file",
            "daily-momo.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "momo-daily-image".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
        MvcResult dailyUploadResult = mockMvc.perform(multipart("/api/v1/media-assets")
                .file(dailyImage)
                .param("biz_type", "daily_log")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.biz_type", is("daily_log")))
            .andReturn();
        String dailyAssetId = JsonPath.read(dailyUploadResult.getResponse().getContentAsString(), "$.data.asset_id");

        mockMvc.perform(post("/api/v1/pets/%s/daily-logs".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "今天精神很好，上传一张照片记录。",
                      "media_asset_ids": ["%s"],
                      "tags": ["照片"],
                      "visibility": "family",
                      "sync_to_community": false,
                      "happened_at": "%s"
                    }
                    """.formatted(dailyAssetId, OffsetDateTime.now())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.media_asset_ids[0]", is(dailyAssetId)))
            .andExpect(jsonPath("$.data.media_assets[0].asset_id", is(dailyAssetId)))
            .andExpect(jsonPath("$.data.media_assets[0].access_url").exists());
    }

    @Test
    void shouldAllowFamilyMemberPreviewReferencedHealthAndDailyMediaOnly() throws Exception {
        String ownerAuthorizationHeader = authorizationHeader();
        String petId = currentPetId(ownerAuthorizationHeader);
        joinFamilyMember(ownerAuthorizationHeader, "13900000000", "member");
        String memberAuthorizationHeader = authorizationHeader("13900000000");

        String healthAssetId = uploadMediaAsset(ownerAuthorizationHeader, "health_report", "shared-checkup.png", MediaType.IMAGE_PNG);
        MvcResult healthRecordResult = mockMvc.perform(post("/api/v1/pets/%s/health-records".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, ownerAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "record_type": "examination",
                      "title": "家庭可见检查附件",
                      "attachment_asset_ids": ["%s"],
                      "occurred_at": "%s",
                      "notes": "家庭成员需要可预览附件"
                    }
                    """.formatted(healthAssetId, OffsetDateTime.now())))
            .andExpect(status().isOk())
            .andReturn();
        String healthRecordId = JsonPath.read(
            healthRecordResult.getResponse().getContentAsString(),
            "$.data.health_record_id"
        );

        mockMvc.perform(get("/api/v1/pets/%s/health-records/%s".formatted(petId, healthRecordId))
                .header(HttpHeaders.AUTHORIZATION, memberAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.attachment_assets[0].asset_id", is(healthAssetId)))
            .andExpect(jsonPath("$.data.attachment_assets[0].access_url").exists());

        mockMvc.perform(get("/api/v1/media-assets/%s/content".formatted(healthAssetId))
                .header(HttpHeaders.AUTHORIZATION, memberAuthorizationHeader))
            .andExpect(status().isOk());

        String dailyAssetId = uploadMediaAsset(ownerAuthorizationHeader, "daily_log", "shared-daily.jpg", MediaType.IMAGE_JPEG);
        MvcResult dailyLogResult = mockMvc.perform(post("/api/v1/pets/%s/daily-logs".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, ownerAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "家庭成员可预览的日常图片。",
                      "media_asset_ids": ["%s"],
                      "tags": ["家庭"],
                      "visibility": "family",
                      "sync_to_community": false,
                      "happened_at": "%s"
                    }
                    """.formatted(dailyAssetId, OffsetDateTime.now())))
            .andExpect(status().isOk())
            .andReturn();
        String dailyLogId = JsonPath.read(
            dailyLogResult.getResponse().getContentAsString(),
            "$.data.daily_log_id"
        );

        mockMvc.perform(get("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, memberAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.media_assets[0].asset_id", is(dailyAssetId)))
            .andExpect(jsonPath("$.data.media_assets[0].access_url").exists());

        mockMvc.perform(get("/api/v1/media-assets/%s/content".formatted(dailyAssetId))
                .header(HttpHeaders.AUTHORIZATION, memberAuthorizationHeader))
            .andExpect(status().isOk());

        String unboundAssetId = uploadMediaAsset(ownerAuthorizationHeader, "daily_log", "unbound-daily.jpg", MediaType.IMAGE_JPEG);
        mockMvc.perform(get("/api/v1/media-assets/%s".formatted(unboundAssetId))
                .header(HttpHeaders.AUTHORIZATION, memberAuthorizationHeader))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code", is("MEDIA_ASSET_NOT_FOUND")));
    }

    @Test
    void shouldCreatePet() throws Exception {
        mockMvc.perform(post("/api/v1/pets")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_name": "Nana",
                      "pet_type": "cat",
                      "breed": "Ragdoll",
                      "gender": "female",
                      "birthday": "2024-03-08",
                      "adopt_date": "2024-06-18",
                      "neuter_status": "pending",
                      "avatar_asset_id": "asset_1001"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pet_name", is("Nana")))
            .andExpect(jsonPath("$.data.pet_type", is("cat")));
    }

    @Test
    void shouldUpdateCurrentPet() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = createPet(authorizationHeader, "Dodo");

        mockMvc.perform(patch("/api/v1/me/settings/current-pet")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_id": "%s"
                    }
                    """.formatted(petId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_pet_id", is(petId)));
    }

    @Test
    void shouldGetPetDetailAndUpdateExtendedPetFields() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);

        mockMvc.perform(patch("/api/v1/pets/%s".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_name": "Momo",
                      "pet_type": "cat",
                      "breed": "British Shorthair",
                      "gender": "female",
                      "birthday": "2023-05-20",
                      "adopt_date": "2023-08-01",
                      "neuter_status": "completed",
                      "weight_kg": "4.8",
                      "allergy_notes": "对海鲜较敏感",
                      "medical_history": "2025 年做过牙结石清理"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.weight_kg", is("4.8")))
            .andExpect(jsonPath("$.data.allergy_notes", is("对海鲜较敏感")))
            .andExpect(jsonPath("$.data.medical_history", is("2025 年做过牙结石清理")))
            .andExpect(jsonPath("$.data.status", is("active")));

        mockMvc.perform(get("/api/v1/pets/%s".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pet_id", is(petId)))
            .andExpect(jsonPath("$.data.weight_kg", is("4.8")))
            .andExpect(jsonPath("$.data.allergy_notes", is("对海鲜较敏感")))
            .andExpect(jsonPath("$.data.medical_history", is("2025 年做过牙结石清理")));
    }

    @Test
    void shouldQueryPetsFromAdminEndpoints() throws Exception {
        String authorizationHeader = authorizationHeader();
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String familyId = currentFamilyId(authorizationHeader);
        String petId = createPet(authorizationHeader, "后台宠物查询Dodo");

        mockMvc.perform(get(
                "/api/v1/admin/pets?keyword=后台宠物&pet_name=后台宠物查询&pet_type=dog&status=active&owner_mobile=13800000000&family_id=%s"
                    .formatted(familyId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.pet.pet_id == '%s')].pet.pet_name"
                .formatted(petId), is(List.of("后台宠物查询Dodo"))))
            .andExpect(jsonPath("$.data[?(@.pet.pet_id == '%s')].owner.mobile"
                .formatted(petId), is(List.of("13800000000"))))
            .andExpect(jsonPath("$.data[?(@.pet.pet_id == '%s')].family.family_id"
                .formatted(petId), is(List.of(familyId))));

        mockMvc.perform(get("/api/v1/admin/pets/%s".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pet.pet_id", is(petId)))
            .andExpect(jsonPath("$.data.pet.pet_type", is("dog")))
            .andExpect(jsonPath("$.data.owner.nickname", is("Momo")))
            .andExpect(jsonPath("$.data.family.family_name", is("Momo的家庭")))
            .andExpect(jsonPath("$.data.family.member_count", is(1)));
    }

    @Test
    void shouldRepairPetFromAdminEndpoint() throws Exception {
        String authorizationHeader = authorizationHeader("13300000000");
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String userId = currentUserId(authorizationHeader);
        String petId = currentPetId(authorizationHeader);
        String familyId = currentFamilyId(authorizationHeader);

        jdbcTemplate.update(
            "DELETE FROM family_members WHERE family_id = ? AND user_id = ?",
            Long.valueOf(familyId),
            Long.valueOf(userId)
        );
        mockMvc.perform(post("/api/v1/admin/pets/%s/repair".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "pet-repair-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "repair_type": "owner_member_missing",
                      "reason": "测试修复主人成员关系"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pet.pet_id", is(petId)))
            .andExpect(jsonPath("$.data.family.member_count", is(1)));

        jdbcTemplate.update("UPDATE pets SET family_id = NULL WHERE id = ?", Long.valueOf(petId));
        mockMvc.perform(post("/api/v1/admin/pets/%s/repair".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "pet-repair-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "repair_type": "family_missing",
                      "reason": "测试修复家庭缺失"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pet.pet_id", is(petId)))
            .andExpect(jsonPath("$.data.family.family_id").exists())
            .andExpect(jsonPath("$.data.family.member_count", is(1)));

        mockMvc.perform(post("/api/v1/admin/pets/%s/repair".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "pet-repair-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "repair_type": "current_pet_context",
                      "reason": "测试重建当前宠物上下文"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pet.pet_id", is(petId)));

        Integer auditCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM audit_logs
            WHERE target_type = 'pet'
              AND target_id = ?
              AND action IN (
                'pet_owner_member_missing_repair',
                'pet_family_missing_repair',
                'pet_current_pet_context_repair'
              )
            """, Integer.class, petId);
        assertEquals(3, auditCount);
    }

    @Test
    void shouldArchiveCurrentPetAndRebuildCurrentPetContext() throws Exception {
        String authorizationHeader = authorizationHeader();
        String fallbackPetId = currentPetId(authorizationHeader);
        String archivedPetId = createPet(authorizationHeader, "Dodo");

        mockMvc.perform(patch("/api/v1/me/settings/current-pet")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_id": "%s"
                    }
                    """.formatted(archivedPetId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_pet_id", is(archivedPetId)));

        mockMvc.perform(patch("/api/v1/pets/%s/archive".formatted(archivedPetId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "archive_status": "memorial"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_pet_id", is(fallbackPetId)))
            .andExpect(jsonPath("$.data.current_pet.pet_name", is("Momo")));

        mockMvc.perform(get("/api/v1/pets")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.pet_id == '%s')]".formatted(archivedPetId)).isEmpty());
    }

    @Test
    void shouldDeleteLastPetAndReturnNoCurrentPetState() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);

        mockMvc.perform(delete("/api/v1/pets/%s".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_pet_id", nullValue()))
            .andExpect(jsonPath("$.data.current_pet", nullValue()));

        mockMvc.perform(get("/api/v1/pets")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldRejectDeletePetForMemberRole() throws Exception {
        String ownerAuthorizationHeader = authorizationHeader();
        String memberAuthorizationHeader = authorizationHeader("13700000000");
        String memberUserId = currentUserId(memberAuthorizationHeader);
        String familyId = currentFamilyId(ownerAuthorizationHeader);
        String petId = currentPetId(ownerAuthorizationHeader);

        familyPersistenceMapper.insertFamilyMember(Long.valueOf(familyId), Long.valueOf(memberUserId), "member");

        mockMvc.perform(delete("/api/v1/pets/%s".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, memberAuthorizationHeader))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code", is("PET_PERMISSION_DENIED")));
    }

    @Test
    void shouldReturnFamilyDetail() throws Exception {
        mockMvc.perform(get("/api/v1/family")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.family_name", is("Momo的家庭")))
            .andExpect(jsonPath("$.data.current_user_role", is("owner")))
            .andExpect(jsonPath("$.data.members[0].role", is("owner")))
            .andExpect(jsonPath("$.data.shared_pets[0].pet_name", is("Momo")));
    }

    @Test
    void shouldReturnExistingFamilyWhenInitializeFamilyCalled() throws Exception {
        mockMvc.perform(post("/api/v1/family")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.family_name", is("Momo的家庭")))
            .andExpect(jsonPath("$.data.current_user_role", is("owner")));
    }

    @Test
    void shouldCreateFamilyInvitation() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);

        mockMvc.perform(post("/api/v1/family/invitations")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "invitee_mobile": "13900000000",
                      "role": "member",
                      "shared_pet_ids": ["%s"]
                    }
                    """.formatted(petId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role", is("member")))
            .andExpect(jsonPath("$.data.shared_pet_ids[0]", is(petId)))
            .andExpect(jsonPath("$.data.status", is("pending")))
            .andExpect(jsonPath("$.data.invite_code").exists());
    }

    @Test
    void shouldPreviewAndAcceptFamilyInvitation() throws Exception {
        String ownerAuthorizationHeader = authorizationHeader();
        String inviteeAuthorizationHeader = authorizationHeader("13900000000");
        String petId = currentPetId(ownerAuthorizationHeader);
        String inviteCode = createFamilyInvitation(ownerAuthorizationHeader, "13900000000", petId);

        mockMvc.perform(get("/api/v1/family/invitations/%s".formatted(inviteCode))
                .header(HttpHeaders.AUTHORIZATION, inviteeAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.family_name", is("Momo的家庭")))
            .andExpect(jsonPath("$.data.role", is("member")))
            .andExpect(jsonPath("$.data.shared_pets[0].pet_id", is(petId)));

        mockMvc.perform(post("/api/v1/family/invitations/%s/accept".formatted(inviteCode))
                .header(HttpHeaders.AUTHORIZATION, inviteeAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.family_name", is("Momo的家庭")))
            .andExpect(jsonPath("$.data.current_user_role", is("member")))
            .andExpect(jsonPath("$.data.shared_pets[0].pet_id", is(petId)));

        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, inviteeAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.family_summary.family_name", is("Momo的家庭")))
            .andExpect(jsonPath("$.data.current_pet_id", is(petId)));
    }

    @Test
    void shouldRejectFamilyInvitation() throws Exception {
        String ownerAuthorizationHeader = authorizationHeader();
        String inviteeAuthorizationHeader = authorizationHeader("13700000000");
        String petId = currentPetId(ownerAuthorizationHeader);
        String inviteCode = createFamilyInvitation(ownerAuthorizationHeader, "13700000000", petId);

        mockMvc.perform(post("/api/v1/family/invitations/%s/reject".formatted(inviteCode))
                .header(HttpHeaders.AUTHORIZATION, inviteeAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("rejected")));

        mockMvc.perform(get("/api/v1/family")
                .header(HttpHeaders.AUTHORIZATION, ownerAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.member_count", is(1)));
    }

    @Test
    void shouldUpdateFamilyMemberRole() throws Exception {
        String authorizationHeader = authorizationHeader();
        String memberId = joinFamilyMember(authorizationHeader, "13900000000", "member");

        mockMvc.perform(patch("/api/v1/family/members/%s/role".formatted(memberId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "role": "admin"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.member_id", is(memberId)))
            .andExpect(jsonPath("$.data.role", is("admin")));
    }

    @Test
    void shouldRemoveFamilyMember() throws Exception {
        String authorizationHeader = authorizationHeader();
        String memberId = joinFamilyMember(authorizationHeader, "13700000000", "member");

        mockMvc.perform(delete("/api/v1/family/members/%s".formatted(memberId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(get("/api/v1/family")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.member_count", is(1)));
    }

    @Test
    void shouldQueryFamiliesFromAdminEndpoints() throws Exception {
        String ownerAuthorizationHeader = authorizationHeader();
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String familyId = currentFamilyId(ownerAuthorizationHeader);
        String petId = currentPetId(ownerAuthorizationHeader);
        joinFamilyMember(ownerAuthorizationHeader, "13900000000", "member");

        mockMvc.perform(get(
                "/api/v1/admin/families?keyword=Momo&family_name=Momo&member_mobile=13900000000&member_role=member&status=1")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.family_id == '%s')].family_name"
                .formatted(familyId), is(List.of("Momo的家庭"))))
            .andExpect(jsonPath("$.data[?(@.family_id == '%s')].owner.mobile"
                .formatted(familyId), is(List.of("13800000000"))))
            .andExpect(jsonPath("$.data[?(@.family_id == '%s')].member_count"
                .formatted(familyId), is(List.of(2))))
            .andExpect(jsonPath("$.data[?(@.family_id == '%s')].pets[0].pet_id"
                .formatted(familyId), is(List.of(petId))));

        mockMvc.perform(get("/api/v1/admin/families/%s".formatted(familyId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.family_id", is(familyId)))
            .andExpect(jsonPath("$.data.family_name", is("Momo的家庭")))
            .andExpect(jsonPath("$.data.members[?(@.mobile == '13900000000')].role", is(List.of("member"))))
            .andExpect(jsonPath("$.data.pets[?(@.pet_id == '%s')].owner_mobile".formatted(petId),
                is(List.of("13800000000"))));
    }

    @Test
    void shouldUpdateFamilyStatusAndRepairOwnerMemberFromAdminEndpoint() throws Exception {
        String ownerAuthorizationHeader = authorizationHeader("13500000000");
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String userId = currentUserId(ownerAuthorizationHeader);
        String familyId = currentFamilyId(ownerAuthorizationHeader);

        mockMvc.perform(patch("/api/v1/admin/families/%s/status".formatted(familyId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "family-governance-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": 2,
                      "reason": "测试停用家庭"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.family_id", is(familyId)))
            .andExpect(jsonPath("$.data.status", is(2)));

        mockMvc.perform(patch("/api/v1/admin/families/%s/status".formatted(familyId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "family-governance-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": 1,
                      "reason": "测试恢复家庭"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is(1)));

        jdbcTemplate.update(
            "UPDATE family_members SET role = 'member' WHERE family_id = ? AND user_id = ?",
            Long.valueOf(familyId),
            Long.valueOf(userId)
        );
        mockMvc.perform(post("/api/v1/admin/families/%s/owner-member-repair".formatted(familyId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "family-governance-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reason": "测试修复家庭 owner 成员关系"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.members[?(@.user_id == '%s')].role".formatted(userId), is(List.of("owner"))));

        Integer auditCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM audit_logs
            WHERE target_type = 'family'
              AND target_id = ?
              AND action IN ('family_disable', 'family_restore', 'family_owner_member_repair')
            """, Integer.class, familyId);
        assertEquals(3, auditCount);
    }

    @Test
    void shouldCreateHealthRecord() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        OffsetDateTime nextReminderAt = OffsetDateTime.now().plusMonths(11).withSecond(0).withNano(0);
        String firstAttachmentId = uploadMediaAsset(authorizationHeader, "health_report", "vaccine-1.png", MediaType.IMAGE_PNG);
        String secondAttachmentId = uploadMediaAsset(authorizationHeader, "health_report", "vaccine-2.png", MediaType.IMAGE_PNG);

        mockMvc.perform(post("/api/v1/pets/%s/health-records".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "record_type": "vaccine",
                      "title": "狂犬疫苗",
                      "hospital_name": "安心宠物医院",
                      "doctor_name": "张医生",
                      "result_summary": "接种完成，无明显异常",
                      "attachment_asset_ids": ["%s", "%s"],
                      "next_reminder_at": "%s",
                      "next_reminder_title": "下一次狂犬疫苗",
                      "occurred_at": "2026-04-17T08:30:00+08:00",
                      "notes": "留观 20 分钟后离院"
                    }
                    """.formatted(firstAttachmentId, secondAttachmentId, nextReminderAt)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.record_type", is("vaccine")))
            .andExpect(jsonPath("$.data.title", is("狂犬疫苗")))
            .andExpect(jsonPath("$.data.hospital_name", is("安心宠物医院")))
            .andExpect(jsonPath("$.data.doctor_name", is("张医生")))
            .andExpect(jsonPath("$.data.result_summary", is("接种完成，无明显异常")))
            .andExpect(jsonPath("$.data.attachment_asset_ids[0]", is(firstAttachmentId)))
            .andExpect(jsonPath("$.data.next_reminder_id").exists())
            .andExpect(jsonPath("$.data.next_reminder_status", is("pending")));
    }

    @Test
    void shouldGetUpdateAndDeleteHealthRecord() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String healthRecordId = createHealthRecord(authorizationHeader, petId, "体重复查");
        OffsetDateTime nextReminderAt = OffsetDateTime.now().plusYears(1).withSecond(0).withNano(0);
        String attachmentId = uploadMediaAsset(authorizationHeader, "health_report", "checkup.pdf", MediaType.APPLICATION_PDF);

        mockMvc.perform(get("/api/v1/pets/%s/health-records/%s".formatted(petId, healthRecordId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.health_record_id", is(healthRecordId)))
            .andExpect(jsonPath("$.data.title", is("体重复查")));

        mockMvc.perform(patch("/api/v1/pets/%s/health-records/%s".formatted(petId, healthRecordId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "record_type": "examination",
                      "title": "年度体检复查",
                      "value": "4.8",
                      "unit": "kg",
                      "hospital_name": "安心宠物医院",
                      "doctor_name": "李医生",
                      "result_summary": "精神状态稳定",
                      "attachment_asset_ids": ["%s"],
                      "next_reminder_at": "%s",
                      "next_reminder_title": "下一次年度体检",
                      "occurred_at": "2026-04-19T10:30:00+08:00",
                      "notes": "精神状态稳定"
                    }
                    """.formatted(attachmentId, nextReminderAt)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title", is("年度体检复查")))
            .andExpect(jsonPath("$.data.value", nullValue()))
            .andExpect(jsonPath("$.data.unit", nullValue()))
            .andExpect(jsonPath("$.data.hospital_name", is("安心宠物医院")))
            .andExpect(jsonPath("$.data.doctor_name", is("李医生")))
            .andExpect(jsonPath("$.data.result_summary", is("精神状态稳定")))
            .andExpect(jsonPath("$.data.attachment_asset_ids[0]", is(attachmentId)))
            .andExpect(jsonPath("$.data.next_reminder_id").exists());

        mockMvc.perform(get("/api/v1/pets/%s/reminders".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.title == '下一次年度体检')].status", is(List.of("pending"))));

        mockMvc.perform(delete("/api/v1/pets/%s/health-records/%s".formatted(petId, healthRecordId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(get("/api/v1/pets/%s/health-records".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.health_record_id == '%s')]".formatted(healthRecordId)).isEmpty());

        mockMvc.perform(get("/api/v1/pets/%s/reminders".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.title == '下一次年度体检')]").isEmpty());
    }

    @Test
    void shouldCompleteReminder() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String reminderId = createReminder(authorizationHeader, petId);

        mockMvc.perform(patch("/api/v1/pets/%s/reminders/%s/complete".formatted(petId, reminderId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("completed")))
            .andExpect(jsonPath("$.data.completed_at").exists());

        mockMvc.perform(get("/api/v1/notifications?notify_type=reminder&read_status=unread")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reminder_unread_count", is(1)))
            .andExpect(jsonPath("$.data.items[0].biz_type", is("reminder_completed")))
            .andExpect(jsonPath("$.data.items[0].biz_id", is(reminderId)))
            .andExpect(jsonPath("$.data.items[0].title", is("提醒已完成")));
    }

    @Test
    void shouldSkipReminder() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String reminderId = createReminder(authorizationHeader, petId);

        mockMvc.perform(patch("/api/v1/pets/%s/reminders/%s/skip".formatted(petId, reminderId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("skipped")))
            .andExpect(jsonPath("$.data.completed_at").exists());

        mockMvc.perform(get("/api/v1/notifications?notify_type=reminder&read_status=unread")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].biz_type", is("reminder_skipped")))
            .andExpect(jsonPath("$.data.items[0].biz_id", is(reminderId)))
            .andExpect(jsonPath("$.data.items[0].title", is("提醒已跳过")));
    }

    @Test
    void shouldCreateNextReminderAfterCompletingCycleReminder() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        OffsetDateTime originalDueAt = OffsetDateTime.now().plusDays(7).withSecond(0).withNano(0);
        CreatedReminderFixture createdReminder = createCycleReminder(
            authorizationHeader,
            petId,
            "驱虫周期提醒",
            1,
            "month",
            originalDueAt
        );

        mockMvc.perform(patch("/api/v1/pets/%s/reminders/%s/complete".formatted(petId, createdReminder.reminderId()))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("completed")))
            .andExpect(jsonPath("$.data.reminder_mode", is("cycle")))
            .andExpect(jsonPath("$.data.cycle_value", is(1)))
            .andExpect(jsonPath("$.data.cycle_unit", is("month")));

        MvcResult reminderListResult = mockMvc.perform(get("/api/v1/pets/%s/reminders".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(2)))
            .andExpect(jsonPath("$.data[0].status", is("pending")))
            .andExpect(jsonPath("$.data[0].title", is("驱虫周期提醒")))
            .andExpect(jsonPath("$.data[0].reminder_mode", is("cycle")))
            .andExpect(jsonPath("$.data[0].cycle_value", is(1)))
            .andExpect(jsonPath("$.data[0].cycle_unit", is("month")))
            .andExpect(jsonPath("$.data[1].status", is("completed")))
            .andReturn();

        String nextDueAtValue = JsonPath.read(reminderListResult.getResponse().getContentAsString(), "$.data[0].due_at");
        assertEquals(originalDueAt.plusMonths(1).toInstant(), OffsetDateTime.parse(nextDueAtValue).toInstant());
    }

    @Test
    void shouldCreateNextReminderAfterSkippingCycleReminder() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        OffsetDateTime originalDueAt = OffsetDateTime.now().plusDays(5).withSecond(0).withNano(0);
        CreatedReminderFixture createdReminder = createCycleReminder(
            authorizationHeader,
            petId,
            "复查周期提醒",
            2,
            "week",
            originalDueAt
        );

        mockMvc.perform(patch("/api/v1/pets/%s/reminders/%s/skip".formatted(petId, createdReminder.reminderId()))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("skipped")))
            .andExpect(jsonPath("$.data.reminder_mode", is("cycle")))
            .andExpect(jsonPath("$.data.cycle_value", is(2)))
            .andExpect(jsonPath("$.data.cycle_unit", is("week")));

        MvcResult reminderListResult = mockMvc.perform(get("/api/v1/pets/%s/reminders".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(2)))
            .andExpect(jsonPath("$.data[0].status", is("pending")))
            .andExpect(jsonPath("$.data[0].title", is("复查周期提醒")))
            .andExpect(jsonPath("$.data[0].reminder_mode", is("cycle")))
            .andExpect(jsonPath("$.data[0].cycle_value", is(2)))
            .andExpect(jsonPath("$.data[0].cycle_unit", is("week")))
            .andExpect(jsonPath("$.data[1].status", is("skipped")))
            .andReturn();

        String nextDueAtValue = JsonPath.read(reminderListResult.getResponse().getContentAsString(), "$.data[0].due_at");
        assertEquals(originalDueAt.plusWeeks(2).toInstant(), OffsetDateTime.parse(nextDueAtValue).toInstant());
    }

    @Test
    void shouldQueryRemindersFromAdminEndpoints() throws Exception {
        String authorizationHeader = authorizationHeader();
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String userId = currentUserId(authorizationHeader);
        String familyId = currentFamilyId(authorizationHeader);
        String petId = currentPetId(authorizationHeader);
        OffsetDateTime completedDueAt = OffsetDateTime.now().plusDays(3).withSecond(0).withNano(0);
        String completedReminderId = createReminderAt(
            authorizationHeader,
            petId,
            "后台提醒查询已完成",
            completedDueAt
        );

        mockMvc.perform(patch("/api/v1/pets/%s/reminders/%s/complete".formatted(petId, completedReminderId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk());

        MvcResult healthRecordResult = mockMvc.perform(post("/api/v1/pets/%s/health-records".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "record_type": "examination",
                      "title": "后台提醒来源体检",
                      "next_reminder_at": "%s",
                      "next_reminder_title": "后台提醒来源复查",
                      "occurred_at": "2026-05-18T10:00:00+08:00",
                      "notes": "后台提醒查询来源记录"
                    }
                    """.formatted(OffsetDateTime.now().plusMonths(2).withSecond(0).withNano(0))))
            .andExpect(status().isOk())
            .andReturn();
        String sourceHealthRecordId = JsonPath.read(
            healthRecordResult.getResponse().getContentAsString(),
            "$.data.health_record_id"
        );
        String sourceReminderId = JsonPath.read(
            healthRecordResult.getResponse().getContentAsString(),
            "$.data.next_reminder_id"
        );

        mockMvc.perform(get("/api/v1/admin/reminders")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .param("keyword", "后台提醒查询")
                .param("status", "completed")
                .param("reminder_type", "deworming")
                .param("reminder_mode", "single")
                .param("pet_id", petId)
                .param("family_id", familyId)
                .param("owner_user_id", userId)
                .param("handler_user_id", userId)
                .param("due_from", completedDueAt.minusHours(1).toString())
                .param("due_to", completedDueAt.plusHours(1).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.reminder.reminder_id == '%s')].reminder.status"
                .formatted(completedReminderId), is(List.of("completed"))))
            .andExpect(jsonPath("$.data[?(@.reminder.reminder_id == '%s')].pet.pet_id"
                .formatted(completedReminderId), is(List.of(petId))))
            .andExpect(jsonPath("$.data[?(@.reminder.reminder_id == '%s')].handler.user_id"
                .formatted(completedReminderId), is(List.of(userId))));

        mockMvc.perform(get("/api/v1/admin/reminders/%s".formatted(completedReminderId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reminder.reminder_id", is(completedReminderId)))
            .andExpect(jsonPath("$.data.reminder.completed_at").exists())
            .andExpect(jsonPath("$.data.pet.family_id", is(familyId)))
            .andExpect(jsonPath("$.data.handler.mobile", is("13800000000")))
            .andExpect(jsonPath("$.data.source_record", nullValue()));

        mockMvc.perform(get("/api/v1/admin/reminders")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .param("status", "pending")
                .param("source_record_id", sourceHealthRecordId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.reminder.reminder_id == '%s')].source_record.source_record_id"
                .formatted(sourceReminderId), is(List.of(sourceHealthRecordId))))
            .andExpect(jsonPath("$.data[?(@.reminder.reminder_id == '%s')].source_record.status"
                .formatted(sourceReminderId), is(List.of("active"))));
    }

    @Test
    void shouldRejectAdminReminderQueryWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reminders"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void shouldManageReminderTemplatesFromAdminEndpoints() throws Exception {
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String templateName = "后台提醒模板-" + System.nanoTime();

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/reminder-templates")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "reminder-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "template_name": "%s",
                      "reminder_type": "vaccine",
                      "default_reminder_mode": "cycle",
                      "default_advance_value": 7,
                      "default_advance_unit": "day",
                      "default_cycle_value": 12,
                      "default_cycle_unit": "month",
                      "applicable_pet_type": "dog",
                      "enabled": true,
                      "sort_order": 3
                    }
                    """.formatted(templateName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.template_name", is(templateName)))
            .andExpect(jsonPath("$.data.reminder_type", is("vaccine")))
            .andExpect(jsonPath("$.data.default_reminder_mode", is("cycle")))
            .andExpect(jsonPath("$.data.default_advance_value", is(7)))
            .andExpect(jsonPath("$.data.default_cycle_value", is(12)))
            .andExpect(jsonPath("$.data.applicable_pet_type", is("dog")))
            .andExpect(jsonPath("$.data.enabled", is(true)))
            .andReturn();
        String templateId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.template_id");

        mockMvc.perform(get("/api/v1/admin/reminder-templates")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .param("keyword", templateName)
                .param("reminder_type", "vaccine")
                .param("default_reminder_mode", "cycle")
                .param("applicable_pet_type", "dog")
                .param("enabled", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.template_id == '%s')].template_name"
                .formatted(templateId), is(List.of(templateName))))
            .andExpect(jsonPath("$.data[?(@.template_id == '%s')].default_cycle_unit"
                .formatted(templateId), is(List.of("month"))));

        mockMvc.perform(get("/api/v1/admin/reminder-templates/%s".formatted(templateId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.template_id", is(templateId)))
            .andExpect(jsonPath("$.data.created_at").exists())
            .andExpect(jsonPath("$.data.updated_at").exists());

        String updatedTemplateName = templateName + "-已更新";
        mockMvc.perform(patch("/api/v1/admin/reminder-templates/%s".formatted(templateId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "reminder-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "template_name": "%s",
                      "reminder_type": "examination",
                      "default_reminder_mode": "single",
                      "default_advance_value": 1,
                      "default_advance_unit": "week",
                      "applicable_pet_type": "cat",
                      "enabled": true,
                      "sort_order": 8
                    }
                    """.formatted(updatedTemplateName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.template_name", is(updatedTemplateName)))
            .andExpect(jsonPath("$.data.reminder_type", is("examination")))
            .andExpect(jsonPath("$.data.default_reminder_mode", is("single")))
            .andExpect(jsonPath("$.data.default_cycle_value", nullValue()))
            .andExpect(jsonPath("$.data.default_cycle_unit", nullValue()))
            .andExpect(jsonPath("$.data.applicable_pet_type", is("cat")))
            .andExpect(jsonPath("$.data.sort_order", is(8)));

        mockMvc.perform(patch("/api/v1/admin/reminder-templates/%s/status".formatted(templateId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "reminder-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "enabled": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.template_id", is(templateId)))
            .andExpect(jsonPath("$.data.enabled", is(false)));

        mockMvc.perform(get("/api/v1/admin/reminder-templates")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .param("keyword", updatedTemplateName)
                .param("reminder_type", "examination")
                .param("default_reminder_mode", "single")
                .param("applicable_pet_type", "cat")
                .param("enabled", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.template_id == '%s')].enabled"
                .formatted(templateId), is(List.of(false))));
    }

    @Test
    void shouldRejectInvalidReminderTemplateCycleConfig() throws Exception {
        mockMvc.perform(post("/api/v1/admin/reminder-templates")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "template_name": "错误模板",
                      "reminder_type": "custom",
                      "default_reminder_mode": "single",
                      "default_advance_value": 0,
                      "default_advance_unit": "day",
                      "default_cycle_value": 1,
                      "default_cycle_unit": "month",
                      "applicable_pet_type": "all",
                      "enabled": true,
                      "sort_order": 0
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    @Test
    void shouldRejectAdminReminderTemplateWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reminder-templates"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void shouldManageMessageTemplatesFromAdminEndpoints() throws Exception {
        String adminAuthorizationHeader = adminAuthorizationHeader();
        long suffix = Math.floorMod(System.nanoTime(), 1_000_000_000L);
        String adminOperator = "msg-template-admin-" + suffix;
        String templateCode = "notice_template_" + suffix;

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/message-templates")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "template_code": "%s",
                      "channel_type": "inbox",
                      "title_template": "模板标题 ${pet_name}",
                      "content_template": "模板内容 ${pet_name}",
                      "enabled": true
                    }
                    """.formatted(templateCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.template_code", is(templateCode)))
            .andExpect(jsonPath("$.data.channel_type", is("inbox")))
            .andExpect(jsonPath("$.data.enabled", is(true)))
            .andReturn();
        String templateId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.template_id");

        mockMvc.perform(post("/api/v1/admin/message-templates")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "template_code": "%s",
                      "channel_type": "inbox",
                      "title_template": "重复模板",
                      "content_template": "重复模板内容",
                      "enabled": true
                    }
                    """.formatted(templateCode)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("消息模板编码和渠道已存在")));

        MvcResult anotherCreateResult = mockMvc.perform(post("/api/v1/admin/message-templates")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "template_code": "%s_alt",
                      "channel_type": "inbox",
                      "title_template": "另一个模板",
                      "content_template": "另一个模板内容",
                      "enabled": true
                    }
                    """.formatted(templateCode)))
            .andExpect(status().isOk())
            .andReturn();
        String anotherTemplateId = JsonPath.read(anotherCreateResult.getResponse().getContentAsString(), "$.data.template_id");

        mockMvc.perform(patch("/api/v1/admin/message-templates/%s".formatted(anotherTemplateId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "template_code": "%s",
                      "channel_type": "inbox",
                      "title_template": "冲突模板",
                      "content_template": "冲突模板内容",
                      "enabled": true
                    }
                    """.formatted(templateCode)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("消息模板编码和渠道已存在")));

        mockMvc.perform(get("/api/v1/admin/message-templates")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .param("template_code", templateCode)
                .param("channel_type", "inbox")
                .param("enabled", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.template_id == '%s')].title_template"
                .formatted(templateId), is(List.of("模板标题 ${pet_name}"))));

        mockMvc.perform(get("/api/v1/admin/message-templates/%s".formatted(templateId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.template_id", is(templateId)))
            .andExpect(jsonPath("$.data.created_at").exists())
            .andExpect(jsonPath("$.data.updated_at").exists());

        mockMvc.perform(patch("/api/v1/admin/message-templates/%s".formatted(templateId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "template_code": "%s",
                      "channel_type": "push",
                      "title_template": "更新后的模板标题",
                      "content_template": "更新后的模板内容",
                      "enabled": true
                    }
                    """.formatted(templateCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.channel_type", is("push")))
            .andExpect(jsonPath("$.data.title_template", is("更新后的模板标题")));

        mockMvc.perform(patch("/api/v1/admin/message-templates/%s/status".formatted(templateId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "enabled": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enabled", is(false)));

        mockMvc.perform(get("/api/v1/admin/notification/audit-logs?operator_id=%s&target_type=message_template"
                .formatted(adminOperator))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.action == 'message_template_status_update')].target_id",
                is(List.of(templateId))));
    }

    @Test
    void shouldManageNotificationChannelsFromAdminEndpoints() throws Exception {
        String adminAuthorizationHeader = adminAuthorizationHeader();
        long suffix = Math.floorMod(System.nanoTime(), 1_000_000_000L);
        String adminOperator = "msg-channel-admin-" + suffix;
        String providerCode = "sms_provider_" + suffix;

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/notification-channels")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "channel_type": "sms",
                      "provider_code": "%s",
                      "provider_name": "短信供应商",
                      "enabled": false,
                      "config_status": "draft",
                      "remark": "仅配置占位，不接真实短信"
                    }
                    """.formatted(providerCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.channel_type", is("sms")))
            .andExpect(jsonPath("$.data.provider_code", is(providerCode)))
            .andExpect(jsonPath("$.data.enabled", is(false)))
            .andExpect(jsonPath("$.data.config_status", is("draft")))
            .andReturn();
        String channelConfigId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.channel_config_id");

        mockMvc.perform(post("/api/v1/admin/notification-channels")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "channel_type": "sms",
                      "provider_code": "%s",
                      "provider_name": "重复短信供应商",
                      "enabled": false,
                      "config_status": "draft"
                    }
                    """.formatted(providerCode)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("通知渠道和供应商编码已存在")));

        mockMvc.perform(get("/api/v1/admin/notification-channels")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .param("channel_type", "sms")
                .param("provider_code", providerCode)
                .param("enabled", "false")
                .param("config_status", "draft"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.channel_config_id == '%s')].provider_name"
                .formatted(channelConfigId), is(List.of("短信供应商"))));

        mockMvc.perform(get("/api/v1/admin/notification-channels/%s".formatted(channelConfigId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.channel_config_id", is(channelConfigId)))
            .andExpect(jsonPath("$.data.created_at").exists())
            .andExpect(jsonPath("$.data.updated_at").exists());

        mockMvc.perform(patch("/api/v1/admin/notification-channels/%s".formatted(channelConfigId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "channel_type": "sms",
                      "provider_code": "%s",
                      "provider_name": "短信供应商 Pro",
                      "enabled": true,
                      "config_status": "ready",
                      "remark": "仍不接真实短信发送"
                    }
                    """.formatted(providerCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.provider_name", is("短信供应商 Pro")))
            .andExpect(jsonPath("$.data.enabled", is(true)))
            .andExpect(jsonPath("$.data.config_status", is("ready")));

        mockMvc.perform(patch("/api/v1/admin/notification-channels/%s/status".formatted(channelConfigId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "enabled": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enabled", is(false)))
            .andExpect(jsonPath("$.data.config_status", is("disabled")));

        mockMvc.perform(get("/api/v1/admin/notification/audit-logs?operator_id=%s&target_type=notification_channel"
                .formatted(adminOperator))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.action == 'notification_channel_status_update')].target_id",
                is(List.of(channelConfigId))));
    }

    @Test
    void shouldRenderWelcomeNotificationFromMessageTemplate() throws Exception {
        String adminAuthorizationHeader = adminAuthorizationHeader();
        jdbcTemplate.update("""
            DELETE FROM message_templates
            WHERE template_code = 'user_welcome'
              AND channel_type = 'inbox'
            """);

        mockMvc.perform(post("/api/v1/admin/message-templates")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "template_code": "user_welcome",
                      "channel_type": "inbox",
                      "title_template": "模板欢迎标题",
                      "content_template": "模板欢迎内容",
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk());

        String authorizationHeader = authorizationHeader(uniqueMobile("137"));
        mockMvc.perform(get("/api/v1/notifications?notify_type=system&read_status=unread")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[?(@.biz_type == 'user_welcome')].title",
                is(List.of("模板欢迎标题"))))
            .andExpect(jsonPath("$.data.items[?(@.biz_type == 'user_welcome')].content",
                is(List.of("模板欢迎内容"))));
    }

    @Test
    void shouldRejectAdminNotificationConfigWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/message-templates"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/v1/admin/notification-channels"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void shouldListEnabledReminderTemplatesForCurrentPet() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String petType = JsonPath.read(
            mockMvc.perform(get("/api/v1/me")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.data.current_pet.pet_type"
        );
        String nonMatchingPetType = "dog".equals(petType) ? "cat" : "dog";
        String templateNamePrefix = "用户端提醒模板-" + System.nanoTime();
        String allPetTemplateName = templateNamePrefix + "-全宠";
        String matchingTemplateName = templateNamePrefix + "-匹配";
        String nonMatchingTemplateName = templateNamePrefix + "-不匹配";
        String disabledTemplateName = templateNamePrefix + "-停用";

        jdbcTemplate.update("""
            INSERT INTO reminder_templates (
              template_name, reminder_type, default_reminder_mode,
              default_advance_value, default_advance_unit,
              default_cycle_value, default_cycle_unit,
              applicable_pet_type, enabled, sort_order,
              created_at, updated_at
            ) VALUES
              (?, 'vaccine', 'cycle', 7, 'day', 12, 'month', 'all', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
              (?, 'deworming', 'cycle', 3, 'day', 1, 'month', ?, 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
              (?, 'examination', 'single', 1, 'week', NULL, NULL, ?, 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
              (?, 'custom', 'single', 0, 'day', NULL, NULL, ?, 0, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            allPetTemplateName,
            matchingTemplateName,
            petType,
            nonMatchingTemplateName,
            nonMatchingPetType,
            disabledTemplateName,
            petType);

        mockMvc.perform(get("/api/v1/pets/%s/reminder-templates".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.template_name == '%s')].applicable_pet_type"
                .formatted(allPetTemplateName), is(List.of("all"))))
            .andExpect(jsonPath("$.data[?(@.template_name == '%s')].applicable_pet_type"
                .formatted(matchingTemplateName), is(List.of(petType))))
            .andExpect(jsonPath("$.data[?(@.template_name == '%s')]"
                .formatted(nonMatchingTemplateName)).isEmpty())
            .andExpect(jsonPath("$.data[?(@.template_name == '%s')]"
                .formatted(disabledTemplateName)).isEmpty());
    }

    @Test
    void shouldRejectUserReminderTemplateWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/pets/10001/reminder-templates"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void shouldCreateDailyLog() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);

        mockMvc.perform(post("/api/v1/pets/%s/daily-logs".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "今天第一次主动跳上窗台晒太阳。",
                      "tags": ["晒太阳", "成长"],
                      "visibility": "public",
                      "sync_to_community": false,
                      "happened_at": "2026-04-17T10:00:00+08:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.visibility", is("public")))
            .andExpect(jsonPath("$.data.sync_to_community", is(false)))
            .andExpect(jsonPath("$.data.tags[0]", is("晒太阳")));
    }

    @Test
    void shouldSyncDailyLogToCommunityFeedAndDetail() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String dailyLogId = createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);

        mockMvc.perform(get("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sync_to_community", is(true)))
            .andExpect(jsonPath("$.data.community_post_id").exists());

        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)))
            .andExpect(jsonPath("$.data[0].source_daily_log_id", is(dailyLogId)))
            .andExpect(jsonPath("$.data[0].title", is("今天第一次主动跳上窗台晒太阳。")))
            .andExpect(jsonPath("$.data[0].author.nickname", is("Momo")))
            .andReturn();

        String postId = JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");
        mockMvc.perform(get("/api/v1/community/posts/%s".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.post_id", is(postId)))
            .andExpect(jsonPath("$.data.source_daily_log_id", is(dailyLogId)))
            .andExpect(jsonPath("$.data.pet.pet_name", is("Momo")))
            .andExpect(jsonPath("$.data.content", is("今天第一次主动跳上窗台晒太阳。")));
    }

    @Test
    void shouldUpdateAndWithdrawCommunityPostWhenDailyLogSyncChanges() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String dailyLogId = createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);

        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();
        String postId = JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");

        mockMvc.perform(patch("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "今天会主动叼玩具找人互动。",
                      "tags": ["互动", "成长"],
                      "visibility": "public",
                      "sync_to_community": true,
                      "happened_at": "2026-04-18T19:30:00+08:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sync_to_community", is(true)))
            .andExpect(jsonPath("$.data.community_post_id", is(postId)));
        approveLatestModerationTaskForPost(postId);

        mockMvc.perform(get("/api/v1/community/posts/%s".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", is("今天会主动叼玩具找人互动。")))
            .andExpect(jsonPath("$.data.title", is("今天会主动叼玩具找人互动。")));

        mockMvc.perform(patch("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "今天会主动叼玩具找人互动。",
                      "tags": ["互动", "成长"],
                      "visibility": "family",
                      "sync_to_community": false,
                      "happened_at": "2026-04-18T19:30:00+08:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sync_to_community", is(false)));

        mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.post_id == '%s')]".formatted(postId)).isEmpty());
    }

    @Test
    void shouldDeleteCommunityPostWhenDeletingSyncedDailyLog() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String dailyLogId = createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);

        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();
        String postId = JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");

        mockMvc.perform(delete("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.post_id == '%s')]".formatted(postId)).isEmpty());
    }

    @Test
    void shouldCreateAndListCommunityComments() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);

        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();
        String postId = JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");

        mockMvc.perform(post("/api/v1/community/posts/%s/comments".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "这条观察很真实，能看出已经越来越放松了。"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.post_id", is(postId)))
            .andExpect(jsonPath("$.data.content", is("这条观察很真实，能看出已经越来越放松了。")))
            .andExpect(jsonPath("$.data.author.nickname", is("Momo")));

        mockMvc.perform(get("/api/v1/community/posts/%s/comments".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)))
            .andExpect(jsonPath("$.data[0].content", is("这条观察很真实，能看出已经越来越放松了。")));

        mockMvc.perform(get("/api/v1/community/posts/%s".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.comment_count", is(1)));
    }

    @Test
    void shouldLikeAndUnlikeCommunityPost() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);

        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();
        String postId = JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");

        mockMvc.perform(post("/api/v1/community/posts/%s/like".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.post_id", is(postId)))
            .andExpect(jsonPath("$.data.liked", is(true)))
            .andExpect(jsonPath("$.data.like_count", is(1)));

        mockMvc.perform(delete("/api/v1/community/posts/%s/like".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.liked", is(false)))
            .andExpect(jsonPath("$.data.like_count", is(0)));
    }

    @Test
    void shouldFavoriteAndUnfavoriteCommunityPost() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);

        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();
        String postId = JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");

        mockMvc.perform(post("/api/v1/community/posts/%s/favorite".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.post_id", is(postId)))
            .andExpect(jsonPath("$.data.favorited", is(true)))
            .andExpect(jsonPath("$.data.favorite_count", is(1)));

        mockMvc.perform(delete("/api/v1/community/posts/%s/favorite".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorited", is(false)))
            .andExpect(jsonPath("$.data.favorite_count", is(0)));
    }

    @Test
    void shouldCreateAndReusePendingCommunityReport() throws Exception {
        String authorAuthorizationHeader = authorizationHeader();
        String reporterAuthorizationHeader = authorizationHeader("13900000000");
        String petId = currentPetId(authorAuthorizationHeader);
        createDailyLog(authorAuthorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);

        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader))
            .andExpect(status().isOk())
            .andReturn();
        String postId = JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");

        MvcResult firstReportResult = mockMvc.perform(post("/api/v1/community/posts/%s/report".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reason_code": "spam",
                      "reason_detail": "连续出现重复引流内容"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.target_type", is("post")))
            .andExpect(jsonPath("$.data.target_id", is(postId)))
            .andExpect(jsonPath("$.data.reason_code", is("spam")))
            .andExpect(jsonPath("$.data.reason_detail", is("连续出现重复引流内容")))
            .andExpect(jsonPath("$.data.status", is("pending")))
            .andReturn();

        String reportId = JsonPath.read(firstReportResult.getResponse().getContentAsString(), "$.data.report_id");

        mockMvc.perform(post("/api/v1/community/posts/%s/report".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reason_code": "spam",
                      "reason_detail": "再次提交同一条举报"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.report_id", is(reportId)))
            .andExpect(jsonPath("$.data.reason_detail", is("连续出现重复引流内容")))
            .andExpect(jsonPath("$.data.status", is("pending")));
    }

    @Test
    void shouldListModerationReports() throws Exception {
        String authorAuthorizationHeader = authorizationHeader();
        String reporterAuthorizationHeader = authorizationHeader("13900000000");
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String petId = currentPetId(authorAuthorizationHeader);
        createDailyLog(authorAuthorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);
        String postId = currentCommunityPostId(reporterAuthorizationHeader);
        createCommunityPostReport(reporterAuthorizationHeader, postId, "harassment", "持续使用攻击性语言");

        mockMvc.perform(get("/api/v1/admin/moderation/reports")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)))
            .andExpect(jsonPath("$.data[0].reporter_nickname", is("宠物家长")))
            .andExpect(jsonPath("$.data[0].reason_code", is("harassment")))
            .andExpect(jsonPath("$.data[0].post_title", is("今天第一次主动跳上窗台晒太阳。")))
            .andExpect(jsonPath("$.data[0].status", is("pending")));
    }

    @Test
    void shouldConfirmViolationAndHideReportedPost() throws Exception {
        String authorAuthorizationHeader = authorizationHeader();
        String reporterAuthorizationHeader = authorizationHeader("13900000000");
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String petId = currentPetId(authorAuthorizationHeader);
        createDailyLog(authorAuthorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);
        String postId = currentCommunityPostId(reporterAuthorizationHeader);
        String reportId = createCommunityPostReport(reporterAuthorizationHeader, postId, "illegal", "包含违规售卖信息");

        mockMvc.perform(patch("/api/v1/admin/moderation/reports/%s".formatted(reportId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "risk-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "action": "confirm_violation",
                      "admin_notes": "确认为违规售卖内容"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("processed")))
            .andExpect(jsonPath("$.data.processed_by", is("risk-admin")))
            .andExpect(jsonPath("$.data.admin_notes", is("确认为违规售卖内容")))
            .andExpect(jsonPath("$.data.post_review_status", is("rejected")));

        mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());

        mockMvc.perform(get("/api/v1/notifications?notify_type=system")
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[?(@.biz_type == 'moderation_report')].biz_id", is(List.of(reportId))))
            .andExpect(jsonPath("$.data.items[?(@.biz_type == 'moderation_report')].title", is(List.of("举报已处理"))));

        mockMvc.perform(get("/api/v1/admin/moderation/audit-logs?operator_id=risk-admin&target_type=moderation_report")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].target_id", is(reportId)))
            .andExpect(jsonPath("$.data[0].action", is("moderation_report_confirm_violation")))
            .andExpect(jsonPath("$.data[0].detail_json").value(org.hamcrest.Matchers.containsString("确认为违规售卖内容")));
    }

    @Test
    void shouldDismissReportAndKeepReportedPostVisible() throws Exception {
        String authorAuthorizationHeader = authorizationHeader();
        String reporterAuthorizationHeader = authorizationHeader("13900000000");
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String petId = currentPetId(authorAuthorizationHeader);
        createDailyLog(authorAuthorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);
        String postId = currentCommunityPostId(reporterAuthorizationHeader);
        String reportId = createCommunityPostReport(reporterAuthorizationHeader, postId, "spam", "误报测试");

        mockMvc.perform(patch("/api/v1/admin/moderation/reports/%s".formatted(reportId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "content-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "action": "dismiss_report",
                      "admin_notes": "误报，内容保持可见"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("rejected")))
            .andExpect(jsonPath("$.data.processed_by", is("content-admin")))
            .andExpect(jsonPath("$.data.admin_notes", is("误报，内容保持可见")))
            .andExpect(jsonPath("$.data.post_review_status", is("approved")));

        mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)))
            .andExpect(jsonPath("$.data[0].post_id", is(postId)));

        mockMvc.perform(patch("/api/v1/notifications/read")
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "notify_type": "all"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.unread_count", is(0)));
    }

    @Test
    void shouldPublishIndependentCommunityPostAndReadTopicQuestionDetails() throws Exception {
        String authorizationHeader = authorizationHeader();
        String answerAuthorizationHeader = authorizationHeader("13900000000");
        String petId = currentPetId(authorizationHeader);
        String topicId = createCommunityTopic("新手养宠讨论-" + System.nanoTime());
        String mediaAssetId = uploadMediaAsset(
            authorizationHeader,
            "community",
            "community-growth.png",
            MediaType.IMAGE_PNG
        );

        MvcResult postResult = mockMvc.perform(post("/api/v1/community/posts")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_id": %s,
                      "topic_id": %s,
                      "post_type": "image_text",
                      "title": "第一次独立发布社区帖子",
                      "content": "这是一条不依赖萌宠日常同步的社区帖子。",
                      "media_asset_ids": ["%s"],
                      "city_code": "310000",
                      "visibility": "public"
                    }
                    """.formatted(petId, topicId, mediaAssetId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title", is("第一次独立发布社区帖子")))
            .andExpect(jsonPath("$.data.source_daily_log_id", nullValue()))
            .andExpect(jsonPath("$.data.topic.topic_id", is(topicId)))
            .andExpect(jsonPath("$.data.media_asset_ids[0]", is(mediaAssetId)))
            .andExpect(jsonPath("$.data.media_assets[0].asset_id", is(mediaAssetId)))
            .andExpect(jsonPath("$.data.review_status", is("pending_review")))
            .andReturn();

        String postId = JsonPath.read(postResult.getResponse().getContentAsString(), "$.data.post_id");
        approveLatestModerationTaskForPost(postId);

        mockMvc.perform(get("/api/v1/community/posts/%s".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.post_id", is(postId)))
            .andExpect(jsonPath("$.data.content", is("这是一条不依赖萌宠日常同步的社区帖子。")))
            .andExpect(jsonPath("$.data.media_assets[0].asset_id", is(mediaAssetId)));

        mockMvc.perform(get("/api/v1/community/topics/%s".formatted(topicId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.topic.topic_id", is(topicId)))
            .andExpect(jsonPath("$.data.posts[?(@.post_id == '%s')].title".formatted(postId),
                is(List.of("第一次独立发布社区帖子"))));

        String questionId = createIndependentCommunityPost(
            authorizationHeader,
            petId,
            topicId,
            "qa",
            "幼猫第一次换粮应该怎么过渡？",
            "准备从幼猫粮换到全阶段粮，想确认过渡周期。"
        );

        mockMvc.perform(post("/api/v1/community/posts/%s/comments".formatted(questionId))
                .header(HttpHeaders.AUTHORIZATION, answerAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "建议至少 7 天逐步增加新粮比例，并观察软便情况。"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/community/questions/%s".formatted(questionId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.question.post_id", is(questionId)))
            .andExpect(jsonPath("$.data.question.post_type", is("qa")))
            .andExpect(jsonPath("$.data.answers[0].content", is("建议至少 7 天逐步增加新粮比例，并观察软便情况。")));
    }

    @Test
    void shouldCreateModerationTaskFilterPendingAndSupportManualReview() throws Exception {
        String authorizationHeader = authorizationHeader("13610000000");
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String topicId = createCommunityTopic("审核底座测试-" + System.nanoTime());

        String pendingPostId = createPendingCommunityPost(
            authorizationHeader,
            petId,
            topicId,
            "image_text",
            "待审核普通帖子",
            "这条内容需要先进入审核队列。"
        );
        mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.post_id == '%s')]".formatted(pendingPostId)).isEmpty());

        MvcResult taskListResult = mockMvc.perform(get(
                "/api/v1/admin/moderation/tasks?target_type=community_post&target_id=%s&review_status=pending"
                    .formatted(pendingPostId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)))
            .andExpect(jsonPath("$.data[0].provider_code", is("dev_noop")))
            .andExpect(jsonPath("$.data[0].content_type", is("text")))
            .andExpect(jsonPath("$.data[0].content_snapshot")
                .value(org.hamcrest.Matchers.containsString("这条内容需要先进入审核队列。")))
            .andReturn();

        String taskId = JsonPath.read(taskListResult.getResponse().getContentAsString(), "$.data[0].task_id");
        mockMvc.perform(patch("/api/v1/admin/moderation/tasks/%s/status".formatted(taskId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "review-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "action": "approve",
                      "risk_labels": [],
                      "admin_notes": "人工复核通过"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.review_status", is("approved")))
            .andExpect(jsonPath("$.data.review_result").value(org.hamcrest.Matchers.containsString("manual")));

        mockMvc.perform(get("/api/v1/community/posts/%s".formatted(pendingPostId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.review_status", is("approved")));

        String rejectedPostId = createPendingCommunityPost(
            authorizationHeader,
            petId,
            topicId,
            "qa",
            "待拒绝问答帖子",
            "这条问答用于验证人工拒绝。"
        );
        String rejectedTaskId = latestModerationTaskIdByTarget("community_question", rejectedPostId);
        mockMvc.perform(patch("/api/v1/admin/moderation/tasks/%s/status".formatted(rejectedTaskId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "review-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "action": "reject",
                      "risk_labels": ["manual_reject"],
                      "admin_notes": "人工复核拒绝"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.review_status", is("rejected")))
            .andExpect(jsonPath("$.data.risk_labels").value(org.hamcrest.Matchers.containsString("manual_reject")));

        mockMvc.perform(get("/api/v1/community/questions/%s".formatted(rejectedPostId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code", is("COMMUNITY_POST_NOT_FOUND")));

        mockMvc.perform(get("/api/v1/admin/moderation/audit-logs?operator_id=review-admin&target_type=moderation_task")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.action == 'moderation_task_approve')].target_id", is(List.of(taskId))))
            .andExpect(jsonPath("$.data[?(@.action == 'moderation_task_reject')].target_id", is(List.of(rejectedTaskId))));
    }

    @Test
    void shouldCreateModerationTaskForSyncedDailyLogAndHideUntilApproved() throws Exception {
        String authorizationHeader = authorizationHeader("13610000001");
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String petId = currentPetId(authorizationHeader);

        MvcResult dailyLogResult = mockMvc.perform(post("/api/v1/pets/%s/daily-logs".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "同步社区前必须先进入内容审核。",
                      "tags": ["审核", "社区"],
                      "visibility": "public",
                      "sync_to_community": true,
                      "happened_at": "2026-04-17T10:00:00+08:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.community_post_id").exists())
            .andReturn();

        String responseBody = dailyLogResult.getResponse().getContentAsString();
        String dailyLogId = JsonPath.read(responseBody, "$.data.daily_log_id");
        String postId = JsonPath.read(responseBody, "$.data.community_post_id");

        mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.post_id == '%s')]".formatted(postId)).isEmpty());

        mockMvc.perform(get("/api/v1/admin/moderation/tasks?target_type=community_post&target_id=%s".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)))
            .andExpect(jsonPath("$.data[0].review_status", is("pending")))
            .andExpect(jsonPath("$.data[0].content_snapshot")
                .value(org.hamcrest.Matchers.containsString(dailyLogId)));
    }

    @Test
    void shouldRegisterPushTokenAndCreatePushTaskFromNotification() throws Exception {
        String authorAuthorizationHeader = authorizationHeader("13610000002");
        String reporterAuthorizationHeader = authorizationHeader("13710000000");
        String reporterUserId = currentUserId(reporterAuthorizationHeader);
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String deviceTokenId = registerPushDeviceToken(reporterAuthorizationHeader, "push-token-" + System.nanoTime());

        String petId = currentPetId(authorAuthorizationHeader);
        createDailyLog(authorAuthorizationHeader, petId, "Push 派生通知测试。", true);
        String postId = currentCommunityPostId(reporterAuthorizationHeader);
        String reportId = createCommunityPostReport(reporterAuthorizationHeader, postId, "spam", "触发举报处理通知");

        mockMvc.perform(patch("/api/v1/admin/moderation/reports/%s".formatted(reportId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", "push-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "action": "dismiss_report",
                      "admin_notes": "Push 派生验证"
                    }
                    """))
            .andExpect(status().isOk());

        MvcResult pushTaskResult = mockMvc.perform(get(
                "/api/v1/admin/push-tasks?user_id=%s&task_status=pending&provider_code=dev_noop"
                    .formatted(reporterUserId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.biz_type == 'moderation_report')].biz_id", is(List.of(reportId))))
            .andExpect(jsonPath("$.data[?(@.biz_type == 'moderation_report')].task_status", is(List.of("pending"))))
            .andReturn();

        List<String> pushTaskIds = JsonPath.read(
            pushTaskResult.getResponse().getContentAsString(),
            "$.data[?(@.biz_type == 'moderation_report')].push_task_id"
        );
        String pushTaskId = pushTaskIds.get(0);
        mockMvc.perform(get("/api/v1/admin/push-deliveries?push_task_id=%s&delivery_status=pending".formatted(pushTaskId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].device_token_id", is(deviceTokenId)))
            .andExpect(jsonPath("$.data[0].provider_code", is("dev_noop")));
    }

    @Test
    void shouldSkipPushTaskWhenNotificationSwitchOff() throws Exception {
        String authorAuthorizationHeader = authorizationHeader("13610000003");
        String reporterAuthorizationHeader = authorizationHeader("13710000001");
        String reporterUserId = currentUserId(reporterAuthorizationHeader);
        String adminAuthorizationHeader = adminAuthorizationHeader();
        registerPushDeviceToken(reporterAuthorizationHeader, "push-token-switch-off-" + System.nanoTime());

        mockMvc.perform(patch("/api/v1/me/settings/notifications")
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "notification_enabled": false,
                      "privacy_level": "normal"
                    }
                    """))
            .andExpect(status().isOk());

        String petId = currentPetId(authorAuthorizationHeader);
        createDailyLog(authorAuthorizationHeader, petId, "通知开关拦截 Push 测试。", true);
        String postId = currentCommunityPostId(authorAuthorizationHeader);
        String reportId = createCommunityPostReport(reporterAuthorizationHeader, postId, "spam", "关闭通知后不应生成站内消息");

        mockMvc.perform(patch("/api/v1/admin/moderation/reports/%s".formatted(reportId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "action": "dismiss_report",
                      "admin_notes": "通知开关关闭"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications?notify_type=system")
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[?(@.biz_type == 'moderation_report')]").isEmpty());

        mockMvc.perform(get("/api/v1/admin/push-tasks?user_id=%s&task_status=skipped".formatted(reporterUserId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.biz_type == 'moderation_report')].biz_id", is(List.of(reportId))))
            .andExpect(jsonPath("$.data[?(@.biz_type == 'moderation_report')].failure_reason",
                is(List.of("notification_switch_off"))));
    }

    @Test
    void shouldRejectModerationTaskAndPushAdminQueriesWithoutAdminToken() throws Exception {
        String authorizationHeader = authorizationHeader("13610000004");

        mockMvc.perform(get("/api/v1/admin/moderation/tasks"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/v1/admin/moderation/tasks")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/v1/admin/push-tasks")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(post("/api/v1/push/device-tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "platform": "ios",
                      "device_token": "anonymous-token"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void shouldFollowUnfollowAndReadFollowingFeed() throws Exception {
        String authorAuthorizationHeader = authorizationHeader("13600000000");
        String followerAuthorizationHeader = authorizationHeader("13700000000");
        String authorUserId = currentUserId(authorAuthorizationHeader);
        String authorPetId = currentPetId(authorAuthorizationHeader);
        String topicId = createCommunityTopic("关注流测试-" + System.nanoTime());
        String postId = createIndependentCommunityPost(
            authorAuthorizationHeader,
            authorPetId,
            topicId,
            "image_text",
            "关注流里的独立帖子",
            "关注作者后应该能在 following 信息流看到这条内容。"
        );

        mockMvc.perform(get("/api/v1/community/users/%s/follow-status".formatted(authorUserId))
                .header(HttpHeaders.AUTHORIZATION, followerAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.followed_user_id", is(authorUserId)))
            .andExpect(jsonPath("$.data.following", is(false)));

        mockMvc.perform(post("/api/v1/community/users/%s/follow".formatted(authorUserId))
                .header(HttpHeaders.AUTHORIZATION, followerAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.following", is(true)));

        mockMvc.perform(get("/api/v1/community/feed?tab=following")
                .header(HttpHeaders.AUTHORIZATION, followerAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.post_id == '%s')].title".formatted(postId),
                is(List.of("关注流里的独立帖子"))));

        mockMvc.perform(delete("/api/v1/community/users/%s/follow".formatted(authorUserId))
                .header(HttpHeaders.AUTHORIZATION, followerAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.following", is(false)));

        mockMvc.perform(get("/api/v1/community/users/%s/follow-status".formatted(authorUserId))
                .header(HttpHeaders.AUTHORIZATION, followerAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.following", is(false)));
    }

    @Test
    void shouldGovernCommunityPostsAndQuestionsFromAdmin() throws Exception {
        String authorizationHeader = authorizationHeader("13500000000");
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String adminOperator = "community-governance-admin";
        String petId = currentPetId(authorizationHeader);
        String topicId = createCommunityTopic("社区治理测试-" + System.nanoTime());
        String postId = createIndependentCommunityPost(
            authorizationHeader,
            petId,
            topicId,
            "image_text",
            "后台治理普通帖子",
            "这条普通帖子用于验证后台下架和恢复。"
        );
        String questionId = createIndependentCommunityPost(
            authorizationHeader,
            petId,
            topicId,
            "qa",
            "后台治理问答帖",
            "这条问答用于验证后台问答治理。"
        );

        mockMvc.perform(get("/api/v1/admin/community/posts?review_status=approved&topic_id=%s".formatted(topicId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.post_id == '%s')].title".formatted(postId),
                is(List.of("后台治理普通帖子"))));

        mockMvc.perform(get("/api/v1/admin/community/posts/%s".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.post_id", is(postId)))
            .andExpect(jsonPath("$.data.review_status", is("approved")));

        mockMvc.perform(patch("/api/v1/admin/community/posts/%s/status".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "action": "take_down",
                      "admin_notes": "内容与社区规则不符"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.review_status", is("rejected")));

        mockMvc.perform(get("/api/v1/community/posts/%s".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code", is("COMMUNITY_POST_NOT_FOUND")));

        mockMvc.perform(patch("/api/v1/admin/community/posts/%s/status".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "action": "restore",
                      "admin_notes": "复核后恢复展示"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.review_status", is("approved")));

        mockMvc.perform(get("/api/v1/community/posts/%s".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.post_id", is(postId)));

        mockMvc.perform(get("/api/v1/admin/community/questions?keyword=问答")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.post_id == '%s')].post_type".formatted(questionId), is(List.of("qa"))));

        mockMvc.perform(get("/api/v1/admin/community/questions/%s".formatted(questionId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.question.post_id", is(questionId)));

        mockMvc.perform(patch("/api/v1/admin/community/questions/%s/status".formatted(questionId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "action": "take_down",
                      "admin_notes": "问答内容不适合展示"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.review_status", is("rejected")));

        mockMvc.perform(get("/api/v1/community/questions/%s".formatted(questionId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/admin/moderation/audit-logs?operator_id=%s&target_type=community_post"
                .formatted(adminOperator))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.action == 'community_post_take_down')].target_id", is(List.of(postId))))
            .andExpect(jsonPath("$.data[?(@.action == 'community_post_restore')].target_id", is(List.of(postId))));

        mockMvc.perform(get("/api/v1/admin/moderation/audit-logs?operator_id=%s&target_type=community_question"
                .formatted(adminOperator))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.action == 'community_question_take_down')].target_id",
                is(List.of(questionId))));
    }

    @Test
    void shouldRejectCommunityClosedLoopPermissionBoundary() throws Exception {
        String authorizationHeader = authorizationHeader();
        String currentUserId = currentUserId(authorizationHeader);

        mockMvc.perform(post("/api/v1/community/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "post_type": "image_text",
                      "content": "未登录不允许发布社区帖子"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/v1/admin/community/posts")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(post("/api/v1/community/users/%s/follow".formatted(currentUserId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("不能关注自己")));
    }

    @Test
    void shouldGetUpdateAndDeleteDailyLog() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String dailyLogId = createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。");

        mockMvc.perform(get("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.daily_log_id", is(dailyLogId)))
            .andExpect(jsonPath("$.data.content", is("今天第一次主动跳上窗台晒太阳。")));

        mockMvc.perform(patch("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "今天学会了在门口等人回家。",
                      "tags": ["等待", "互动"],
                      "visibility": "family",
                      "sync_to_community": false,
                      "happened_at": "2026-04-18T19:30:00+08:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", is("今天学会了在门口等人回家。")))
            .andExpect(jsonPath("$.data.tags[0]", is("等待")))
            .andExpect(jsonPath("$.data.visibility", is("family")));

        mockMvc.perform(delete("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(get("/api/v1/pets/%s/daily-logs".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.daily_log_id == '%s')]".formatted(dailyLogId)).isEmpty());
    }

    @Test
    void shouldReturnTimelineEventsFromHealthAndDailyLogs() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String healthRecordId = createHealthRecord(authorizationHeader, petId, "体重复查");
        String dailyLogId = createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。");

        mockMvc.perform(get("/api/v1/pets/%s/timeline".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(2)))
            .andExpect(jsonPath("$.data[0].event_type", is("daily_log")))
            .andExpect(jsonPath("$.data[0].source_type", is("daily_log")))
            .andExpect(jsonPath("$.data[0].source_id", is(dailyLogId)))
            .andExpect(jsonPath("$.data[1].event_type", is("health")))
            .andExpect(jsonPath("$.data[1].source_type", is("health_record")))
            .andExpect(jsonPath("$.data[1].source_id", is(healthRecordId)));

        mockMvc.perform(get("/api/v1/pets/%s/timeline?event_type=health".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)))
            .andExpect(jsonPath("$.data[0].event_type", is("health")))
            .andExpect(jsonPath("$.data[0].title", is("体重复查")))
            .andExpect(jsonPath("$.data[0].summary", is("4.3 kg · 饮食正常")));
    }

    @Test
    void shouldQueryHealthDailyAndTimelineFromAdminEndpoints() throws Exception {
        String authorizationHeader = authorizationHeader();
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String healthAssetId = uploadMediaAsset(
            authorizationHeader,
            "health_report",
            "admin-health.png",
            MediaType.IMAGE_PNG
        );
        String dailyAssetId = uploadMediaAsset(
            authorizationHeader,
            "daily_log",
            "admin-daily.jpg",
            MediaType.IMAGE_JPEG
        );

        MvcResult healthRecordResult = mockMvc.perform(post("/api/v1/pets/%s/health-records".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "record_type": "examination",
                      "title": "后台体重复查",
                      "attachment_asset_ids": ["%s"],
                      "occurred_at": "2026-04-19T10:30:00+08:00",
                      "notes": "后台查询健康记录附件"
                    }
                    """.formatted(healthAssetId)))
            .andExpect(status().isOk())
            .andReturn();
        String healthRecordId = JsonPath.read(
            healthRecordResult.getResponse().getContentAsString(),
            "$.data.health_record_id"
        );

        MvcResult dailyLogResult = mockMvc.perform(post("/api/v1/pets/%s/daily-logs".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "后台日常查询内容",
                      "media_asset_ids": ["%s"],
                      "tags": ["后台"],
                      "visibility": "public",
                      "sync_to_community": false,
                      "happened_at": "2026-04-19T11:30:00+08:00"
                    }
                    """.formatted(dailyAssetId)))
            .andExpect(status().isOk())
            .andReturn();
        String dailyLogId = JsonPath.read(dailyLogResult.getResponse().getContentAsString(), "$.data.daily_log_id");

        mockMvc.perform(get("/api/v1/admin/health-records?pet_id=%s&record_type=examination&keyword=后台体重"
                .formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.health_record.health_record_id == '%s')].pet.pet_name"
                .formatted(healthRecordId), is(List.of("Momo"))))
            .andExpect(jsonPath("$.data[?(@.health_record.health_record_id == '%s')].health_record.attachment_assets[0].asset_id"
                .formatted(healthRecordId), is(List.of(healthAssetId))));

        mockMvc.perform(get("/api/v1/admin/health-records/%s".formatted(healthRecordId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.health_record.title", is("后台体重复查")))
            .andExpect(jsonPath("$.data.operator.user_id").exists())
            .andExpect(jsonPath("$.data.health_record.attachment_assets[0].asset_id", is(healthAssetId)));

        mockMvc.perform(get("/api/v1/admin/daily-logs?pet_id=%s&visibility=public&keyword=后台日常"
                .formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.daily_log.daily_log_id == '%s')].author.nickname"
                .formatted(dailyLogId), is(List.of("Momo"))))
            .andExpect(jsonPath("$.data[?(@.daily_log.daily_log_id == '%s')].daily_log.media_assets[0].asset_id"
                .formatted(dailyLogId), is(List.of(dailyAssetId))));

        mockMvc.perform(get("/api/v1/admin/daily-logs/%s".formatted(dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.daily_log.content", is("后台日常查询内容")))
            .andExpect(jsonPath("$.data.pet.pet_id", is(petId)))
            .andExpect(jsonPath("$.data.daily_log.media_assets[0].asset_id", is(dailyAssetId)));

        MvcResult timelineResult = mockMvc.perform(get(
                "/api/v1/admin/timeline/events?pet_id=%s&event_type=health&source_type=health_record"
                    .formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.timeline_event.source_id == '%s')].source_status"
                .formatted(healthRecordId), is(List.of("active"))))
            .andReturn();

        List<String> eventIds = JsonPath.read(
            timelineResult.getResponse().getContentAsString(),
            "$.data[?(@.timeline_event.source_id == '%s')].timeline_event.event_id".formatted(healthRecordId)
        );
        assertEquals(1, eventIds.size());

        mockMvc.perform(get("/api/v1/admin/timeline/events/%s".formatted(eventIds.getFirst()))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.timeline_event.source_id", is(healthRecordId)))
            .andExpect(jsonPath("$.data.source_status", is("active")))
            .andExpect(jsonPath("$.data.pet.pet_id", is(petId)));
    }

    @Test
    void shouldSyncTimelineEventsAfterUpdatingAndDeletingSourceRecords() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String healthRecordId = createHealthRecord(authorizationHeader, petId, "体重复查");
        String dailyLogId = createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。");

        mockMvc.perform(patch("/api/v1/pets/%s/health-records/%s".formatted(petId, healthRecordId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "record_type": "examination",
                      "title": "年度体检复查",
                      "value": "4.8",
                      "unit": "kg",
                      "occurred_at": "2026-04-19T10:30:00+08:00",
                      "notes": "精神状态稳定"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "今天学会了在门口等人回家。",
                      "tags": ["等待", "互动"],
                      "visibility": "family",
                      "sync_to_community": false,
                      "happened_at": "2026-04-18T19:30:00+08:00"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/pets/%s/timeline?event_type=health".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title", is("年度体检复查")))
            .andExpect(jsonPath("$.data[0].summary", is("4.8 kg · 精神状态稳定")));

        mockMvc.perform(get("/api/v1/pets/%s/timeline?event_type=daily_log".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title", is("今天学会了在门口等人回家。")))
            .andExpect(jsonPath("$.data[0].summary", is("今天学会了在门口等人回家。")));

        mockMvc.perform(delete("/api/v1/pets/%s/health-records/%s".formatted(petId, healthRecordId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/pets/%s/timeline".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldReturnServiceHomeAndProviderDirectory() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        ProviderFixture provider = createServiceProviderWithSlot("hospital", "安心宠物医院", 2);

        mockMvc.perform(get("/api/v1/services/home?pet_id=%s&city_code=310000".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.opened", is(true)))
            .andExpect(jsonPath("$.data.categories[?(@.provider_type == 'hospital')].provider_count", is(List.of(1))))
            .andExpect(jsonPath("$.data.featured_providers[0].provider_name", is("安心宠物医院")))
            .andExpect(jsonPath("$.data.commerce_placeholder", is("商城当前保持预留，不进入服务预约链路")));

        mockMvc.perform(get("/api/v1/providers?provider_type=hospital&city_code=310000")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].provider_id", is(provider.providerId())))
            .andExpect(jsonPath("$.data[0].service_items[0].service_name", is("基础问诊")))
            .andExpect(jsonPath("$.data[0].available_slots[0].available_quota", is(2)));

        mockMvc.perform(get("/api/v1/providers/%s/slots?appointment_type=hospital&start_date=%s&end_date=%s"
                .formatted(provider.providerId(), provider.slotDate(), provider.slotDate()))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].appointment_type", is("hospital")))
            .andExpect(jsonPath("$.data[0].bookable", is(true)));
    }

    @Test
    void shouldManageServiceCityConfigInAdminAndControlUserDirectory() throws Exception {
        String authorizationHeader = authorizationHeader();
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String adminOperator = "service-city-admin";
        String petId = currentPetId(authorizationHeader);

        mockMvc.perform(post("/api/v1/admin/service/cities")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "city_code": "330100",
                      "city_name": "杭州",
                      "opened": false,
                      "unavailable_reason": "杭州服务正在筹备中",
                      "sort_order": 20
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.city_code", is("330100")))
            .andExpect(jsonPath("$.data.opened", is(false)));

        ProviderFixture provider = createServiceProviderWithSlot("hospital", "西湖宠物医院", 2, "330100");

        mockMvc.perform(get("/api/v1/services/home?pet_id=%s&city_code=330100".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.city_name", is("杭州")))
            .andExpect(jsonPath("$.data.opened", is(false)))
            .andExpect(jsonPath("$.data.unavailable_reason", is("杭州服务正在筹备中")))
            .andExpect(jsonPath("$.data.featured_providers").isEmpty());

        mockMvc.perform(get("/api/v1/providers?provider_type=hospital&city_code=330100")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());

        mockMvc.perform(get("/api/v1/providers/%s".formatted(provider.providerId()))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("杭州服务正在筹备中")));

        mockMvc.perform(get("/api/v1/providers/%s/slots?appointment_type=hospital&start_date=%s&end_date=%s"
                .formatted(provider.providerId(), provider.slotDate(), provider.slotDate()))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("杭州服务正在筹备中")));

        mockMvc.perform(post("/api/v1/admin/service/cities")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "city_code": "330100",
                      "city_name": "杭州",
                      "opened": true,
                      "sort_order": 20
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.opened", is(true)))
            .andExpect(jsonPath("$.data.unavailable_reason", nullValue()));

        mockMvc.perform(get("/api/v1/admin/service/cities?city_code=330100&opened=true")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].city_name", is("杭州")));

        mockMvc.perform(get("/api/v1/providers?provider_type=hospital&city_code=330100")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].provider_id", is(provider.providerId())));

        mockMvc.perform(get("/api/v1/admin/service/audit-logs?operator_id=%s&target_type=service_city"
                .formatted(adminOperator))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.action == 'service_city_upsert')].target_id",
                is(List.of("330100", "330100"))));
    }

    @Test
    void shouldCreateCancelAppointmentAndSyncTimelineNotification() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        ProviderFixture provider = createServiceProviderWithSlot("grooming", "毛孩子洗护中心", 1);

        MvcResult appointmentResult = mockMvc.perform(post("/api/v1/appointments")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_id": "%s",
                      "provider_id": "%s",
                      "appointment_type": "grooming",
                      "appointment_date": "%s",
                      "appointment_slot": "%s",
                      "demand_desc": "基础洗护，注意耳朵清洁",
                      "contact_name": "Momo家长",
                      "contact_mobile": "13800000000"
                    }
                    """.formatted(petId, provider.providerId(), provider.slotDate(), provider.appointmentSlot())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("pending_confirm")))
            .andExpect(jsonPath("$.data.provider_name", is("毛孩子洗护中心")))
            .andReturn();

        String appointmentId = JsonPath.read(appointmentResult.getResponse().getContentAsString(), "$.data.appointment_id");

        mockMvc.perform(get("/api/v1/providers/%s/slots?appointment_type=grooming&start_date=%s&end_date=%s"
                .formatted(provider.providerId(), provider.slotDate(), provider.slotDate()))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].available_quota", is(0)))
            .andExpect(jsonPath("$.data[0].status", is("full")))
            .andExpect(jsonPath("$.data[0].bookable", is(false)));

        mockMvc.perform(get("/api/v1/appointments?status=pending_confirm")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].appointment_id", is(appointmentId)));

        mockMvc.perform(get("/api/v1/pets/%s/timeline?event_type=service".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].source_type", is("service_appointment")))
            .andExpect(jsonPath("$.data[0].source_id", is(appointmentId)))
            .andExpect(jsonPath("$.data[0].title", is("洗护美容预约")));

        mockMvc.perform(get("/api/v1/notifications?notify_type=appointment")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].biz_type", is("appointment_created")))
            .andExpect(jsonPath("$.data.items[0].biz_id", is(appointmentId)));

        mockMvc.perform(patch("/api/v1/appointments/%s/cancel".formatted(appointmentId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cancel_reason": "临时改期"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("canceled")))
            .andExpect(jsonPath("$.data.remark", is("临时改期")));

        mockMvc.perform(get("/api/v1/providers/%s/slots?appointment_type=grooming&start_date=%s&end_date=%s"
                .formatted(provider.providerId(), provider.slotDate(), provider.slotDate()))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].available_quota", is(1)))
            .andExpect(jsonPath("$.data[0].status", is("open")));
    }

    @Test
    void shouldManageServiceProviderItemsAndSlotsInAdmin() throws Exception {
        String authorizationHeader = authorizationHeader();
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String adminOperator = "service-resource-admin";
        LocalDate slotDate = LocalDate.now().plusDays(5);
        ensureServiceCityOpened("310000", "上海");

        MvcResult providerResult = mockMvc.perform(post("/api/v1/admin/service/providers")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "provider_type": "boarding",
                      "provider_name": "优住宠物寄养中心",
                      "city_code": "310000",
                      "address": "上海市长宁区宠物友好路 66 号",
                      "latitude": 31.218,
                      "longitude": 121.402,
                      "contact_phone": "021-87654321",
                      "business_hours": "08:30-21:00",
                      "rating_avg": 4.7,
                      "review_count": 12,
                      "status": "rest"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.provider_name", is("优住宠物寄养中心")))
            .andExpect(jsonPath("$.data.status", is("rest")))
            .andReturn();

        String providerId = JsonPath.read(providerResult.getResponse().getContentAsString(), "$.data.provider_id");

        mockMvc.perform(patch("/api/v1/admin/service/providers/%s".formatted(providerId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "provider_type": "boarding",
                      "provider_name": "优住宠物寄养中心 Pro",
                      "city_code": "310000",
                      "address": "上海市长宁区宠物友好路 66 号",
                      "latitude": 31.218,
                      "longitude": 121.402,
                      "contact_phone": "021-87654321",
                      "business_hours": "08:30-21:00",
                      "rating_avg": 4.8,
                      "review_count": 13,
                      "status": "online"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.provider_name", is("优住宠物寄养中心 Pro")))
            .andExpect(jsonPath("$.data.status", is("online")));

        MvcResult itemResult = mockMvc.perform(post("/api/v1/admin/service/providers/%s/items".formatted(providerId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "service_code": "boarding_daycare",
                      "service_name": "日间托管",
                      "service_desc": "白天照看、喂食和活动记录",
                      "price_min": 99,
                      "price_max": 199,
                      "status": "active"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.service_items[0].service_name", is("日间托管")))
            .andReturn();

        String serviceItemId = JsonPath.read(
            itemResult.getResponse().getContentAsString(),
            "$.data.service_items[0].service_item_id"
        );

        mockMvc.perform(patch("/api/v1/admin/service/providers/%s/items/%s".formatted(providerId, serviceItemId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "service_code": "boarding_daycare",
                      "service_name": "日间托管升级版",
                      "service_desc": "白天照看、喂食、活动记录和接送确认",
                      "price_min": 129,
                      "price_max": 229,
                      "status": "active"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.service_items[0].service_name", is("日间托管升级版")));

        MvcResult slotResult = mockMvc.perform(post("/api/v1/admin/service/providers/%s/slots".formatted(providerId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "appointment_type": "boarding",
                      "slot_date": "%s",
                      "start_time": "09:00:00",
                      "end_time": "10:30:00",
                      "quota": 3,
                      "status": "open"
                    }
                    """.formatted(slotDate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available_slots[0].available_quota", is(3)))
            .andReturn();

        String slotId = JsonPath.read(
            slotResult.getResponse().getContentAsString(),
            "$.data.available_slots[0].slot_id"
        );

        mockMvc.perform(patch("/api/v1/admin/service/providers/%s/slots/%s".formatted(providerId, slotId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "appointment_type": "boarding",
                      "slot_date": "%s",
                      "start_time": "09:00:00",
                      "end_time": "10:30:00",
                      "quota": 4,
                      "status": "open"
                    }
                    """.formatted(slotDate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available_slots[0].available_quota", is(4)));

        mockMvc.perform(get("/api/v1/admin/service/providers?provider_type=boarding&city_code=310000&status=online")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.provider_id == '%s')].provider_name".formatted(providerId),
                is(List.of("优住宠物寄养中心 Pro"))));

        mockMvc.perform(get("/api/v1/providers?provider_type=boarding&city_code=310000")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.provider_id == '%s')].service_items[0].service_name".formatted(providerId),
                is(List.of("日间托管升级版"))));

        mockMvc.perform(get("/api/v1/admin/service/audit-logs?operator_id=%s&target_type=provider_schedule_slot"
                .formatted(adminOperator))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.action == 'provider_schedule_slot_update')].target_id",
                is(List.of(slotId))));
    }

    @Test
    void shouldListAndUpdateServiceAppointmentsInAdmin() throws Exception {
        String authorizationHeader = authorizationHeader();
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String adminOperator = "service-appointment-admin";
        String petId = currentPetId(authorizationHeader);
        ProviderFixture provider = createServiceProviderWithSlot("training", "正向训练营", 1);

        MvcResult appointmentResult = mockMvc.perform(post("/api/v1/appointments")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_id": "%s",
                      "provider_id": "%s",
                      "appointment_type": "training",
                      "appointment_date": "%s",
                      "appointment_slot": "%s",
                      "demand_desc": "基础随行训练",
                      "contact_name": "Momo家长",
                      "contact_mobile": "13800000000"
                    }
                    """.formatted(petId, provider.providerId(), provider.slotDate(), provider.appointmentSlot())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("pending_confirm")))
            .andReturn();

        String appointmentId = JsonPath.read(appointmentResult.getResponse().getContentAsString(), "$.data.appointment_id");

        mockMvc.perform(get("/api/v1/admin/service/appointments?status=pending_confirm&provider_type=training&city_code=310000")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.appointment_id == '%s')].provider_name".formatted(appointmentId),
                is(List.of("正向训练营"))));

        mockMvc.perform(patch("/api/v1/admin/service/appointments/%s/status".formatted(appointmentId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "confirmed",
                      "remark": "已确认训练师和到店时间"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("confirmed")))
            .andExpect(jsonPath("$.data.remark", is("已确认训练师和到店时间")));

        mockMvc.perform(patch("/api/v1/admin/service/appointments/%s/status".formatted(appointmentId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "canceled",
                      "remark": "用户改期，后台取消"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("canceled")))
            .andExpect(jsonPath("$.data.remark", is("用户改期，后台取消")));

        mockMvc.perform(get("/api/v1/providers/%s/slots?appointment_type=training&start_date=%s&end_date=%s"
                .formatted(provider.providerId(), provider.slotDate(), provider.slotDate()))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].available_quota", is(1)))
            .andExpect(jsonPath("$.data[0].status", is("open")));

        mockMvc.perform(get("/api/v1/admin/service/audit-logs?operator_id=%s&target_type=service_appointment"
                .formatted(adminOperator))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.action == 'service_appointment_status_update')].target_id",
                is(List.of(appointmentId, appointmentId))));
    }

    @Test
    void shouldCreateListAndModerateServiceProviderReviews() throws Exception {
        String authorizationHeader = authorizationHeader();
        String adminAuthorizationHeader = adminAuthorizationHeader();
        String adminOperator = "service-review-admin";
        String petId = currentPetId(authorizationHeader);
        ProviderFixture provider = createServiceProviderWithSlot("hospital", "口碑宠物医院", 1);

        MvcResult appointmentResult = mockMvc.perform(post("/api/v1/appointments")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_id": "%s",
                      "provider_id": "%s",
                      "appointment_type": "hospital",
                      "appointment_date": "%s",
                      "appointment_slot": "%s",
                      "demand_desc": "复查体重和饮食状态",
                      "contact_name": "Momo家长",
                      "contact_mobile": "13800000000"
                    }
                    """.formatted(petId, provider.providerId(), provider.slotDate(), provider.appointmentSlot())))
            .andExpect(status().isOk())
            .andReturn();

        String appointmentId = JsonPath.read(appointmentResult.getResponse().getContentAsString(), "$.data.appointment_id");

        mockMvc.perform(patch("/api/v1/admin/service/appointments/%s/status".formatted(appointmentId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "confirmed",
                      "remark": "已确认到店时间"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/service/appointments/%s/status".formatted(appointmentId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "completed",
                      "remark": "服务已完成"
                    }
                    """))
            .andExpect(status().isOk());

        MvcResult reviewResult = mockMvc.perform(post("/api/v1/appointments/%s/review".formatted(appointmentId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 5,
                      "content": "医生沟通很细心，复查建议也清楚。"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.rating", is(5)))
            .andExpect(jsonPath("$.data.status", is("visible")))
            .andReturn();

        String reviewId = JsonPath.read(reviewResult.getResponse().getContentAsString(), "$.data.review_id");

        mockMvc.perform(get("/api/v1/providers/%s/reviews".formatted(provider.providerId()))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].review_id", is(reviewId)))
            .andExpect(jsonPath("$.data[0].content", is("医生沟通很细心，复查建议也清楚。")));

        mockMvc.perform(get("/api/v1/appointments?status=completed")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.appointment_id == '%s')].reviewed".formatted(appointmentId), is(List.of(true))));

        mockMvc.perform(get("/api/v1/admin/service/reviews?status=visible&provider_type=hospital&city_code=310000")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.review_id == '%s')].provider_name".formatted(reviewId), is(List.of("口碑宠物医院"))));

        mockMvc.perform(patch("/api/v1/admin/service/reviews/%s/status".formatted(reviewId))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader)
                .header("X-Admin-Operator", adminOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "hidden"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("hidden")));

        mockMvc.perform(get("/api/v1/providers/%s/reviews".formatted(provider.providerId()))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());

        mockMvc.perform(get("/api/v1/admin/service/audit-logs?operator_id=%s&target_type=provider_review"
                .formatted(adminOperator))
                .header(HttpHeaders.AUTHORIZATION, adminAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.action == 'provider_review_status_update')].target_id",
                is(List.of(reviewId))));
    }

    @Test
    void shouldReturnAggregatedPetSummary() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        createReminder(authorizationHeader, petId);
        createHealthRecord(authorizationHeader, petId, "体重复查");
        createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。");

        mockMvc.perform(get("/api/v1/pets/%s/summary".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.today_todo_count", is(1)))
            .andExpect(jsonPath("$.data.recent_health_records[0]", is("体重复查")))
            .andExpect(jsonPath("$.data.recent_daily_logs[0]").exists());
    }

    @Test
    void shouldReturnHomeAggregate() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        createReminder(authorizationHeader, petId);
        createHealthRecord(authorizationHeader, petId, "首页聚合体重复查");
        createDailyLog(authorizationHeader, petId, "首页聚合日常记录。", true);

        mockMvc.perform(get("/api/v1/home")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_user.current_pet_id", is(petId)))
            .andExpect(jsonPath("$.data.dashboard.pet.pet_id", is(petId)))
            .andExpect(jsonPath("$.data.dashboard.today_todo_count", is(1)))
            .andExpect(jsonPath("$.data.dashboard.health_records[0].title", is("首页聚合体重复查")))
            .andExpect(jsonPath("$.data.dashboard.daily_logs[0].content", is("首页聚合日常记录。")))
            .andExpect(jsonPath("$.data.quick_actions[0].action_key", is("feed")))
            .andExpect(jsonPath("$.data.service_entries[?(@.entry_key == 'hospital')]").isNotEmpty())
            .andExpect(jsonPath("$.data.device_summary.enabled", is(false)));
    }

    @Test
    void shouldReturnWeeklyPetReport() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        OffsetDateTime now = OffsetDateTime.now().withSecond(0).withNano(0);

        createReminderAt(
            authorizationHeader,
            petId,
            "疫苗复查提醒",
            now.minusDays(1).withHour(9).withMinute(0)
        );
        String completedReminderId = createReminderAt(
            authorizationHeader,
            petId,
            "体内驱虫提醒",
            now.minusDays(2).withHour(10).withMinute(0)
        );
        String skippedReminderId = createReminderAt(
            authorizationHeader,
            petId,
            "复查观察提醒",
            now.minusDays(3).withHour(11).withMinute(0)
        );
        createWeightHealthRecordAt(
            authorizationHeader,
            petId,
            "本周体重记录",
            now.minusDays(2).withHour(8).withMinute(30),
            "4.5",
            "称重时精神状态稳定"
        );
        createMedicationHealthRecordAt(
            authorizationHeader,
            petId,
            "益生菌补充",
            now.minusDays(1).withHour(20).withMinute(0),
            "晚饭后补充"
        );
        createDailyLogWithTagsAt(
            authorizationHeader,
            petId,
            "快捷记录：今天完成了一次喂食。",
            List.of("快捷记录", "喂食"),
            false,
            now.minusDays(1).withHour(8).withMinute(0)
        );
        createDailyLogWithTagsAt(
            authorizationHeader,
            petId,
            "快捷记录：今天补记了一次饮水。",
            List.of("快捷记录", "饮水"),
            false,
            now.minusDays(1).withHour(14).withMinute(0)
        );
        createDailyLogWithTagsAt(
            authorizationHeader,
            petId,
            "快捷记录：今天补记了一次排便观察。",
            List.of("快捷记录", "排便"),
            true,
            now.minusDays(2).withHour(18).withMinute(0)
        );

        mockMvc.perform(patch("/api/v1/pets/%s/reminders/%s/complete".formatted(petId, completedReminderId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/pets/%s/reminders/%s/skip".formatted(petId, skippedReminderId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/home/reports/weekly")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.report_type", is("weekly")))
            .andExpect(jsonPath("$.data.pet.pet_id", is(petId)))
            .andExpect(jsonPath("$.data.pending_reminder_count", is(1)))
            .andExpect(jsonPath("$.data.completed_reminder_count", is(1)))
            .andExpect(jsonPath("$.data.skipped_reminder_count", is(1)))
            .andExpect(jsonPath("$.data.health_record_count", is(2)))
            .andExpect(jsonPath("$.data.daily_log_count", is(3)))
            .andExpect(jsonPath("$.data.community_sync_count", is(1)))
            .andExpect(jsonPath("$.data.feed_count", is(1)))
            .andExpect(jsonPath("$.data.water_count", is(1)))
            .andExpect(jsonPath("$.data.toilet_count", is(1)))
            .andExpect(jsonPath("$.data.weight_record_count", is(1)))
            .andExpect(jsonPath("$.data.medication_record_count", is(1)))
            .andExpect(jsonPath("$.data.highlights[0]").exists())
            .andExpect(jsonPath("$.data.recent_reminders[0].reminder_id").exists())
            .andExpect(jsonPath("$.data.recent_health_records[0].title", is("益生菌补充")))
            .andExpect(jsonPath("$.data.recent_daily_logs[0].tags[0]", is("快捷记录")));
    }

    @Test
    void shouldReturnMonthlyPetReport() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        OffsetDateTime now = OffsetDateTime.now().withSecond(0).withNano(0);

        createReminderAt(
            authorizationHeader,
            petId,
            "月度体检提醒",
            now.minusDays(10).withHour(9).withMinute(0)
        );
        createWeightHealthRecordAt(
            authorizationHeader,
            petId,
            "月度体重记录",
            now.minusDays(12).withHour(8).withMinute(0),
            "4.8",
            "本月状态平稳"
        );
        createDailyLogWithTagsAt(
            authorizationHeader,
            petId,
            "这个月第一次主动在门口等人回家。",
            List.of("成长", "互动"),
            false,
            now.minusDays(9).withHour(19).withMinute(30)
        );

        mockMvc.perform(get("/api/v1/home/reports/monthly")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.report_type", is("monthly")))
            .andExpect(jsonPath("$.data.pet.pet_id", is(petId)))
            .andExpect(jsonPath("$.data.window_start").exists())
            .andExpect(jsonPath("$.data.window_end").exists())
            .andExpect(jsonPath("$.data.health_record_count", is(1)))
            .andExpect(jsonPath("$.data.daily_log_count", is(1)))
            .andExpect(jsonPath("$.data.recent_daily_logs[0].content", is("这个月第一次主动在门口等人回家。")));
    }

    @Test
    void shouldRejectProtectedApiWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    private String authorizationHeader() throws Exception {
        return authorizationHeader("13800000000");
    }

    private String uniqueMobile(String prefix) {
        return prefix + "%08d".formatted(Math.floorMod(System.nanoTime(), 100_000_000L));
    }

    private String authorizationHeader(String mobile) throws Exception {
        return loginTokens(mobile).authorizationHeader();
    }

    private String adminAuthorizationHeader() throws Exception {
        return adminLoginTokens().authorizationHeader();
    }

    private LoginTokensFixture loginTokens(String mobile) throws Exception {
        sendSmsCodeForLogin(mobile);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mobile": "%s",
                      "code": "%s"
                    }
                    """.formatted(mobile, TEST_SMS_CODE)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = JsonPath.read(responseBody, "$.data.access_token");
        String refreshToken = JsonPath.read(responseBody, "$.data.refresh_token");
        return new LoginTokensFixture(accessToken, refreshToken);
    }

    private LoginTokensFixture adminLoginTokens() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "admin",
                      "password": "petlife123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.admin.username", is("admin")))
            .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = JsonPath.read(responseBody, "$.data.access_token");
        String refreshToken = JsonPath.read(responseBody, "$.data.refresh_token");
        return new LoginTokensFixture(accessToken, refreshToken);
    }

    private void sendSmsCodeForLogin(String mobile) throws Exception {
        resetSmsSendWindow(mobile);
        mockMvc.perform(post("/api/v1/auth/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mobile": "%s",
                      "scene": "%s"
                    }
                    """.formatted(mobile, TEST_SMS_SCENE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.mocked_code").doesNotExist());
        prepareLatestSmsCodeForTest(mobile, TEST_SMS_CODE);
    }

    private void resetSmsSendWindow(String mobile) {
        jdbcTemplate.update("""
            UPDATE sms_send_records
            SET created_at = DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 HOUR)
            WHERE mobile = ?
            """, mobile);
    }

    private void prepareLatestSmsCodeForTest(String mobile, String code) {
        Long verificationId = jdbcTemplate.queryForObject("""
            SELECT id
            FROM sms_verification_codes
            WHERE mobile = ? AND scene = ? AND status = 'active'
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """, Long.class, mobile, TEST_SMS_SCENE);
        String salt = "phase-one-" + Math.floorMod(System.nanoTime(), 1_000_000_000L);
        jdbcTemplate.update("""
            UPDATE sms_verification_codes
            SET salt = ?,
                code_hash = ?
            WHERE id = ?
            """, salt, hashSmsCode(salt, mobile, TEST_SMS_SCENE, code), verificationId);
    }

    private String latestSmsCodeHash(String mobile) {
        return jdbcTemplate.queryForObject("""
            SELECT code_hash
            FROM sms_verification_codes
            WHERE mobile = ? AND scene = ?
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """, String.class, mobile, TEST_SMS_SCENE);
    }

    private String latestSmsCodeSalt(String mobile) {
        return jdbcTemplate.queryForObject("""
            SELECT salt
            FROM sms_verification_codes
            WHERE mobile = ? AND scene = ?
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """, String.class, mobile, TEST_SMS_SCENE);
    }

    private String hashSmsCode(String salt, String mobile, String scene, String code) {
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

    private String currentUserId(String authorizationHeader) throws Exception {
        MvcResult currentUserResult = mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(currentUserResult.getResponse().getContentAsString(), "$.data.user.user_id");
    }

    private String currentFamilyId(String authorizationHeader) throws Exception {
        MvcResult familyResult = mockMvc.perform(get("/api/v1/family")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(familyResult.getResponse().getContentAsString(), "$.data.family_id");
    }

    private String currentPetId(String authorizationHeader) throws Exception {
        MvcResult currentUserResult = mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(currentUserResult.getResponse().getContentAsString(), "$.data.current_pet_id");
    }

    private String createPet(String authorizationHeader, String petName) throws Exception {
        MvcResult createPetResult = mockMvc.perform(post("/api/v1/pets")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_name": "%s",
                      "pet_type": "dog",
                      "breed": "Corgi",
                      "gender": "male",
                      "birthday": "2024-03-08",
                      "adopt_date": "2024-06-18",
                      "neuter_status": "pending",
                      "avatar_asset_id": "asset_1001"
                    }
                    """.formatted(petName)))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(createPetResult.getResponse().getContentAsString(), "$.data.pet_id");
    }

    private String createHealthRecord(String authorizationHeader, String petId, String title) throws Exception {
        MvcResult createHealthRecordResult = mockMvc.perform(post("/api/v1/pets/%s/health-records".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "record_type": "weight",
                      "title": "%s",
                      "value": "4.3",
                      "unit": "kg",
                      "occurred_at": "2026-04-17T08:30:00+08:00",
                      "notes": "饮食正常"
                    }
                    """.formatted(title)))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(createHealthRecordResult.getResponse().getContentAsString(), "$.data.health_record_id");
    }

    private String createReminder(String authorizationHeader, String petId) throws Exception {
        return createReminderAt(
            authorizationHeader,
            petId,
            "体内驱虫提醒",
            OffsetDateTime.parse("2026-04-18T09:00:00+08:00")
        );
    }

    private String createReminderAt(
        String authorizationHeader,
        String petId,
        String title,
        OffsetDateTime dueAt
    ) throws Exception {
        MvcResult createReminderResult = mockMvc.perform(post("/api/v1/pets/%s/reminders".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reminder_type": "deworming",
                      "title": "%s",
                      "due_at": "%s",
                      "notes": "饭后执行"
                    }
                    """.formatted(title, dueAt)))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(createReminderResult.getResponse().getContentAsString(), "$.data.reminder_id");
    }

    private CreatedReminderFixture createCycleReminder(
        String authorizationHeader,
        String petId,
        String title,
        int cycleValue,
        String cycleUnit,
        OffsetDateTime dueAt
    ) throws Exception {
        MvcResult createReminderResult = mockMvc.perform(post("/api/v1/pets/%s/reminders".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reminder_type": "deworming",
                      "title": "%s",
                      "reminder_mode": "cycle",
                      "cycle_value": %s,
                      "cycle_unit": "%s",
                      "due_at": "%s",
                      "notes": "周期提醒测试"
                    }
                    """.formatted(title, cycleValue, cycleUnit, dueAt)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = createReminderResult.getResponse().getContentAsString();
        return new CreatedReminderFixture(
            JsonPath.read(responseBody, "$.data.reminder_id"),
            JsonPath.read(responseBody, "$.data.due_at")
        );
    }

    private String createDailyLog(String authorizationHeader, String petId, String content) throws Exception {
        return createDailyLog(authorizationHeader, petId, content, false);
    }

    private String createDailyLog(
        String authorizationHeader,
        String petId,
        String content,
        boolean syncToCommunity
    ) throws Exception {
        return createDailyLogWithTagsAt(
            authorizationHeader,
            petId,
            content,
            List.of("晒太阳", "成长"),
            syncToCommunity,
            OffsetDateTime.parse("2026-04-17T10:00:00+08:00")
        );
    }

    private String createDailyLogWithTagsAt(
        String authorizationHeader,
        String petId,
        String content,
        List<String> tags,
        boolean syncToCommunity,
        OffsetDateTime happenedAt
    ) throws Exception {
        MvcResult createDailyLogResult = mockMvc.perform(post("/api/v1/pets/%s/daily-logs".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "%s",
                      "tags": %s,
                      "visibility": "public",
                      "sync_to_community": %s,
                      "happened_at": "%s"
                    }
                    """.formatted(content, toJsonArray(tags), syncToCommunity, happenedAt)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = createDailyLogResult.getResponse().getContentAsString();
        if (syncToCommunity) {
            String communityPostId = JsonPath.read(responseBody, "$.data.community_post_id");
            approveLatestModerationTaskForPost(communityPostId);
        }
        return JsonPath.read(responseBody, "$.data.daily_log_id");
    }

    private String createWeightHealthRecordAt(
        String authorizationHeader,
        String petId,
        String title,
        OffsetDateTime occurredAt,
        String value,
        String notes
    ) throws Exception {
        return createHealthRecordAt(
            authorizationHeader,
            petId,
            "weight",
            title,
            occurredAt,
            value,
            "kg",
            notes
        );
    }

    private String createMedicationHealthRecordAt(
        String authorizationHeader,
        String petId,
        String title,
        OffsetDateTime occurredAt,
        String notes
    ) throws Exception {
        return createHealthRecordAt(
            authorizationHeader,
            petId,
            "medication",
            title,
            occurredAt,
            null,
            null,
            notes
        );
    }

    private String uploadMediaAsset(
        String authorizationHeader,
        String bizType,
        String fileName,
        MediaType contentType
    ) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            fileName,
            contentType.toString(),
            ("media-" + fileName).getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/media-assets")
                .file(file)
                .param("biz_type", bizType)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.asset_id").exists())
            .andReturn();

        return JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.data.asset_id");
    }

    private String createHealthRecordAt(
        String authorizationHeader,
        String petId,
        String recordType,
        String title,
        OffsetDateTime occurredAt,
        String value,
        String unit,
        String notes
    ) throws Exception {
        MvcResult createHealthRecordResult = mockMvc.perform(post("/api/v1/pets/%s/health-records".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "record_type": "%s",
                      "title": "%s",
                      "value": %s,
                      "unit": %s,
                      "occurred_at": "%s",
                      "notes": "%s"
                    }
                    """.formatted(
                    recordType,
                    title,
                    value == null ? "null" : "\"%s\"".formatted(value),
                    unit == null ? "null" : "\"%s\"".formatted(unit),
                    occurredAt,
                    notes
                )))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(createHealthRecordResult.getResponse().getContentAsString(), "$.data.health_record_id");
    }

    private String toJsonArray(List<String> values) {
        return values.stream()
            .map(value -> "\"%s\"".formatted(value))
            .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
    }

    private String currentCommunityPostId(String authorizationHeader) throws Exception {
        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");
    }

    private String createCommunityPostReport(
        String authorizationHeader,
        String postId,
        String reasonCode,
        String reasonDetail
    ) throws Exception {
        MvcResult reportResult = mockMvc.perform(post("/api/v1/community/posts/%s/report".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reason_code": "%s",
                      "reason_detail": "%s"
                    }
                    """.formatted(reasonCode, reasonDetail)))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(reportResult.getResponse().getContentAsString(), "$.data.report_id");
    }

    private String createCommunityTopic(String topicName) {
        jdbcTemplate.update("""
            INSERT INTO community_topics (
              topic_name, topic_desc, city_code, status, created_at, updated_at
            ) VALUES (
              ?, '用于接口自动化验证的社区话题', '310000', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            """, topicName);
        Long topicId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return topicId.toString();
    }

    private String createIndependentCommunityPost(
        String authorizationHeader,
        String petId,
        String topicId,
        String postType,
        String title,
        String content
    ) throws Exception {
        MvcResult postResult = mockMvc.perform(post("/api/v1/community/posts")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_id": %s,
                      "topic_id": %s,
                      "post_type": "%s",
                      "title": "%s",
                      "content": "%s",
                      "media_asset_ids": [],
                      "city_code": "310000",
                      "visibility": "public"
                    }
                    """.formatted(petId, topicId, postType, title, content)))
            .andExpect(status().isOk())
            .andReturn();

        String postId = JsonPath.read(postResult.getResponse().getContentAsString(), "$.data.post_id");
        approveLatestModerationTaskForPost(postId);
        return postId;
    }

    private String createPendingCommunityPost(
        String authorizationHeader,
        String petId,
        String topicId,
        String postType,
        String title,
        String content
    ) throws Exception {
        MvcResult postResult = mockMvc.perform(post("/api/v1/community/posts")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_id": %s,
                      "topic_id": %s,
                      "post_type": "%s",
                      "title": "%s",
                      "content": "%s",
                      "media_asset_ids": [],
                      "city_code": "310000",
                      "visibility": "public"
                    }
                    """.formatted(petId, topicId, postType, title, content)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.review_status", is("pending_review")))
            .andReturn();

        return JsonPath.read(postResult.getResponse().getContentAsString(), "$.data.post_id");
    }

    private void approveLatestModerationTaskForPost(String postId) {
        jdbcTemplate.update("""
            UPDATE moderation_tasks
            SET review_status = 'approved',
                review_result = JSON_OBJECT('source', 'test_setup'),
                risk_labels = JSON_ARRAY(),
                reviewed_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE target_id = ?
              AND target_type IN ('community_post', 'community_question')
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """, Long.valueOf(postId));
        jdbcTemplate.update("""
            UPDATE community_posts
            SET review_status = 'approved',
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, Long.valueOf(postId));
    }

    private String latestModerationTaskIdByTarget(String targetType, String targetId) {
        Long taskId = jdbcTemplate.queryForObject("""
            SELECT id
            FROM moderation_tasks
            WHERE target_type = ?
              AND target_id = ?
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """, Long.class, targetType, Long.valueOf(targetId));
        return String.valueOf(taskId);
    }

    private String registerPushDeviceToken(String authorizationHeader, String deviceToken) throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/v1/push/device-tokens")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "platform": "android",
                      "provider_code": "dev_noop",
                      "device_token": "%s",
                      "device_id": "android-test-device",
                      "app_version": "1.0.0"
                    }
                    """.formatted(deviceToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enabled", is(true)))
            .andExpect(jsonPath("$.data.provider_code", is("dev_noop")))
            .andExpect(jsonPath("$.data.device_token_suffix", is(deviceToken.substring(deviceToken.length() - 6))))
            .andReturn();
        return JsonPath.read(registerResult.getResponse().getContentAsString(), "$.data.device_token_id");
    }

    private ProviderFixture createServiceProviderWithSlot(
        String providerType,
        String providerName,
        int quota
    ) {
        ensureServiceCityOpened("310000", "上海");
        return createServiceProviderWithSlot(providerType, providerName, quota, "310000");
    }

    private ProviderFixture createServiceProviderWithSlot(
        String providerType,
        String providerName,
        int quota,
        String cityCode
    ) {
        jdbcTemplate.update("""
            INSERT INTO service_providers (
              provider_type, provider_name, city_code, address, contact_phone,
              business_hours, rating_avg, review_count, status, created_at, updated_at
            ) VALUES (
              ?, ?, ?, '上海市徐汇区宠物友好路 88 号', '021-12345678',
              '09:00-20:00', 4.8, 16, 'online', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            """, providerType, providerName, cityCode);
        Long providerId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("""
            INSERT INTO provider_service_items (
              provider_id, service_code, service_name, service_desc,
              price_min, price_max, status, created_at, updated_at
            ) VALUES (
              ?, ?, '基础问诊', '面向日常照护的基础服务项目',
              99.00, 199.00, 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            """, providerId, providerType + "_basic");
        LocalDate slotDate = LocalDate.now().plusDays(3);
        jdbcTemplate.update("""
            INSERT INTO provider_schedule_slots (
              provider_id, appointment_type, slot_date, start_time, end_time,
              quota, booked_count, status, created_at, updated_at
            ) VALUES (
              ?, ?, ?, '10:00:00', '11:00:00',
              ?, 0, 'open', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            """, providerId, providerType, slotDate, quota);
        return new ProviderFixture(providerId.toString(), slotDate, "10:00-11:00");
    }

    private void ensureServiceCityOpened(String cityCode, String cityName) {
        jdbcTemplate.update("""
            INSERT INTO service_city_configs (
              city_code, city_name, opened, unavailable_reason, sort_order,
              created_at, updated_at
            ) VALUES (
              ?, ?, 1, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON DUPLICATE KEY UPDATE
              city_name = VALUES(city_name),
              opened = 1,
              unavailable_reason = NULL,
              deleted_at = NULL,
              updated_at = CURRENT_TIMESTAMP
            """, cityCode, cityName);
    }

    private String createFamilyInvitation(String authorizationHeader, String mobile, String petId) throws Exception {
        MvcResult invitationResult = mockMvc.perform(post("/api/v1/family/invitations")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "invitee_mobile": "%s",
                      "role": "member",
                      "shared_pet_ids": ["%s"]
                    }
                    """.formatted(mobile, petId)))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(invitationResult.getResponse().getContentAsString(), "$.data.invite_code");
    }

    @SuppressWarnings("unchecked")
    private String joinFamilyMember(String ownerAuthorizationHeader, String mobile, String role) throws Exception {
        String memberAuthorizationHeader = authorizationHeader(mobile);
        String memberUserId = currentUserId(memberAuthorizationHeader);
        String familyId = currentFamilyId(ownerAuthorizationHeader);
        familyPersistenceMapper.insertFamilyMember(Long.valueOf(familyId), Long.valueOf(memberUserId), role);

        MvcResult familyResult = mockMvc.perform(get("/api/v1/family")
                .header(HttpHeaders.AUTHORIZATION, ownerAuthorizationHeader))
            .andExpect(status().isOk())
            .andReturn();

        List<String> memberIds = JsonPath.read(
            familyResult.getResponse().getContentAsString(),
            "$.data.members[?(@.user_id == '%s')].member_id".formatted(memberUserId)
        );
        return memberIds.get(0);
    }

    private record CreatedReminderFixture(
        String reminderId,
        String dueAt
    ) {
    }

    private record LoginTokensFixture(
        String accessToken,
        String refreshToken
    ) {
        private String authorizationHeader() {
            return "Bearer " + accessToken;
        }
    }

    private record ProviderFixture(
        String providerId,
        LocalDate slotDate,
        String appointmentSlot
    ) {
    }
}
