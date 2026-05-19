# 服务端线程交接文档

## 记录规则

- 本文件记录 `petlife` 服务端线程每次完成需求、修复问题、调整设计或发现缺口后的交接信息。
- 每次记录必须包含：新完成内容、新增/修改文件、验证命令与结果、未完成事项、风险或阻塞、下一步建议。
- 如功能状态变化，同步更新 `docs/project/02-feature-completion-checklist.md` 和 `docs/project/01-current-delivery-status.md`。
- 按完整交付标准记录，不使用“核心可用”等阶段性表述。

## 2026-05-19 社区用户增长闭环服务端能力补齐

### 新完成内容

- 新增独立社区帖子发布接口 `POST /api/v1/community/posts`，不再只依赖萌宠日常同步社区。
- 社区帖子读模型扩展话题、媒体资产 ID、媒体资产元数据和审核状态；萌宠日常同步社区时同步日常媒体资产 ID。
- `GET /api/v1/community/feed` 补齐 `recommended`、`following`、`city`、`qa` 四类真实服务端流。
- 新增社区帖子详情、话题页、问答详情、关注、取消关注和关注状态接口。
- 新增后台社区内容治理接口：
  - `GET /api/v1/admin/community/posts`
  - `GET /api/v1/admin/community/posts/{postId}`
  - `PATCH /api/v1/admin/community/posts/{postId}/status`
  - `GET /api/v1/admin/community/questions`
  - `GET /api/v1/admin/community/questions/{questionId}`
  - `PATCH /api/v1/admin/community/questions/{questionId}/status`
- 后台治理下架 / 恢复统一更新 `community_posts.review_status`，并写入 `audit_logs`；审核审计日志查询已支持 `community_post`、`community_question` 目标类型。
- 新增服务端测试覆盖独立发布、详情、关注、话题、问答、后台治理、审计写入和权限边界。

### 新增/修改文件

- 新增服务端文件：
  - `server/src/main/java/com/petlife/server/modules/community/controller/AdminCommunityController.java`
  - `server/src/main/java/com/petlife/server/modules/community/domain/entity/CommunityTopicEntity.java`
  - `server/src/main/java/com/petlife/server/modules/community/dto/request/CreateCommunityPostRequest.java`
  - `server/src/main/java/com/petlife/server/modules/community/dto/request/AdminUpdateCommunityContentStatusRequest.java`
  - `server/src/main/java/com/petlife/server/modules/community/dto/response/CommunityFollowStatusResponse.java`
  - `server/src/main/java/com/petlife/server/modules/community/dto/response/CommunityQuestionDetailResponse.java`
  - `server/src/main/java/com/petlife/server/modules/community/dto/response/CommunityTopicDetailResponse.java`
  - `server/src/main/java/com/petlife/server/modules/community/dto/response/CommunityTopicResponse.java`
  - `server/src/main/java/com/petlife/server/modules/community/persistence/command/CreateUserFollowCommand.java`
  - `server/src/main/java/com/petlife/server/modules/community/persistence/command/DeleteUserFollowCommand.java`
  - `server/src/main/java/com/petlife/server/modules/community/persistence/command/UpdateCommunityPostReviewStatusCommand.java`
  - `server/src/main/java/com/petlife/server/modules/community/persistence/dataobject/CommunityTopicDataObject.java`
- 修改服务端文件：
  - `server/src/main/java/com/petlife/server/common/response/ResponseCode.java`
  - `server/src/main/java/com/petlife/server/config/GlobalExceptionHandler.java`
  - `server/src/main/java/com/petlife/server/modules/admin/persistence/AuditLogPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/admin/service/AuditLogApplicationService.java`
  - `server/src/main/java/com/petlife/server/modules/community/controller/CommunityController.java`
  - `server/src/main/java/com/petlife/server/modules/community/converter/CommunityPostConverter.java`
  - `server/src/main/java/com/petlife/server/modules/community/domain/entity/CommunityPostEntity.java`
  - `server/src/main/java/com/petlife/server/modules/community/dto/response/CommunityPostResponse.java`
  - `server/src/main/java/com/petlife/server/modules/community/persistence/CommunityPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/community/persistence/command/CreateCommunityPostCommand.java`
  - `server/src/main/java/com/petlife/server/modules/community/persistence/command/UpdateCommunityPostCommand.java`
  - `server/src/main/java/com/petlife/server/modules/community/persistence/dataobject/CommunityPostDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/community/service/CommunityApplicationService.java`
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
  - 结果：通过，`Tests run: 77, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`
  - 结果：通过。
- `git diff --check`
  - 结果：通过。

### 未完成事项

- mobile-app 尚未接入社区独立发布页、话题页、问答详情页和关注 / 取消关注交互。
- admin-web 尚未接入社区帖子治理、问答治理、下架/恢复和治理审计查看页面。
- 真实第三方内容审核仍未接入；本轮不接第三方审核，不新增自动审核任务。
- 对象存储云厂商适配器、推送通道、真实短信和地图能力仍未完成。

### 风险或阻塞

- 独立社区帖子媒体只允许引用 `biz_type=community` 的已上传图片或视频；移动端接入发布页时不能复用日常或健康附件 asset_id。
- 下架/恢复接口只接受动作 `take_down` / `restore`，前端不能直接提交 `review_status`。
- 当前用户侧发布由服务端写入 `review_status=approved`，后台人工治理负责后续下架/恢复；若后续接入第三方审核，需要重新定义发布后的审核态流转。

### 下一步建议

1. mobile-app 按 OpenAPI 接入社区发布页、话题页、问答详情页和关注交互。
2. admin-web 按 OpenAPI 接入社区帖子治理、问答治理和审计查看页面。
3. 服务端后续按缺口清单推进通知模板管理、通知发送配置和第三方审核设计。

## 2026-05-18 后台真实账号、治理写能力与首页聚合补齐

### 新完成内容

- 新增后台真实账号与会话链路：`admin_accounts`、`admin_sessions`、后台登录、刷新、退出和后台 token 鉴权边界。
- 后台 `/admin/**` 业务接口改为只接受后台 access token，普通 App 用户 token 会被拒绝。
- 新增用户封禁/恢复接口，封禁时同步吊销用户端会话并写审计。
- 新增家庭停用/恢复和 owner 成员关系修复接口，停用时重建相关当前宠物上下文。
- 新增宠物问题数据修复接口，支持 `family_missing`、`owner_member_missing`、`current_pet_context`。
- 新增 `GET /api/v1/home` 首页专用聚合接口，移动端首页可直接消费该读模型。

### 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 使用远程 MySQL 测试库运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 -Dtest=PhaseOneApiTests test`：通过，`Tests run: 69, Failures: 0, Errors: 0, Skipped: 0`。
- 使用远程 MySQL 测试库运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 73, Failures: 0, Errors: 0, Skipped: 0`。

### 未完成事项

- 真实短信、推送通道、对象存储云厂商适配器、第三方审核和地图能力仍未接入。
- 细粒度后台 RBAC 仍未展开，本轮仅落地独立后台账号、会话和审计身份。

## 2026-05-18 用户端提醒模板读取接口补齐

### 新完成内容

- 新增用户端宠物可用提醒模板接口：`GET /api/v1/pets/{petId}/reminder-templates`。
- 接口会校验当前登录用户是否可访问该宠物，仅返回 `enabled=true` 且 `applicable_pet_type=all` 或匹配当前宠物类型的模板。
- 新增服务端测试覆盖猫宠可见模板范围、停用模板过滤、非匹配宠物类型过滤和无 token 权限边界。
- 已同步 OpenAPI、技术接口说明、功能完成清单、当前交付状态和 UI 收口清单。

### 新增/修改文件

- 新增：`server/src/main/java/com/petlife/server/modules/reminder/controller/ReminderTemplateController.java`
- 修改：`server/src/main/java/com/petlife/server/modules/reminder/service/ReminderTemplateApplicationService.java`
- 修改：`server/src/main/java/com/petlife/server/modules/reminder/persistence/ReminderTemplatePersistenceMapper.java`
- 修改：`server/src/test/java/com/petlife/server/bootstrap/PhaseOneApiTests.java`
- 修改：`docs/api/petlife-openapi.yaml`
- 修改：`docs/technical/02-api-and-events.md`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/03-ui-closure-checklist.md`

### 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 67, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`：通过。
- `git diff --check`：通过。

### 未完成事项

- 提醒模板仍只是读取和表单预填能力；提醒创建仍由用户确认提交。
- 后台真实账号体系、通知模板和推送通道仍未完成。

### 风险或阻塞

- 当前接口不提供分页，返回最多 100 条启用模板；如果后台模板量明显增长，需要补分页契约。

## 2026-05-18 后台提醒模板管理接口补齐

### 新完成内容

- 新增提醒模板模型与 DDL 草案 `reminder_templates`，覆盖模板名称、提醒类型、默认提醒模式、默认提前量、默认周期、适用宠物类型、启用状态、排序、创建/更新时间和软删除预留字段。
- 新增后台提醒模板管理接口：
  - `GET /api/v1/admin/reminder-templates`
  - `GET /api/v1/admin/reminder-templates/{templateId}`
  - `POST /api/v1/admin/reminder-templates`
  - `PATCH /api/v1/admin/reminder-templates/{templateId}`
  - `PATCH /api/v1/admin/reminder-templates/{templateId}/status`
- 新增模板字段校验与状态规则：`single` 模板不能传默认周期字段，`cycle` 模板必须传默认周期值和单位；提醒类型、单位、适用宠物类型均按枚举归一校验。
- 模板创建、更新、启停复用现有 Bearer 鉴权和 `X-Admin-Operator` 审计上下文，不引入真实后台账号体系。
- 新增服务端测试覆盖列表、详情、筛选、创建、更新、启停、非法周期配置和无 token 权限边界。
- 已同步 OpenAPI、技术接口说明、DDL 草案、后台接口缺口清单、功能完成清单和当前交付状态。

### 新增/修改文件

- 新增服务端文件：
  - `server/src/main/java/com/petlife/server/modules/reminder/controller/AdminReminderTemplateController.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/converter/ReminderTemplateConverter.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/domain/entity/ReminderTemplateEntity.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/dto/request/AdminUpsertReminderTemplateRequest.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/dto/request/AdminUpdateReminderTemplateStatusRequest.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/dto/response/ReminderTemplateResponse.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/persistence/ReminderTemplatePersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/persistence/command/UpsertReminderTemplateCommand.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/persistence/command/UpdateReminderTemplateStatusCommand.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/persistence/dataobject/ReminderTemplateDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/reminder/service/ReminderTemplateApplicationService.java`
- 修改服务端文件：
  - `server/src/main/java/com/petlife/server/common/response/ResponseCode.java`
  - `server/src/main/java/com/petlife/server/config/GlobalExceptionHandler.java`
  - `server/src/test/java/com/petlife/server/bootstrap/PhaseOneApiTests.java`
- 修改文档：
  - `docs/api/petlife-openapi.yaml`
  - `docs/technical/02-api-and-events.md`
  - `docs/technical/03-ddl-draft.sql`
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
  - 结果：通过，`Tests run: 65, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`
  - 结果：通过。
- `git diff --check`
  - 结果：通过。

### 未完成事项

- admin-web 尚未接入系统提醒查询页面和提醒模板管理页面。
- 后台真实账号体系、角色权限边界、登录、退出、刷新和管理端审计身份仍未完成。
- 消息模板管理、通知发送配置模型和维护接口仍未完成。
- 提醒模板暂未接入用户端提醒创建流程，本轮仅完成后台管理端服务端契约。

### 风险或阻塞

- 当前模板写接口仍复用现有 Bearer 鉴权过滤器和 `X-Admin-Operator` 审计标识；真实管理员身份与权限边界需后续单独实现。
- 当前列表按 `sort_order ASC, id DESC` 返回最多 200 条；如果 admin-web 需要分页，需要先更新 OpenAPI 契约再扩展。
- DDL 已进入草案和测试库建表逻辑，生产或长期环境仍需按 `docs/technical/03-ddl-draft.sql` 执行表结构变更。

### 下一步建议

1. admin-web 可按 OpenAPI 接入系统提醒查询和提醒模板管理页面。
2. 服务端下一轮按缺口清单推进消息模板管理与通知发送配置模型。
3. 后台真实账号体系应单独设计角色权限、会话与审计身份后再实现。

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
