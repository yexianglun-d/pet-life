-- 后台真实账号与会话升级脚本
-- 适用场景：已经执行过 03-ddl-draft.sql 的测试库，需要补齐本轮后台真实登录能力。

USE `pet_life`;

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

-- 管理员账号密码必须使用 BCrypt 摘要写入，不在 SQL 中保存明文密码。
-- 示例：
-- INSERT INTO admin_accounts (username, password_hash, display_name, role_code, status)
-- VALUES ('admin', '<BCryptHash>', '系统管理员', 'super_admin', 1)
-- ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), status = 1, deleted_at = NULL;
