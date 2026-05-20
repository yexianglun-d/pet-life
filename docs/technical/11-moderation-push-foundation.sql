-- 内容审核底座 + Push 推送底座增量 SQL 草案。
-- 说明：本轮只落地任务、状态和供应商抽象，不接第三方审核或 Push SDK。

ALTER TABLE `moderation_tasks`
  ADD COLUMN `content_type` VARCHAR(30) NOT NULL DEFAULT 'text' COMMENT '内容形态：text/image_text/video/qa' AFTER `target_id`,
  ADD COLUMN `content_snapshot` JSON NOT NULL COMMENT '审核内容快照' AFTER `content_type`,
  ADD COLUMN `provider_code` VARCHAR(64) NOT NULL DEFAULT 'dev_noop' COMMENT '审核供应商编码' AFTER `content_snapshot`,
  ADD COLUMN `risk_labels` JSON DEFAULT NULL COMMENT '风险标签' AFTER `review_result`,
  ADD COLUMN `failure_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因' AFTER `risk_labels`,
  ADD COLUMN `callback_payload` JSON DEFAULT NULL COMMENT '供应商回调载荷' AFTER `failure_reason`,
  ADD COLUMN `reviewed_at` DATETIME DEFAULT NULL COMMENT '审核完成时间' AFTER `callback_payload`;

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
