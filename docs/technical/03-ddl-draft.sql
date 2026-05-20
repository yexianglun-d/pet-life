-- 宠物生活管家 MySQL 8 DDL 草案
-- 设计目标：
-- 1. 以 pet 作为核心主轴，支撑健康、日常、社区、服务、商城、设备、家庭共养。
-- 2. 采用模块化单体友好的强业务表 + 技术支撑表设计。
-- 3. 保留 deleted_at 软删除字段，避免直接物理删除核心业务数据。
-- 4. 本文件是全量 DDL 草案，当前首批迁移仅执行用户端范围所需表。
-- 5. `products/*` 和 `devices/*` 相关表保留为后续预留，不进入当前迁移计划。

CREATE DATABASE IF NOT EXISTS `pet_life`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `pet_life`;

-- ====================================================================
-- 用户与认证
-- ====================================================================

CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `mobile` VARCHAR(20) NOT NULL COMMENT '手机号',
  `nickname` VARCHAR(50) NOT NULL COMMENT '用户昵称',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  `city_code` VARCHAR(32) DEFAULT NULL COMMENT '当前城市编码',
  `city_name` VARCHAR(50) DEFAULT NULL COMMENT '当前城市名称',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常 2-禁用 3-注销中',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最近登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_mobile` (`mobile`),
  KEY `idx_users_city` (`city_code`),
  KEY `idx_users_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `user_settings` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `current_pet_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '当前宠物 ID',
  `notification_switch` TINYINT NOT NULL DEFAULT 1 COMMENT '通知总开关：0-关 1-开',
  `privacy_level` VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '隐私级别',
  `extra_json` JSON DEFAULT NULL COMMENT '扩展配置',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_settings_user` (`user_id`),
  CONSTRAINT `fk_user_settings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户设置表';

CREATE TABLE IF NOT EXISTS `user_sessions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `refresh_token_hash` CHAR(64) NOT NULL COMMENT '刷新令牌摘要',
  `device_id` VARCHAR(64) DEFAULT NULL COMMENT '客户端设备标识',
  `device_type` VARCHAR(20) DEFAULT NULL COMMENT '设备类型：ios/android/web',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT '登录 IP',
  `user_agent` VARCHAR(255) DEFAULT NULL COMMENT '客户端标识',
  `expires_at` DATETIME NOT NULL COMMENT '过期时间',
  `revoked_at` DATETIME DEFAULT NULL COMMENT '吊销时间',
  `last_active_at` DATETIME DEFAULT NULL COMMENT '最近活跃时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sessions_token` (`refresh_token_hash`),
  KEY `idx_user_sessions_user` (`user_id`),
  KEY `idx_user_sessions_expire` (`expires_at`),
  CONSTRAINT `fk_user_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户登录会话表';

CREATE TABLE IF NOT EXISTS `sms_verification_codes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `mobile` VARCHAR(20) NOT NULL COMMENT '手机号',
  `scene` VARCHAR(30) NOT NULL COMMENT '业务场景：login',
  `code_hash` CHAR(64) NOT NULL COMMENT '验证码 SHA256 摘要',
  `salt` VARCHAR(64) NOT NULL COMMENT '验证码摘要随机盐',
  `expires_at` DATETIME NOT NULL COMMENT '过期时间',
  `verified_at` DATETIME DEFAULT NULL COMMENT '验证通过时间',
  `attempt_count` INT NOT NULL DEFAULT 0 COMMENT '已尝试次数',
  `max_attempt_count` INT NOT NULL DEFAULT 5 COMMENT '最大尝试次数',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/verified/expired/locked/send_failed',
  `request_ip` VARCHAR(64) DEFAULT NULL COMMENT '请求 IP',
  `user_agent` VARCHAR(255) DEFAULT NULL COMMENT '客户端标识',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sms_codes_mobile_scene_status` (`mobile`, `scene`, `status`, `created_at` DESC),
  KEY `idx_sms_codes_expire` (`expires_at`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信验证码记录表';

CREATE TABLE IF NOT EXISTS `sms_send_records` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `verification_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '验证码记录 ID',
  `mobile` VARCHAR(20) NOT NULL COMMENT '手机号',
  `scene` VARCHAR(30) NOT NULL COMMENT '业务场景：login',
  `provider_code` VARCHAR(64) NOT NULL COMMENT '短信供应商编码',
  `send_status` VARCHAR(20) NOT NULL COMMENT '发送状态：accepted/failed/blocked',
  `failure_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败或拦截原因',
  `request_ip` VARCHAR(64) DEFAULT NULL COMMENT '请求 IP',
  `user_agent` VARCHAR(255) DEFAULT NULL COMMENT '客户端标识',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sms_send_mobile_scene_time` (`mobile`, `scene`, `created_at` DESC),
  KEY `idx_sms_send_ip_scene_time` (`request_ip`, `scene`, `created_at` DESC),
  KEY `idx_sms_send_provider_status` (`provider_code`, `send_status`),
  CONSTRAINT `fk_sms_send_verification` FOREIGN KEY (`verification_id`) REFERENCES `sms_verification_codes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信发送记录表';

CREATE TABLE IF NOT EXISTS `admin_accounts` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `username` VARCHAR(50) NOT NULL COMMENT '后台登录账号',
  `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码摘要',
  `display_name` VARCHAR(50) NOT NULL COMMENT '管理员展示名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常 2-禁用',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最近登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_accounts_username` (`username`),
  KEY `idx_admin_accounts_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台管理员账号表';

CREATE TABLE IF NOT EXISTS `admin_sessions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `admin_account_id` BIGINT UNSIGNED NOT NULL COMMENT '管理员账号 ID',
  `refresh_token_hash` CHAR(64) NOT NULL COMMENT '刷新令牌 SHA256 摘要',
  `expires_at` DATETIME NOT NULL COMMENT '会话过期时间',
  `revoked_at` DATETIME DEFAULT NULL COMMENT '吊销时间',
  `last_active_at` DATETIME DEFAULT NULL COMMENT '最近活跃时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_sessions_refresh_hash` (`refresh_token_hash`),
  KEY `idx_admin_sessions_account` (`admin_account_id`),
  KEY `idx_admin_sessions_active` (`revoked_at`, `expires_at`),
  CONSTRAINT `fk_admin_sessions_account` FOREIGN KEY (`admin_account_id`) REFERENCES `admin_accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台管理员登录会话表';

-- ====================================================================
-- 家庭与宠物主数据
-- ====================================================================

CREATE TABLE IF NOT EXISTS `families` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `family_name` VARCHAR(100) NOT NULL COMMENT '家庭名称',
  `owner_user_id` BIGINT UNSIGNED NOT NULL COMMENT '家庭拥有者用户 ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '家庭状态：1-正常 2-停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_families_owner` (`owner_user_id`),
  KEY `idx_families_status` (`status`),
  CONSTRAINT `fk_families_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭表';

CREATE TABLE IF NOT EXISTS `family_members` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `family_id` BIGINT UNSIGNED NOT NULL COMMENT '家庭 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '成员用户 ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：owner/admin/member',
  `invite_status` VARCHAR(20) NOT NULL DEFAULT 'joined' COMMENT '邀请状态：pending/joined/rejected',
  `joined_at` DATETIME DEFAULT NULL COMMENT '加入时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_family_members_unique` (`family_id`, `user_id`),
  KEY `idx_family_members_user` (`user_id`),
  KEY `idx_family_members_role` (`role`),
  CONSTRAINT `fk_family_members_family` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`),
  CONSTRAINT `fk_family_members_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭成员表';

CREATE TABLE IF NOT EXISTS `family_invitations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `family_id` BIGINT UNSIGNED NOT NULL COMMENT '家庭 ID',
  `inviter_user_id` BIGINT UNSIGNED NOT NULL COMMENT '邀请人 ID',
  `invitee_mobile` VARCHAR(20) DEFAULT NULL COMMENT '被邀请手机号',
  `invitee_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '被邀请用户 ID',
  `role` VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT '邀请角色',
  `shared_pet_ids` JSON DEFAULT NULL COMMENT '共享宠物 ID 列表',
  `invite_code` VARCHAR(64) NOT NULL COMMENT '邀请唯一码',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '邀请状态：pending/accepted/rejected/expired',
  `expired_at` DATETIME DEFAULT NULL COMMENT '过期时间',
  `accepted_at` DATETIME DEFAULT NULL COMMENT '接受时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_family_invitations_code` (`invite_code`),
  KEY `idx_family_invitations_family` (`family_id`),
  KEY `idx_family_invitations_inviter` (`inviter_user_id`),
  KEY `idx_family_invitations_invitee` (`invitee_user_id`),
  KEY `idx_family_invitations_status` (`status`),
  CONSTRAINT `fk_family_invitations_family` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`),
  CONSTRAINT `fk_family_invitations_inviter` FOREIGN KEY (`inviter_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_family_invitations_invitee` FOREIGN KEY (`invitee_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭邀请表';

CREATE TABLE IF NOT EXISTS `pets` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `family_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '所属家庭 ID',
  `owner_user_id` BIGINT UNSIGNED NOT NULL COMMENT '主要拥有者用户 ID',
  `pet_name` VARCHAR(50) NOT NULL COMMENT '宠物名称',
  `pet_type` VARCHAR(20) NOT NULL COMMENT '宠物类型：cat/dog/other',
  `breed` VARCHAR(50) DEFAULT NULL COMMENT '品种',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别',
  `birthday` DATE DEFAULT NULL COMMENT '出生日期',
  `adopt_date` DATE DEFAULT NULL COMMENT '到家日期',
  `neuter_status` TINYINT DEFAULT NULL COMMENT '绝育状态：0-否 1-是',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  `weight_kg` DECIMAL(5,2) DEFAULT NULL COMMENT '当前体重（kg）',
  `allergy_notes` VARCHAR(500) DEFAULT NULL COMMENT '过敏信息',
  `medical_history` TEXT DEFAULT NULL COMMENT '重要病史',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '宠物状态：active/memorial/rehomed',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_pets_owner` (`owner_user_id`, `status`),
  KEY `idx_pets_family` (`family_id`),
  KEY `idx_pets_type` (`pet_type`),
  CONSTRAINT `fk_pets_family` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`),
  CONSTRAINT `fk_pets_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宠物主档表';

-- ====================================================================
-- 媒体资产
-- ====================================================================

CREATE TABLE IF NOT EXISTS `media_assets` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `uploader_user_id` BIGINT UNSIGNED NOT NULL COMMENT '上传用户 ID',
  `biz_type` VARCHAR(30) NOT NULL COMMENT '业务类型：avatar/daily_log/community/health_report',
  `media_type` VARCHAR(20) NOT NULL COMMENT '媒体类型：image/video/file',
  `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `object_key` VARCHAR(255) NOT NULL COMMENT '对象存储路径',
  `bucket_name` VARCHAR(100) NOT NULL COMMENT '存储桶名称',
  `cdn_url` VARCHAR(255) DEFAULT NULL COMMENT 'CDN 访问地址',
  `content_type` VARCHAR(100) DEFAULT NULL COMMENT '文件内容类型',
  `file_size` BIGINT UNSIGNED DEFAULT NULL COMMENT '文件大小（字节）',
  `width` INT DEFAULT NULL COMMENT '媒体宽度',
  `height` INT DEFAULT NULL COMMENT '媒体高度',
  `duration_ms` INT DEFAULT NULL COMMENT '视频时长（毫秒）',
  `file_hash` CHAR(64) DEFAULT NULL COMMENT '文件哈希值',
  `upload_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '上传状态：pending/uploaded/failed',
  `review_status` VARCHAR(20) NOT NULL DEFAULT 'pending_review' COMMENT '审核状态',
  `completed_at` DATETIME DEFAULT NULL COMMENT '上传完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_media_assets_object` (`object_key`),
  KEY `idx_media_assets_user` (`uploader_user_id`),
  KEY `idx_media_assets_status` (`upload_status`, `review_status`),
  KEY `idx_media_assets_hash` (`file_hash`),
  CONSTRAINT `fk_media_assets_user` FOREIGN KEY (`uploader_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='媒体资产表';

-- ====================================================================
-- 健康记录、提醒、萌宠日常、时间轴
-- ====================================================================

CREATE TABLE IF NOT EXISTS `pet_health_records` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `pet_id` BIGINT UNSIGNED NOT NULL COMMENT '宠物 ID',
  `record_type` VARCHAR(30) NOT NULL COMMENT '记录类型：vaccine/deworming/checkup/medication/observation',
  `title` VARCHAR(100) NOT NULL COMMENT '记录标题',
  `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
  `operator_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '记录操作者用户 ID',
  `hospital_name` VARCHAR(100) DEFAULT NULL COMMENT '医院名称',
  `doctor_name` VARCHAR(50) DEFAULT NULL COMMENT '医生名称',
  `severity_level` VARCHAR(20) DEFAULT NULL COMMENT '严重程度',
  `result_summary` VARCHAR(500) DEFAULT NULL COMMENT '结果摘要',
  `attachments` JSON DEFAULT NULL COMMENT '附件资产 ID 列表',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_pet_health_pet_type_time` (`pet_id`, `record_type`, `occurred_at` DESC),
  KEY `idx_pet_health_operator` (`operator_user_id`),
  CONSTRAINT `fk_pet_health_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`),
  CONSTRAINT `fk_pet_health_operator` FOREIGN KEY (`operator_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宠物健康记录表';

CREATE TABLE IF NOT EXISTS `pet_reminders` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `pet_id` BIGINT UNSIGNED NOT NULL COMMENT '宠物 ID',
  `reminder_type` VARCHAR(30) NOT NULL COMMENT '提醒类型：vaccine/deworming/checkup/recheck/custom',
  `title` VARCHAR(100) NOT NULL COMMENT '提醒标题',
  `reminder_mode` VARCHAR(20) NOT NULL COMMENT '提醒模式：single/cycle',
  `cycle_value` INT DEFAULT NULL COMMENT '周期间隔值',
  `cycle_unit` VARCHAR(20) DEFAULT NULL COMMENT '周期单位：day/week/month',
  `remind_at` DATETIME NOT NULL COMMENT '提醒时间',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '提醒状态：pending/done/skipped',
  `source_record_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '来源健康记录 ID',
  `handler_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最近处理人 ID',
  `handled_at` DATETIME DEFAULT NULL COMMENT '最近处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_pet_reminders_pet_status_time` (`pet_id`, `status`, `remind_at`),
  KEY `idx_pet_reminders_handler` (`handler_user_id`),
  KEY `idx_pet_reminders_source` (`source_record_id`),
  CONSTRAINT `fk_pet_reminders_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`),
  CONSTRAINT `fk_pet_reminders_handler` FOREIGN KEY (`handler_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_pet_reminders_source` FOREIGN KEY (`source_record_id`) REFERENCES `pet_health_records` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宠物提醒表';

CREATE TABLE IF NOT EXISTS `reminder_templates` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `reminder_type` VARCHAR(30) NOT NULL COMMENT '提醒类型：vaccine/deworming/examination/medication/custom',
  `default_reminder_mode` VARCHAR(20) NOT NULL COMMENT '默认提醒模式：single/cycle',
  `default_advance_value` INT NOT NULL DEFAULT 0 COMMENT '默认提前量',
  `default_advance_unit` VARCHAR(20) NOT NULL DEFAULT 'day' COMMENT '默认提前单位：day/week/month',
  `default_cycle_value` INT DEFAULT NULL COMMENT '默认周期值',
  `default_cycle_unit` VARCHAR(20) DEFAULT NULL COMMENT '默认周期单位：day/week/month',
  `applicable_pet_type` VARCHAR(20) NOT NULL DEFAULT 'all' COMMENT '适用宠物类型：all/cat/dog/other',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-否 1-是',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '展示排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_reminder_templates_type_enabled` (`reminder_type`, `enabled`),
  KEY `idx_reminder_templates_pet_sort` (`applicable_pet_type`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提醒模板表';

CREATE TABLE IF NOT EXISTS `pet_daily_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `pet_id` BIGINT UNSIGNED NOT NULL COMMENT '宠物 ID',
  `author_user_id` BIGINT UNSIGNED NOT NULL COMMENT '记录人用户 ID',
  `media_list` JSON DEFAULT NULL COMMENT '媒体资产 ID 列表',
  `title` VARCHAR(100) DEFAULT NULL COMMENT '日常标题',
  `content` TEXT DEFAULT NULL COMMENT '日常描述',
  `scene_tags` JSON DEFAULT NULL COMMENT '场景标签列表',
  `mood_tags` JSON DEFAULT NULL COMMENT '状态标签列表',
  `visibility` VARCHAR(20) NOT NULL DEFAULT 'private' COMMENT '可见范围：private/family/public',
  `sync_to_community` TINYINT NOT NULL DEFAULT 0 COMMENT '是否同步社区：0-否 1-是',
  `sync_to_timeline` TINYINT NOT NULL DEFAULT 1 COMMENT '是否同步时间轴：0-否 1-是',
  `community_post_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '同步生成的社区帖子 ID',
  `happened_at` DATETIME NOT NULL COMMENT '记录发生时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_pet_daily_logs_pet_time` (`pet_id`, `happened_at` DESC),
  KEY `idx_pet_daily_logs_author` (`author_user_id`),
  KEY `idx_pet_daily_logs_visibility` (`visibility`),
  CONSTRAINT `fk_pet_daily_logs_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`),
  CONSTRAINT `fk_pet_daily_logs_author` FOREIGN KEY (`author_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='萌宠日常表';

CREATE TABLE IF NOT EXISTS `pet_timeline_events` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `pet_id` BIGINT UNSIGNED NOT NULL COMMENT '宠物 ID',
  `event_type` VARCHAR(30) NOT NULL COMMENT '事件类型：health/daily_log/service/device/memorial',
  `source_type` VARCHAR(30) NOT NULL COMMENT '来源表类型',
  `source_id` BIGINT UNSIGNED NOT NULL COMMENT '来源记录 ID',
  `event_time` DATETIME NOT NULL COMMENT '事件时间',
  `title` VARCHAR(100) NOT NULL COMMENT '事件标题',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '事件摘要',
  `cover_url` VARCHAR(255) DEFAULT NULL COMMENT '封面图地址',
  `visibility` VARCHAR(20) NOT NULL DEFAULT 'family' COMMENT '可见范围：private/family/public',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_timeline_source` (`source_type`, `source_id`),
  KEY `idx_pet_timeline_pet_time` (`pet_id`, `event_time` DESC),
  KEY `idx_pet_timeline_event_type` (`event_type`),
  CONSTRAINT `fk_pet_timeline_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宠物成长时间轴事件表';

-- ====================================================================
-- 社区
-- ====================================================================

CREATE TABLE IF NOT EXISTS `community_topics` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `topic_name` VARCHAR(100) NOT NULL COMMENT '话题名称',
  `topic_desc` VARCHAR(255) DEFAULT NULL COMMENT '话题描述',
  `city_code` VARCHAR(32) DEFAULT NULL COMMENT '同城话题城市编码',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 2-停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_community_topics_city` (`city_code`),
  KEY `idx_community_topics_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区话题表';

CREATE TABLE IF NOT EXISTS `community_posts` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '发布用户 ID',
  `pet_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联宠物 ID',
  `topic_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联话题 ID',
  `post_type` VARCHAR(20) NOT NULL COMMENT '帖子类型：image_text/video/qa/experience',
  `title` VARCHAR(100) DEFAULT NULL COMMENT '帖子标题',
  `content` TEXT DEFAULT NULL COMMENT '帖子正文',
  `media_list` JSON DEFAULT NULL COMMENT '媒体资产 ID 列表',
  `source_daily_log_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '来源萌宠日常 ID',
  `source_service_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '来源服务记录 ID',
  `source_product_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '来源商品记录 ID',
  `city_code` VARCHAR(32) DEFAULT NULL COMMENT '同城分发城市编码',
  `visibility` VARCHAR(20) NOT NULL DEFAULT 'public' COMMENT '可见性：public/follower/draft',
  `review_status` VARCHAR(20) NOT NULL DEFAULT 'pending_review' COMMENT '审核状态',
  `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
  `favorite_count` INT NOT NULL DEFAULT 0 COMMENT '收藏数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_community_posts_user_time` (`user_id`, `published_at` DESC),
  KEY `idx_community_posts_pet` (`pet_id`),
  KEY `idx_community_posts_topic` (`topic_id`),
  KEY `idx_community_posts_city_review_time` (`city_code`, `review_status`, `published_at` DESC),
  KEY `idx_community_posts_visibility` (`visibility`),
  CONSTRAINT `fk_community_posts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_community_posts_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`),
  CONSTRAINT `fk_community_posts_topic` FOREIGN KEY (`topic_id`) REFERENCES `community_topics` (`id`),
  CONSTRAINT `fk_community_posts_daily_log` FOREIGN KEY (`source_daily_log_id`) REFERENCES `pet_daily_logs` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区帖子表';

CREATE TABLE IF NOT EXISTS `community_comments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '帖子 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '评论用户 ID',
  `parent_comment_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论 ID',
  `content` VARCHAR(1000) NOT NULL COMMENT '评论内容',
  `status` VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '状态：normal/deleted/blocked',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_community_comments_post_time` (`post_id`, `created_at` ASC),
  KEY `idx_community_comments_user` (`user_id`),
  KEY `idx_community_comments_parent` (`parent_comment_id`),
  CONSTRAINT `fk_community_comments_post` FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`id`),
  CONSTRAINT `fk_community_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_community_comments_parent` FOREIGN KEY (`parent_comment_id`) REFERENCES `community_comments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区评论表';

CREATE TABLE IF NOT EXISTS `community_post_reactions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '帖子 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `reaction_type` VARCHAR(20) NOT NULL DEFAULT 'like' COMMENT '互动类型：like',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_community_post_reactions` (`post_id`, `user_id`, `reaction_type`),
  KEY `idx_community_post_reactions_user` (`user_id`),
  CONSTRAINT `fk_community_post_reactions_post` FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`id`),
  CONSTRAINT `fk_community_post_reactions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区帖子互动表';

CREATE TABLE IF NOT EXISTS `community_post_favorites` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '帖子 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_community_post_favorites` (`post_id`, `user_id`),
  KEY `idx_community_post_favorites_user` (`user_id`),
  CONSTRAINT `fk_community_post_favorites_post` FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`id`),
  CONSTRAINT `fk_community_post_favorites_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区帖子收藏表';

CREATE TABLE IF NOT EXISTS `community_reports` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `reporter_user_id` BIGINT UNSIGNED NOT NULL COMMENT '举报用户 ID',
  `target_type` VARCHAR(20) NOT NULL COMMENT '举报对象类型：post/comment/user',
  `target_id` BIGINT UNSIGNED NOT NULL COMMENT '举报对象 ID',
  `reason_code` VARCHAR(30) NOT NULL COMMENT '举报原因编码',
  `reason_detail` VARCHAR(500) DEFAULT NULL COMMENT '举报补充说明',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/processed/rejected',
  `processed_by` VARCHAR(64) DEFAULT NULL COMMENT '处理人标识',
  `admin_notes` VARCHAR(500) DEFAULT NULL COMMENT '管理员处理备注',
  `processed_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_community_reports_reporter` (`reporter_user_id`),
  KEY `idx_community_reports_target` (`target_type`, `target_id`),
  KEY `idx_community_reports_status` (`status`),
  CONSTRAINT `fk_community_reports_reporter` FOREIGN KEY (`reporter_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区举报表';

CREATE TABLE IF NOT EXISTS `user_follows` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `follower_user_id` BIGINT UNSIGNED NOT NULL COMMENT '关注人用户 ID',
  `followed_user_id` BIGINT UNSIGNED NOT NULL COMMENT '被关注用户 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follows_pair` (`follower_user_id`, `followed_user_id`),
  KEY `idx_user_follows_followed` (`followed_user_id`),
  CONSTRAINT `fk_user_follows_follower` FOREIGN KEY (`follower_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_user_follows_followed` FOREIGN KEY (`followed_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_user_follows_self` CHECK (`follower_user_id` <> `followed_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户关注关系表';

-- ====================================================================
-- 服务、医院、预约
-- ====================================================================

CREATE TABLE IF NOT EXISTS `service_city_configs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `city_code` VARCHAR(32) NOT NULL COMMENT '城市编码',
  `city_name` VARCHAR(50) NOT NULL COMMENT '城市名称',
  `opened` TINYINT NOT NULL DEFAULT 0 COMMENT '是否开通：0-否 1-是',
  `unavailable_reason` VARCHAR(255) DEFAULT NULL COMMENT '未开通原因',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '展示排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_service_city_configs_code` (`city_code`),
  KEY `idx_service_city_configs_opened_sort` (`opened`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务城市开通配置表';

CREATE TABLE IF NOT EXISTS `service_providers` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `provider_type` VARCHAR(20) NOT NULL COMMENT '服务商类型：hospital/boarding/grooming/training',
  `provider_name` VARCHAR(100) NOT NULL COMMENT '服务商名称',
  `city_code` VARCHAR(32) NOT NULL COMMENT '城市编码',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  `coordinate_source` VARCHAR(20) DEFAULT NULL COMMENT '坐标来源：manual/amap',
  `coordinate_updated_at` DATETIME DEFAULT NULL COMMENT '坐标更新时间',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `business_hours` VARCHAR(255) DEFAULT NULL COMMENT '营业时间',
  `rating_avg` DECIMAL(3,2) DEFAULT NULL COMMENT '平均评分',
  `review_count` INT NOT NULL DEFAULT 0 COMMENT '评价数量',
  `status` VARCHAR(20) NOT NULL DEFAULT 'online' COMMENT '状态：online/rest/offline',
  `ext_json` JSON DEFAULT NULL COMMENT '扩展信息，如医院科室、价格带',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_service_providers_type_city_status` (`provider_type`, `city_code`, `status`),
  KEY `idx_service_providers_location` (`city_code`, `latitude`, `longitude`),
  KEY `idx_service_providers_coordinate_source` (`coordinate_source`, `coordinate_updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务商表';

CREATE TABLE IF NOT EXISTS `provider_service_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `provider_id` BIGINT UNSIGNED NOT NULL COMMENT '服务商 ID',
  `service_code` VARCHAR(30) NOT NULL COMMENT '服务编码',
  `service_name` VARCHAR(100) NOT NULL COMMENT '服务名称',
  `service_desc` VARCHAR(500) DEFAULT NULL COMMENT '服务说明',
  `price_min` DECIMAL(10,2) DEFAULT NULL COMMENT '最低价格',
  `price_max` DECIMAL(10,2) DEFAULT NULL COMMENT '最高价格',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/inactive',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_service_items` (`provider_id`, `service_code`),
  CONSTRAINT `fk_provider_service_items_provider` FOREIGN KEY (`provider_id`) REFERENCES `service_providers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务商服务项目表';

CREATE TABLE IF NOT EXISTS `provider_schedule_slots` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `provider_id` BIGINT UNSIGNED NOT NULL COMMENT '服务商 ID',
  `appointment_type` VARCHAR(20) NOT NULL COMMENT '预约类型',
  `slot_date` DATE NOT NULL COMMENT '预约日期',
  `start_time` TIME NOT NULL COMMENT '开始时间',
  `end_time` TIME NOT NULL COMMENT '结束时间',
  `quota` INT NOT NULL DEFAULT 0 COMMENT '可预约名额',
  `booked_count` INT NOT NULL DEFAULT 0 COMMENT '已预约数量',
  `status` VARCHAR(20) NOT NULL DEFAULT 'open' COMMENT '状态：open/closed/full',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_schedule_slot` (`provider_id`, `appointment_type`, `slot_date`, `start_time`, `end_time`),
  KEY `idx_provider_schedule_lookup` (`provider_id`, `slot_date`, `status`),
  CONSTRAINT `fk_provider_schedule_provider` FOREIGN KEY (`provider_id`) REFERENCES `service_providers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务商预约时段表';

CREATE TABLE IF NOT EXISTS `service_appointments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '下单用户 ID',
  `pet_id` BIGINT UNSIGNED NOT NULL COMMENT '关联宠物 ID',
  `provider_id` BIGINT UNSIGNED NOT NULL COMMENT '服务商 ID',
  `appointment_type` VARCHAR(20) NOT NULL COMMENT '预约类型：hospital/boarding/grooming/training',
  `appointment_date` DATE NOT NULL COMMENT '预约日期',
  `appointment_slot` VARCHAR(50) DEFAULT NULL COMMENT '预约时间段',
  `demand_desc` VARCHAR(500) DEFAULT NULL COMMENT '需求描述',
  `contact_name` VARCHAR(50) NOT NULL COMMENT '联系人姓名',
  `contact_mobile` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending_confirm' COMMENT '预约状态',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_service_appointments_user_status_date` (`user_id`, `status`, `appointment_date` DESC),
  KEY `idx_service_appointments_pet` (`pet_id`),
  KEY `idx_service_appointments_provider` (`provider_id`, `appointment_date`),
  CONSTRAINT `fk_service_appointments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_service_appointments_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`),
  CONSTRAINT `fk_service_appointments_provider` FOREIGN KEY (`provider_id`) REFERENCES `service_providers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务预约表';

CREATE TABLE IF NOT EXISTS `provider_reviews` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `provider_id` BIGINT UNSIGNED NOT NULL COMMENT '服务商 ID',
  `appointment_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联预约 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '评价用户 ID',
  `pet_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联宠物 ID',
  `rating` TINYINT NOT NULL COMMENT '评分：1-5',
  `content` VARCHAR(1000) DEFAULT NULL COMMENT '评价内容',
  `images` JSON DEFAULT NULL COMMENT '评价图片列表',
  `status` VARCHAR(20) NOT NULL DEFAULT 'visible' COMMENT '状态：visible/hidden',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_provider_reviews_provider_time` (`provider_id`, `created_at` DESC),
  KEY `idx_provider_reviews_user` (`user_id`),
  CONSTRAINT `fk_provider_reviews_provider` FOREIGN KEY (`provider_id`) REFERENCES `service_providers` (`id`),
  CONSTRAINT `fk_provider_reviews_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `service_appointments` (`id`),
  CONSTRAINT `fk_provider_reviews_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_provider_reviews_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务商评价表';

-- ====================================================================
-- 商城、购物车、订单
-- ====================================================================

CREATE TABLE IF NOT EXISTS `products` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `product_name` VARCHAR(150) NOT NULL COMMENT '商品名称',
  `category_code` VARCHAR(32) NOT NULL COMMENT '商品分类编码',
  `pet_type` VARCHAR(20) DEFAULT NULL COMMENT '适用品类',
  `age_stage` VARCHAR(20) DEFAULT NULL COMMENT '适用年龄阶段',
  `brand_name` VARCHAR(50) DEFAULT NULL COMMENT '品牌名称',
  `main_image` VARCHAR(255) DEFAULT NULL COMMENT '主图地址',
  `detail_images` JSON DEFAULT NULL COMMENT '详情图列表',
  `status` VARCHAR(20) NOT NULL DEFAULT 'online' COMMENT '商品状态：online/offline',
  `description` TEXT DEFAULT NULL COMMENT '商品详情',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_products_category_status` (`category_code`, `status`),
  KEY `idx_products_pet_type_stage` (`pet_type`, `age_stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品表';

CREATE TABLE IF NOT EXISTS `product_skus` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品 ID',
  `sku_code` VARCHAR(64) NOT NULL COMMENT 'SKU 编码',
  `sku_name` VARCHAR(100) NOT NULL COMMENT 'SKU 名称',
  `sale_price` DECIMAL(10,2) NOT NULL COMMENT '销售价',
  `market_price` DECIMAL(10,2) DEFAULT NULL COMMENT '市场价',
  `stock_qty` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  `weight_g` INT DEFAULT NULL COMMENT '重量（克）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'SKU 状态：active/inactive',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_skus_code` (`sku_code`),
  UNIQUE KEY `uk_product_skus_product_code` (`product_id`, `sku_code`),
  KEY `idx_product_skus_status` (`status`),
  CONSTRAINT `fk_product_skus_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品 SKU 表';

CREATE TABLE IF NOT EXISTS `cart_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `pet_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联宠物 ID',
  `sku_id` BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
  `checked` TINYINT NOT NULL DEFAULT 1 COMMENT '是否勾选：0-否 1-是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_cart_items_user` (`user_id`),
  KEY `idx_cart_items_sku` (`sku_id`),
  KEY `idx_cart_items_pet` (`pet_id`),
  CONSTRAINT `fk_cart_items_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_cart_items_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`),
  CONSTRAINT `fk_cart_items_sku` FOREIGN KEY (`sku_id`) REFERENCES `product_skus` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车表';

CREATE TABLE IF NOT EXISTS `user_addresses` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
  `receiver_mobile` VARCHAR(20) NOT NULL COMMENT '收货人手机号',
  `province_code` VARCHAR(32) DEFAULT NULL COMMENT '省份编码',
  `city_code` VARCHAR(32) DEFAULT NULL COMMENT '城市编码',
  `district_code` VARCHAR(32) DEFAULT NULL COMMENT '区县编码',
  `detail_address` VARCHAR(255) NOT NULL COMMENT '详细地址',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址：0-否 1-是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_addresses_user` (`user_id`, `is_default`),
  CONSTRAINT `fk_user_addresses_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收货地址表';

CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '下单用户 ID',
  `pet_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联宠物 ID',
  `address_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '收货地址 ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `order_status` VARCHAR(20) NOT NULL DEFAULT 'pending_pay' COMMENT '订单状态',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实际支付金额',
  `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
  `receiver_mobile` VARCHAR(20) NOT NULL COMMENT '收货人手机号',
  `receiver_address` VARCHAR(255) NOT NULL COMMENT '收货地址快照',
  `buyer_remark` VARCHAR(255) DEFAULT NULL COMMENT '买家备注',
  `pay_at` DATETIME DEFAULT NULL COMMENT '支付时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_orders_order_no` (`order_no`),
  KEY `idx_orders_user_status_time` (`user_id`, `order_status`, `created_at` DESC),
  KEY `idx_orders_pet` (`pet_id`),
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_orders_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`),
  CONSTRAINT `fk_orders_address` FOREIGN KEY (`address_id`) REFERENCES `user_addresses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `order_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品 ID',
  `sku_id` BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
  `product_name` VARCHAR(150) NOT NULL COMMENT '商品名称快照',
  `sku_name` VARCHAR(100) NOT NULL COMMENT 'SKU 名称快照',
  `sale_price` DECIMAL(10,2) NOT NULL COMMENT '成交单价',
  `quantity` INT NOT NULL COMMENT '购买数量',
  `main_image` VARCHAR(255) DEFAULT NULL COMMENT '主图快照',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_items_order` (`order_id`),
  KEY `idx_order_items_product` (`product_id`),
  CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_order_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_order_items_sku` FOREIGN KEY (`sku_id`) REFERENCES `product_skus` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细表';

CREATE TABLE IF NOT EXISTS `order_status_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
  `from_status` VARCHAR(20) DEFAULT NULL COMMENT '原状态',
  `to_status` VARCHAR(20) NOT NULL COMMENT '目标状态',
  `operator_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作者用户 ID',
  `operator_type` VARCHAR(20) NOT NULL DEFAULT 'system' COMMENT '操作者类型：system/user/admin',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '状态流转备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_status_logs_order` (`order_id`, `created_at` DESC),
  CONSTRAINT `fk_order_status_logs_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_order_status_logs_user` FOREIGN KEY (`operator_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单状态流水表';

-- ====================================================================
-- 设备
-- ====================================================================

CREATE TABLE IF NOT EXISTS `devices` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `device_sn` VARCHAR(64) NOT NULL COMMENT '设备序列号',
  `device_type` VARCHAR(30) NOT NULL COMMENT '设备类型：feeder/water_fountain/litter_box/camera',
  `brand_name` VARCHAR(50) DEFAULT NULL COMMENT '品牌名称',
  `model_name` VARCHAR(50) DEFAULT NULL COMMENT '型号名称',
  `firmware_version` VARCHAR(50) DEFAULT NULL COMMENT '固件版本',
  `online_status` VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '在线状态：online/offline/alert',
  `last_online_at` DATETIME DEFAULT NULL COMMENT '最近在线时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_devices_sn` (`device_sn`),
  KEY `idx_devices_type_status` (`device_type`, `online_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备主表';

CREATE TABLE IF NOT EXISTS `device_bindings` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `device_id` BIGINT UNSIGNED NOT NULL COMMENT '设备 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '绑定用户 ID',
  `pet_id` BIGINT UNSIGNED NOT NULL COMMENT '关联宠物 ID',
  `bind_name` VARCHAR(50) DEFAULT NULL COMMENT '设备自定义名称',
  `room_name` VARCHAR(50) DEFAULT NULL COMMENT '房间名称',
  `bind_status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '绑定状态：pending/active/unbound',
  `bound_at` DATETIME DEFAULT NULL COMMENT '绑定时间',
  `unbound_at` DATETIME DEFAULT NULL COMMENT '解绑时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_bindings_device` (`device_id`),
  KEY `idx_device_bindings_user` (`user_id`),
  KEY `idx_device_bindings_pet_status` (`pet_id`, `bind_status`),
  CONSTRAINT `fk_device_bindings_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`),
  CONSTRAINT `fk_device_bindings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_device_bindings_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备绑定表';

CREATE TABLE IF NOT EXISTS `device_snapshots` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `device_id` BIGINT UNSIGNED NOT NULL COMMENT '设备 ID',
  `pet_id` BIGINT UNSIGNED NOT NULL COMMENT '宠物 ID',
  `snapshot_time` DATETIME NOT NULL COMMENT '快照时间',
  `online_status` VARCHAR(20) NOT NULL COMMENT '在线状态',
  `health_status` VARCHAR(20) DEFAULT NULL COMMENT '设备健康状态',
  `metrics_json` JSON DEFAULT NULL COMMENT '归一化指标快照',
  `last_event_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最近设备事件 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_snapshots_device` (`device_id`),
  KEY `idx_device_snapshots_pet` (`pet_id`),
  CONSTRAINT `fk_device_snapshots_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`),
  CONSTRAINT `fk_device_snapshots_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备快照表';

CREATE TABLE IF NOT EXISTS `device_events` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `device_id` BIGINT UNSIGNED NOT NULL COMMENT '设备 ID',
  `pet_id` BIGINT UNSIGNED NOT NULL COMMENT '关联宠物 ID',
  `dedupe_key` VARCHAR(128) NOT NULL COMMENT '去重键',
  `event_code` VARCHAR(50) NOT NULL COMMENT '事件编码',
  `event_type` VARCHAR(30) NOT NULL COMMENT '事件类型：feed/drink/clean/offline/alert',
  `event_time` DATETIME NOT NULL COMMENT '事件发生时间',
  `event_value` VARCHAR(100) DEFAULT NULL COMMENT '事件值',
  `severity_level` VARCHAR(20) DEFAULT NULL COMMENT '严重程度：normal/warn/critical',
  `raw_payload` JSON DEFAULT NULL COMMENT '原始上报内容',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_events_dedupe` (`dedupe_key`),
  KEY `idx_device_events_device_time` (`device_id`, `event_time` DESC),
  KEY `idx_device_events_pet_time` (`pet_id`, `event_time` DESC),
  KEY `idx_device_events_type` (`event_type`, `severity_level`),
  CONSTRAINT `fk_device_events_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`),
  CONSTRAINT `fk_device_events_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备事件表';

-- ====================================================================
-- 通知、审核、异步事件、审计
-- ====================================================================

CREATE TABLE IF NOT EXISTS `notifications` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收用户 ID',
  `notify_type` VARCHAR(30) NOT NULL COMMENT '通知类型：system/interaction/appointment/order/device/reminder',
  `biz_type` VARCHAR(30) DEFAULT NULL COMMENT '业务类型',
  `biz_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '业务主键 ID',
  `title` VARCHAR(100) NOT NULL COMMENT '通知标题',
  `content` VARCHAR(500) NOT NULL COMMENT '通知内容',
  `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '已读状态：0-未读 1-已读',
  `sent_at` DATETIME NOT NULL COMMENT '发送时间',
  `read_at` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_notifications_user_read_time` (`user_id`, `read_status`, `sent_at` DESC),
  KEY `idx_notifications_biz` (`biz_type`, `biz_id`),
  CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知表';

CREATE TABLE IF NOT EXISTS `message_templates` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `template_code` VARCHAR(64) NOT NULL COMMENT '模板编码',
  `channel_type` VARCHAR(20) NOT NULL COMMENT '渠道类型：sms/push/inbox',
  `title_template` VARCHAR(100) DEFAULT NULL COMMENT '标题模板',
  `content_template` TEXT NOT NULL COMMENT '内容模板',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/inactive',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_templates_code` (`template_code`, `channel_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息模板表';

CREATE TABLE IF NOT EXISTS `notification_channel_configs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `channel_type` VARCHAR(20) NOT NULL COMMENT '渠道类型：inbox/sms/push',
  `provider_code` VARCHAR(64) NOT NULL COMMENT '供应商编码',
  `provider_name` VARCHAR(100) NOT NULL COMMENT '供应商名称',
  `enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用：0-否 1-是',
  `config_status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '配置状态：draft/ready/disabled',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_channel_provider` (`channel_type`, `provider_code`),
  KEY `idx_notification_channel_enabled` (`channel_type`, `enabled`),
  KEY `idx_notification_channel_status` (`config_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知渠道配置表';

CREATE TABLE IF NOT EXISTS `moderation_tasks` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `target_type` VARCHAR(30) NOT NULL COMMENT '目标类型：community_post/community_question',
  `target_id` BIGINT UNSIGNED NOT NULL COMMENT '目标 ID',
  `content_type` VARCHAR(30) NOT NULL COMMENT '内容形态：text/image_text/video/qa',
  `content_snapshot` JSON NOT NULL COMMENT '审核内容快照',
  `provider_code` VARCHAR(64) NOT NULL COMMENT '审核供应商编码',
  `review_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/approved/rejected/failed',
  `review_result` JSON DEFAULT NULL COMMENT '审核结果',
  `risk_labels` JSON DEFAULT NULL COMMENT '风险标签',
  `failure_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `callback_payload` JSON DEFAULT NULL COMMENT '供应商回调载荷',
  `reviewed_at` DATETIME DEFAULT NULL COMMENT '审核完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_moderation_tasks_target` (`target_type`, `target_id`),
  KEY `idx_moderation_tasks_status` (`review_status`, `created_at`),
  KEY `idx_moderation_tasks_provider` (`provider_code`, `review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审核任务表';

CREATE TABLE IF NOT EXISTS `user_push_device_tokens` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  `platform` VARCHAR(20) NOT NULL COMMENT '客户端平台：ios/android',
  `provider_code` VARCHAR(64) NOT NULL COMMENT 'Push 供应商编码',
  `device_token` VARCHAR(512) NOT NULL COMMENT '设备 Token',
  `device_id` VARCHAR(128) DEFAULT NULL COMMENT '客户端设备标识',
  `app_version` VARCHAR(40) DEFAULT NULL COMMENT 'App 版本',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-否 1-是',
  `last_registered_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近注册时间',
  `unregistered_at` DATETIME DEFAULT NULL COMMENT '解绑时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_push_device_provider_token` (`provider_code`, `device_token`),
  KEY `idx_push_device_user_enabled` (`user_id`, `enabled`),
  CONSTRAINT `fk_push_device_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户 Push 设备 Token 表';

CREATE TABLE IF NOT EXISTS `push_tasks` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收用户 ID',
  `notification_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联站内通知 ID',
  `notify_type` VARCHAR(30) NOT NULL COMMENT '通知类型',
  `biz_type` VARCHAR(50) DEFAULT NULL COMMENT '业务类型',
  `biz_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '业务 ID',
  `title` VARCHAR(100) NOT NULL COMMENT 'Push 标题',
  `content` VARCHAR(500) NOT NULL COMMENT 'Push 内容',
  `provider_code` VARCHAR(64) NOT NULL COMMENT 'Push 供应商编码',
  `task_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '任务状态：pending/skipped/failed/sent',
  `failure_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败或跳过原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_push_tasks_user_status` (`user_id`, `task_status`, `created_at`),
  KEY `idx_push_tasks_notification` (`notification_id`),
  KEY `idx_push_tasks_provider_status` (`provider_code`, `task_status`),
  CONSTRAINT `fk_push_tasks_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_push_tasks_notification` FOREIGN KEY (`notification_id`) REFERENCES `notifications` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Push 任务表';

CREATE TABLE IF NOT EXISTS `push_delivery_records` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `push_task_id` BIGINT UNSIGNED NOT NULL COMMENT 'Push 任务 ID',
  `device_token_id` BIGINT UNSIGNED NOT NULL COMMENT '设备 Token ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收用户 ID',
  `provider_code` VARCHAR(64) NOT NULL COMMENT 'Push 供应商编码',
  `delivery_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '投递状态：pending/skipped/failed/sent',
  `failure_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `attempted_at` DATETIME DEFAULT NULL COMMENT '投递尝试时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_push_delivery_task` (`push_task_id`),
  KEY `idx_push_delivery_user_status` (`user_id`, `delivery_status`, `created_at`),
  KEY `idx_push_delivery_provider_status` (`provider_code`, `delivery_status`),
  CONSTRAINT `fk_push_delivery_task` FOREIGN KEY (`push_task_id`) REFERENCES `push_tasks` (`id`),
  CONSTRAINT `fk_push_delivery_token` FOREIGN KEY (`device_token_id`) REFERENCES `user_push_device_tokens` (`id`),
  CONSTRAINT `fk_push_delivery_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Push 投递记录表';

CREATE TABLE IF NOT EXISTS `outbox_events` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `aggregate_type` VARCHAR(30) NOT NULL COMMENT '聚合类型',
  `aggregate_id` BIGINT UNSIGNED NOT NULL COMMENT '聚合 ID',
  `event_type` VARCHAR(100) NOT NULL COMMENT '事件类型',
  `payload_json` JSON NOT NULL COMMENT '事件负载',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/processing/success/failed',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `next_retry_at` DATETIME DEFAULT NULL COMMENT '下次重试时间',
  `published_at` DATETIME DEFAULT NULL COMMENT '消费完成时间',
  `last_error` VARCHAR(500) DEFAULT NULL COMMENT '最近失败原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_outbox_events_status_retry` (`status`, `next_retry_at`),
  KEY `idx_outbox_events_aggregate` (`aggregate_type`, `aggregate_id`),
  KEY `idx_outbox_events_type_time` (`event_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='事务外发事件表';

CREATE TABLE IF NOT EXISTS `audit_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `operator_type` VARCHAR(20) NOT NULL COMMENT '操作者类型：system/user/admin',
  `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作者标识',
  `target_type` VARCHAR(30) NOT NULL COMMENT '目标类型',
  `target_id` VARCHAR(64) NOT NULL COMMENT '目标标识',
  `action` VARCHAR(50) NOT NULL COMMENT '操作动作',
  `detail_json` JSON DEFAULT NULL COMMENT '操作详情',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT '操作 IP',
  `user_agent` VARCHAR(255) DEFAULT NULL COMMENT '客户端标识',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_audit_logs_target` (`target_type`, `target_id`, `created_at` DESC),
  KEY `idx_audit_logs_operator` (`operator_type`, `operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审计日志表';

-- ====================================================================
-- DDL 结束
-- 说明：
-- 1. 该版本为当前项目数据库结构参考基线，服务端实现默认以本文件所定义表结构为准。
-- 2. 后续若采用分库分表或应用侧发号，可将 AUTO_INCREMENT 切换为应用生成 ID。
-- 3. 后台 RBAC、统计数仓、推荐召回等表不在当前草案范围内。
-- ====================================================================
