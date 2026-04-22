# Persistence Integration

## 目标

当前服务端已经切换为数据库唯一数据源，不再保留开发期内存适配器，也不再依赖 Flyway 自动建表。

数据库结构以 [03-ddl-draft.sql](./03-ddl-draft.sql) 为准，并默认连接 `PETLIFE_DATASOURCE_*` 环境变量指定的 MySQL 实例。

## 已补充内容

### 1. 运行时数据库配置

- `server/src/main/resources/application.yml`
  默认启用 MySQL DataSource 与 MyBatis 驼峰映射。
- `server/src/main/java/com/petlife/server/bootstrap/PetLifeServerApplication.java`
  使用 `@MapperScan` 扫描正式 MyBatis Mapper。

### 2. 当前落库范围

- 用户与认证：`users`、`user_settings`、`user_sessions`
- 家庭共养：`families`、`family_members`
- 宠物主档：`pets`
- 健康档案：`pet_health_records`
- 疫苗、驱虫、体检、洗护等提醒：`pet_reminders`
- 萌宠日常：`pet_daily_logs`
- 成长时间轴派生事件：`pet_timeline_events`

### 3. 明确未进入本批迁移的范围

- 商城商品、购物车、订单真实后端
- 智能设备厂商绑定、设备数据上报
- 医院、洗护、寄养、训练真实预约后端
- 社区完整发帖、评论、点赞、审核链路

这些模块在当前产品策略中仍以真实页面占位或后续迭代为主，不提前创建强耦合数据表。

## 持久化 Mapper 边界

当前已补齐以下 MyBatis Mapper 骨架：

- `AuthTokenPersistenceMapper`：登录会话写入、校验、活跃时间更新、吊销。
- `UserPersistenceMapper`：用户资料、手机号查用户、家庭摘要、当前宠物切换。
- `PetPersistenceMapper`：宠物列表、详情、创建、更新。
- `HealthRecordPersistenceMapper`：健康记录列表与新增。
- `ReminderPersistenceMapper`：提醒列表、新增、完成。
- `DailyLogPersistenceMapper`：萌宠日常列表与新增。

这些 Mapper 已经接入当前应用服务，不再只是骨架。

当前统一约定：

- Mapper 查询输出统一为 `DataObject`，不直接返回接口响应对象。
- 应用服务通过 `converter` 将 `DataObject` 转为 `Entity` 后再参与业务编排。
- 控制器对外只暴露 `Response DTO`，不泄漏数据库字段语义。

## 已接入的数据库实现

当前已完成以下数据库主链路切换：

- 登录：`AuthApplicationService` 基于 `users`、`families`、`family_members`、`pets`、`user_settings`、`user_sessions` 完成登录初始化。
- 鉴权：`DevelopmentTokenAuthenticationFilter` 从 Bearer Token 中解析会话 ID，并通过 `user_sessions` 校验登录态。
- 当前用户：`UserApplicationService` 从数据库加载当前用户、当前宠物和家庭摘要。
- 宠物：`PetApplicationService` 基于数据库完成列表、创建、详情、更新、摘要聚合。
- 健康记录：`HealthApplicationService` 已切换到 `pet_health_records`。
- 提醒：`ReminderApplicationService` 已切换到 `pet_reminders`。
- 萌宠日常：`DailyLogApplicationService` 已切换到 `pet_daily_logs`。

## 当前运行时边界

当前没有内存回退模式，也没有 `db` Profile。

这意味着：

- 本地启动和自动化测试都要求 MySQL 可连。
- 数据库建表由人工执行 DDL，不由应用自动迁移。
- 如果数据库不可用，服务端不会退回内存模式。

## 本地数据库启动方式

启动服务前，需要先确认目标 MySQL 已执行 `03-ddl-draft.sql` 中的建表脚本，并提供可连通的数据库连接信息。

推荐通过环境变量传入：

```bash
export PETLIFE_DATASOURCE_URL='jdbc:mysql://<host>:3306/pet_life?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_0900_ai_ci&serverTimezone=Asia/Shanghai'
export PETLIFE_DATASOURCE_USERNAME='<username>'
export PETLIFE_DATASOURCE_PASSWORD='<password>'
mvn spring-boot:run
```

## 当前验证结果

- 远程测试库已完成 JDBC 连通性验证。
- `mvn -Dmaven.repo.local=/tmp/petlife-m2 test` 已在远程测试库上通过。

## 下一步

1. 继续在既定 `DataObject -> Entity -> Response` 结构上扩写业务接口。
2. 用户端和后台接入真实接口时复用现有应用服务，不绕开领域转换层。
3. 后续若需要数据库版本管理，再重新引入迁移工具，但应以当前线上表结构为基线，不再回退到旧的迁移草案。
