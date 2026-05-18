# Technical Design Docs

## 文档说明

该目录用于承接 `宠物生活管家` 的技术设计阶段文档，目标是把现有产品方案转成研发可执行的系统设计、接口契约和工程边界。

## 文档结构

1. [01-system-design.md](./01-system-design.md)
   全量技术设计主文档，包含系统架构、模块边界、数据所有权、核心流程、第三方集成、安全边界、性能与演进策略。
2. [02-api-and-events.md](./02-api-and-events.md)
   API 契约、分页与鉴权约定、核心接口清单、关键领域事件和失败码约定。
3. [03-ddl-draft.sql](./03-ddl-draft.sql)
   MySQL 8 DDL 草案，覆盖核心业务表、技术支撑表、索引、唯一约束和关键外键关系。
4. [04-execution-plan.md](./04-execution-plan.md)
   技术实施计划，包含建设主线、阶段拆分、任务清单、验收标准、测试计划、交付检查点和数据库接入建议。
5. [05-persistence-integration.md](./05-persistence-integration.md)
   数据库唯一数据源说明、MyBatis 持久化边界、运行配置与当前验证结果。
6. [06-server-code-structure.md](./06-server-code-structure.md)
   服务端分层结构、命名约定、层间边界和注释规范。

## 与产品文档的关系

- 产品目标与范围：`docs/product/01-product-overview.md`
- 信息架构与页面清单：`docs/product/02-information-architecture.md`
- 页面级 PRD：`docs/product/03-prd-core-pages.md`
- 产品层业务实体模型：`docs/product/05-business-entities.md`

## 当前技术设计原则

1. 先保证 `宠物主轴` 和核心链路正确，不为了未来规模过度拆分。
2. v1 采用 `模块化单体 + 异步任务`，不直接上微服务。
3. 当前批次同步推进用户端、服务端和后台管理端；商城仅保留占位页，设备仅保留技术预留。
4. 时间轴、通知、审核统一采用 `事件驱动的派生处理`，设备事件设计保留到后续接入阶段。
