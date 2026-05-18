# 服务端线程交接文档

## 记录规则

- 本文件记录 `petlife` 服务端线程每次完成需求、修复问题、调整设计或发现缺口后的交接信息。
- 每次记录必须包含：新完成内容、新增/修改文件、验证命令与结果、未完成事项、风险或阻塞、下一步建议。
- 如功能状态变化，同步更新 `docs/project/02-feature-completion-checklist.md` 和 `docs/project/01-current-delivery-status.md`。
- 按完整交付标准记录，不使用“核心可用”等阶段性表述。

## 2026-05-18 后台提醒查询接口补齐

### 新完成内容

- 新增 `GET /api/v1/admin/reminders`，支持按关键词、状态、提醒类型、提醒模式、宠物、家庭、主人、处理人、来源健康记录、提醒时间范围查询系统提醒。
- 新增 `GET /api/v1/admin/reminders/{reminderId}`，返回单条提醒详情。
- 响应返回提醒主体、宠物归属、处理人和来源健康记录上下文；接口层统一将数据库 `done` 状态回显为 `completed`。
- 后台提醒查询走真实 `pet_reminders`、`pets`、`families`、`users`、`pet_health_records` 数据，不做假数据、不走内存。
- 新增服务端测试覆盖列表、详情、组合筛选、来源健康记录筛选和无 token 权限边界。
- 已同步 OpenAPI、技术接口说明、后台接口缺口清单、功能完成清单和当前交付状态。

### 新增/修改文件

- 新增服务端文件：
  - `server/src/main/java/com/petlife/server/modules/reminder/controller/AdminReminderController.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/converter/AdminReminderConverter.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/domain/entity/AdminReminderEntity.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/domain/entity/AdminReminderSourceEntity.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/dto/response/AdminReminderResponse.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/dto/response/AdminReminderSourceResponse.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/persistence/dataobject/AdminReminderDataObject.java`
- 修改服务端文件：
  - `server/src/main/java/com/petlife/server/modules/reminder/persistence/ReminderPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/service/ReminderApplicationService.java`
  - `server/src/test/java/com/petlife/server/bootstrap/PhaseOneApiTests.java`
- 修改文档：
  - `docs/api/petlife-openapi.yaml`
  - `docs/technical/02-api-and-events.md`
  - `docs/project/01-current-delivery-status.md`
  - `docs/project/02-feature-completion-checklist.md`
  - `docs/project/04-admin-web-api-gap-list.md`
  - `docs/project/server-thread-summary.md`
  - `docs/project/admin-web-thread-summary.md`
  - `docs/project/mobile-app-thread-summary.md`

### 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`
  - 结果：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`
  - 结果：通过，`Tests run: 62, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`
  - 结果：通过。
- `git diff --check`
  - 结果：通过。

### 未完成事项

- 后台真实账号体系、角色权限边界、登录、退出、刷新和管理端审计身份仍未完成。
- 提醒模板模型和模板管理接口未定义，本轮未新增提醒模板写能力。
- admin-web 尚未接入提醒后台页面。
- 消息模板管理、通知发送配置模型和维护接口仍未完成。

### 风险或阻塞

- 当前后台提醒接口仍复用现有 Bearer 鉴权过滤器，只验证登录态；真实后台账号和后台角色权限需后续单独补齐。
- 列表接口当前按提醒时间倒序返回最多 200 条，后续如后台页面需要翻页，需要在 OpenAPI 中明确分页契约后再扩展。
- 来源记录上下文当前只覆盖健康记录派生提醒；其他来源类型在 DDL 或模型扩展前不能提前硬编码。

### 下一步建议

1. admin-web 可按 OpenAPI 接入系统提醒查询页面，先只做列表、详情和筛选。
2. 服务端下一轮优先设计提醒模板模型，再实现模板列表、新增、编辑、启停接口。
3. 后台真实账号体系应单独作为一轮设计与实现，先明确角色权限和审计写入规则。

## 2026-05-18 后台治理查询接口补齐

### 新完成内容

- 审核与举报处理：
  - `PATCH /api/v1/admin/moderation/reports/{reportId}` 支持 `admin_notes` 入库、回显。
  - 新增 `GET /api/v1/admin/moderation/audit-logs`，用于查询审核处理审计日志。
- 当前用户与个人中心：
  - 新增 `GET /api/v1/admin/users`。
  - 新增 `GET /api/v1/admin/users/{userId}`。
  - 返回用户资料、城市、通知设置、主要家庭和当前宠物上下文。
- 家庭共养：
  - 新增 `GET /api/v1/admin/families`。
  - 新增 `GET /api/v1/admin/families/{familyId}`。
  - 返回家庭基础信息、拥有者、成员关系和家庭宠物列表。
- 宠物主档：
  - 新增 `GET /api/v1/admin/pets`。
  - 新增 `GET /api/v1/admin/pets/{petId}`。
  - 返回宠物详情、主人上下文和家庭归属上下文。
- 已同步 OpenAPI、技术说明、后台接口缺口清单、功能完成清单和当前交付状态。

### 新增/修改文件

- 新增服务端文件：
  - `server/src/main/java/com/petlife/server/modules/user/controller/AdminUserController.java`
  - `server/src/main/java/com/petlife/server/modules/user/converter/AdminUserConverter.java`
  - `server/src/main/java/com/petlife/server/modules/user/domain/entity/AdminUserEntity.java`
  - `server/src/main/java/com/petlife/server/modules/user/dto/response/AdminUserFamilyResponse.java`
  - `server/src/main/java/com/petlife/server/modules/user/dto/response/AdminUserResponse.java`
  - `server/src/main/java/com/petlife/server/modules/user/dto/response/AdminUserSettingsResponse.java`
  - `server/src/main/java/com/petlife/server/modules/user/persistence/dataobject/AdminUserDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/family/controller/AdminFamilyController.java`
  - `server/src/main/java/com/petlife/server/modules/family/converter/AdminFamilyConverter.java`
  - `server/src/main/java/com/petlife/server/modules/family/domain/entity/AdminFamilyEntity.java`
  - `server/src/main/java/com/petlife/server/modules/family/domain/entity/AdminFamilyPetEntity.java`
  - `server/src/main/java/com/petlife/server/modules/family/dto/response/AdminFamilyPetResponse.java`
  - `server/src/main/java/com/petlife/server/modules/family/dto/response/AdminFamilyResponse.java`
  - `server/src/main/java/com/petlife/server/modules/family/persistence/dataobject/AdminFamilyDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/family/persistence/dataobject/AdminFamilyPetDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/pet/controller/AdminPetController.java`
  - `server/src/main/java/com/petlife/server/modules/pet/converter/AdminPetConverter.java`
  - `server/src/main/java/com/petlife/server/modules/pet/domain/entity/AdminPetEntity.java`
  - `server/src/main/java/com/petlife/server/modules/pet/dto/response/AdminPetFamilyResponse.java`
  - `server/src/main/java/com/petlife/server/modules/pet/dto/response/AdminPetResponse.java`
  - `server/src/main/java/com/petlife/server/modules/pet/persistence/dataobject/AdminPetDataObject.java`
- 修改服务端文件：
  - `server/src/main/java/com/petlife/server/modules/moderation/controller/ModerationController.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/converter/ModerationReportConverter.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/domain/entity/ModerationReportEntity.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/dto/request/ProcessModerationReportRequest.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/dto/response/ModerationReportResponse.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/persistence/ModerationPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/persistence/command/ProcessModerationReportCommand.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/persistence/dataobject/ModerationReportDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/service/ModerationApplicationService.java`
  - `server/src/main/java/com/petlife/server/modules/admin/persistence/AuditLogPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/admin/service/AuditLogApplicationService.java`
  - `server/src/main/java/com/petlife/server/modules/user/persistence/UserPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/user/service/UserApplicationService.java`
  - `server/src/main/java/com/petlife/server/modules/family/persistence/FamilyPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/family/service/FamilyApplicationService.java`
  - `server/src/main/java/com/petlife/server/modules/pet/persistence/PetPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/pet/service/PetApplicationService.java`
  - `server/src/test/java/com/petlife/server/bootstrap/PhaseOneApiTests.java`
- 修改文档：
  - `docs/api/petlife-openapi.yaml`
  - `docs/technical/02-api-and-events.md`
  - `docs/technical/03-ddl-draft.sql`
  - `docs/project/01-current-delivery-status.md`
  - `docs/project/02-feature-completion-checklist.md`
  - `docs/project/04-admin-web-api-gap-list.md`
  - `docs/project/server-thread-summary.md`

### 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`
  - 结果：通过。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`
  - 结果：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`
  - 结果：通过，`Tests run: 60, Failures: 0, Errors: 0`。

### 未完成事项

- 后台真实账号模型、权限边界、登录、退出、刷新接口和审计写入。
- 宠物问题数据修复工具：可修复问题类型、状态机、审计动作和写接口未定义。
- 管理端提醒查询接口、提醒模板模型和模板管理接口。
- 消息模板管理、通知发送配置模型和维护接口。
- 真实短信服务、对象存储云厂商 SDK/CDN、第三方内容审核、推送通道、地图与定位能力。

### 风险或阻塞

- 当前后台查询接口仍复用现有 Bearer 鉴权过滤器，尚未接入真实后台账号体系和后台角色权限。
- 宠物、家庭、用户当前仅补齐只读治理查询；封禁、停用、恢复、成员关系修复等写治理需要先定义状态机与审计动作。
- 远程库存在历史数据，测试不能依赖默认初始化字段固定值；新增测试已改为用例内创建确定数据。
- 仓库当前存在大量既有未提交变更和未跟踪文件，后续提交前需要按服务端范围仔细拆分。

### 下一步建议

1. 继续按缺口清单补齐 `GET /api/v1/admin/reminders` 和 `GET /api/v1/admin/reminders/{reminderId}`。
2. 之后定义提醒模板模型，再补充模板列表、新增、编辑、启停接口。
3. 再推进消息模板与通知发送配置模型。
4. 后台真实账号体系应单独作为一轮设计与实现，先明确权限边界和审计规则。
