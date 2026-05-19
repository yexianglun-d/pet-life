-- 通知与消息配置闭环增量 SQL 草案
-- 背景：message_templates 已在全量 DDL 草案中存在；通知发送渠道配置此前没有独立表。
-- 说明：本轮只落地配置管理和站内消息模板渲染，不接真实短信、Push 或第三方 SDK。

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
