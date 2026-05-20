-- PetLife 高德地图与定位底座增量 SQL 草案
-- 说明：
-- 1. 不包含任何高德 Web 服务 Key，Key 仅通过 PETLIFE_AMAP_WEB_SERVICE_KEY 注入服务端配置。
-- 2. 本轮只维护服务商坐标元数据，真实导航、路径规划和前端地图组件不在本 SQL 范围内。

ALTER TABLE `service_providers`
  ADD COLUMN `coordinate_source` VARCHAR(20) DEFAULT NULL COMMENT '坐标来源：manual/amap' AFTER `longitude`,
  ADD COLUMN `coordinate_updated_at` DATETIME DEFAULT NULL COMMENT '坐标更新时间' AFTER `coordinate_source`;

CREATE INDEX `idx_service_providers_coordinate_source`
  ON `service_providers` (`coordinate_source`, `coordinate_updated_at`);
