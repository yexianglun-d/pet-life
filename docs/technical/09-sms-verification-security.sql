-- 短信验证码安全底座增量 SQL 草案
-- 背景：移除固定验证码 123456，服务端生成随机验证码并只保存摘要。
-- 说明：本轮只落地供应商无关底座，默认 `dev_noop` 供应商只记录发送受理状态，不接真实短信 SDK。

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
