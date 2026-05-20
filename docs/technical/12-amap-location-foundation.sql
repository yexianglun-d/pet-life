-- PetLife 高德地图与定位底座增量 SQL 草案
-- 说明：
-- 1. 不包含任何高德 Web 服务 Key，Key 仅通过 PETLIFE_AMAP_WEB_SERVICE_KEY 注入服务端配置。
-- 2. 本轮只维护服务商坐标元数据，真实导航、路径规划和前端地图组件不在本 SQL 范围内。
-- 3. 该脚本允许在字段已存在但索引缺失的库上重复执行，用于修复历史环境只执行了部分变更的情况。

SET @coordinate_source_column_count = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'service_providers'
    AND COLUMN_NAME = 'coordinate_source'
);

SET @ddl = IF(
  @coordinate_source_column_count = 0,
  'ALTER TABLE `service_providers` ADD COLUMN `coordinate_source` VARCHAR(20) DEFAULT NULL COMMENT ''坐标来源：manual/amap'' AFTER `longitude`',
  'SELECT ''coordinate_source exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @coordinate_updated_at_column_count = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'service_providers'
    AND COLUMN_NAME = 'coordinate_updated_at'
);

SET @ddl = IF(
  @coordinate_updated_at_column_count = 0,
  'ALTER TABLE `service_providers` ADD COLUMN `coordinate_updated_at` DATETIME DEFAULT NULL COMMENT ''坐标更新时间'' AFTER `coordinate_source`',
  'SELECT ''coordinate_updated_at exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @coordinate_source_index_count = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'service_providers'
    AND INDEX_NAME = 'idx_service_providers_coordinate_source'
);

SET @ddl = IF(
  @coordinate_source_index_count = 0,
  'CREATE INDEX `idx_service_providers_coordinate_source` ON `service_providers` (`coordinate_source`, `coordinate_updated_at`)',
  'SELECT ''idx_service_providers_coordinate_source exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
