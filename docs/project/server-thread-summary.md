# 服务端线程交接文档

## 记录规则

- 本文件记录 `petlife` 服务端线程每次完成需求、修复问题、调整设计或发现缺口后的交接信息。
- 每次记录必须包含：新完成内容、新增/修改文件、验证命令与结果、未完成事项、风险或阻塞、下一步建议。
- 如功能状态变化，同步更新 `docs/project/02-feature-completion-checklist.md` 和 `docs/project/01-current-delivery-status.md`。
- 按完整交付标准记录，不使用“核心可用”等阶段性表述。

## 2026-05-20 高德 Web 服务与服务商定位底座

### 新完成内容

- 新增高德 Web 服务配置读取，使用 `PETLIFE_AMAP_WEB_SERVICE_KEY` 注入，不提交真实 Key。
- 新增高德 HTTP Client 适配层，覆盖地理编码、逆地理编码和距离接口封装；配置缺失时返回 `MAP_CONFIGURATION_MISSING`，供应商请求异常返回 `MAP_PROVIDER_REQUEST_FAILED`。
- 新增后台地图运营接口：
  - `GET /api/v1/admin/map/config`
  - `GET /api/v1/admin/map/geocode`
  - `GET /api/v1/admin/map/reverse-geocode`
- 新增服务商坐标维护接口：
  - `PATCH /api/v1/admin/service/providers/{providerId}/location`
- 服务商列表 `GET /api/v1/providers` 支持 `latitude`、`longitude` 和 `sort=distance`，返回 `distance_meters`；当前按已维护坐标计算直线距离，不伪装真实导航路线距离。
- 新增 `docs/technical/12-amap-location-foundation.sql`，补充服务商坐标来源和坐标更新时间字段草案。
- 补充测试覆盖配置缺失、适配层 Mock HTTP、服务商坐标维护、服务商距离返回和距离排序。

### 新增/修改文件

- 新增服务端文件：
  - `server/src/main/java/com/petlife/server/modules/location/controller/AdminMapController.java`
  - `server/src/main/java/com/petlife/server/modules/location/config/AmapWebServiceClientConfig.java`
  - `server/src/main/java/com/petlife/server/modules/location/converter/AmapLocationConverter.java`
  - `server/src/main/java/com/petlife/server/modules/location/domain/entity/AmapConfigStatusEntity.java`
  - `server/src/main/java/com/petlife/server/modules/location/domain/entity/AmapDistanceEntity.java`
  - `server/src/main/java/com/petlife/server/modules/location/domain/entity/AmapGeocodeEntity.java`
  - `server/src/main/java/com/petlife/server/modules/location/domain/entity/AmapReverseGeocodeEntity.java`
  - `server/src/main/java/com/petlife/server/modules/location/domain/entity/GeoPointEntity.java`
  - `server/src/main/java/com/petlife/server/modules/location/dto/request/AdminGeocodeRequest.java`
  - `server/src/main/java/com/petlife/server/modules/location/dto/response/AmapConfigStatusResponse.java`
  - `server/src/main/java/com/petlife/server/modules/location/dto/response/AmapGeocodeResponse.java`
  - `server/src/main/java/com/petlife/server/modules/location/dto/response/AmapReverseGeocodeResponse.java`
  - `server/src/main/java/com/petlife/server/modules/location/service/AmapLocationApplicationService.java`
  - `server/src/main/java/com/petlife/server/modules/location/service/AmapWebServiceProperties.java`
  - `server/src/main/java/com/petlife/server/modules/location/service/provider/AmapWebServiceClient.java`
  - `server/src/main/java/com/petlife/server/modules/service/dto/request/AdminUpdateProviderLocationRequest.java`
  - `server/src/main/java/com/petlife/server/modules/service/persistence/command/UpdateServiceProviderLocationCommand.java`
  - `server/src/test/java/com/petlife/server/modules/location/service/provider/AmapWebServiceClientTests.java`
  - `docs/technical/12-amap-location-foundation.sql`
- 修改服务端文件：
  - `server/src/main/resources/application.yml`
  - `server/src/main/java/com/petlife/server/common/response/ResponseCode.java`
  - `server/src/main/java/com/petlife/server/config/GlobalExceptionHandler.java`
  - `server/src/main/java/com/petlife/server/modules/service/controller/ServiceCenterController.java`
  - `server/src/main/java/com/petlife/server/modules/service/controller/AdminServiceCenterController.java`
  - `server/src/main/java/com/petlife/server/modules/service/converter/ServiceProviderConverter.java`
  - `server/src/main/java/com/petlife/server/modules/service/domain/entity/ServiceProviderEntity.java`
  - `server/src/main/java/com/petlife/server/modules/service/dto/request/AdminUpsertServiceProviderRequest.java`
  - `server/src/main/java/com/petlife/server/modules/service/dto/response/ServiceProviderResponse.java`
  - `server/src/main/java/com/petlife/server/modules/service/persistence/ServiceCenterPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/service/persistence/command/UpsertServiceProviderCommand.java`
  - `server/src/main/java/com/petlife/server/modules/service/persistence/dataobject/ServiceProviderDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/service/service/ServiceCenterApplicationService.java`
  - `server/src/test/java/com/petlife/server/bootstrap/PhaseOneApiTests.java`
- 修改文档：
  - `docs/api/petlife-openapi.yaml`
  - `docs/technical/02-api-and-events.md`
  - `docs/project/server-thread-summary.md`
  - `docs/project/admin-web-thread-summary.md`
  - `docs/project/mobile-app-thread-summary.md`
  - `docs/project/04-admin-web-api-gap-list.md`
  - `docs/project/02-feature-completion-checklist.md`
  - `docs/project/01-current-delivery-status.md`

### 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`
  - 结果：通过。
- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests test`
  - 结果：通过测试编译。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`
  - 结果：通过，`Tests run: 97, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`
  - 结果：通过。
- `git diff --check`
  - 结果：通过。
- `matches=$(rg -n --fixed-strings 'key: ${PETLIFE_AMAP_WEB_SERVICE_KEY:' server/src/main/resources docs/api docs/technical || true); bad=$(printf "%s\n" "$matches" | rg -v --fixed-strings 'PETLIFE_AMAP_WEB_SERVICE_KEY:}' || true); if [ -n "$bad" ]; then printf "%s\n" "$bad"; exit 1; else echo "no amap key default found"; fi`
  - 结果：通过，未发现高德 Web 服务 Key 默认值。

### 未完成事项

- 未接高德地图前端 SDK、App 内嵌地图和导航。
- 未提交真实高德 Web 服务 Key；地理编码/逆地理编码在 Key 缺失时会明确失败。
- 服务商列表当前返回直线距离，不代表真实路线距离或导航耗时。

### 风险或阻塞

- 后台地图辅助接口依赖环境变量 `PETLIFE_AMAP_WEB_SERVICE_KEY`；未配置时只能查看配置状态，不能调用 geocode/regeo。
- 服务商坐标质量依赖后台维护，未维护坐标的服务商不会返回距离。
- 真实路线距离、导航和地图选点需要后续明确前端 SDK Key、合规提示和路线距离契约。

### 下一步建议

1. admin-web 接入地图配置状态、地理编码辅助和服务商坐标维护页面。
2. mobile-app 若切换服务端距离排序，按 OpenAPI 传入用户授权后的经纬度并消费 `distance_meters`。
3. 后续如要真实路线距离，先明确高德 distance 类型、批量上限、缓存策略和降级口径。

## 2026-05-20 内容审核底座 + Push 推送底座

### 新完成内容

- 新增内容审核任务模型与状态机，覆盖 `target_type`、`target_id`、`content_type`、`content_snapshot`、`provider_code`、`review_status`、`review_result`、`risk_labels`、`failure_reason`、`callback_payload`。
- 社区独立发帖、问答帖和萌宠日常同步社区不再直接进入公开曝光，统一写入 `pending_review` 并生成 `moderation_tasks`。
- 用户侧社区推荐流、关注流、同城流、话题页、帖子详情和问答详情继续只读取 `review_status=approved` 内容；待审和拒绝内容不进入公开流。
- 新增内容审核 provider 抽象与 `dev_noop` 实现；当前只沉淀任务，不伪造自动通过。
- 新增审核供应商回调入口 `POST /api/v1/moderation/callbacks/{providerCode}`，以及后台审核任务查询、详情、人工通过、人工拒绝接口：
  - `GET /api/v1/admin/moderation/tasks`
  - `GET /api/v1/admin/moderation/tasks/{taskId}`
  - `PATCH /api/v1/admin/moderation/tasks/{taskId}/status`
- 人工审核会同步社区内容审核状态并写入 `audit_logs.target_type=moderation_task`；旧待审任务在内容更新时置为 `failed/content_updated_before_review`，避免旧结论覆盖新内容。
- 新增 Push 设备 Token、Push 任务和 Push 投递记录底座：
  - `POST /api/v1/push/device-tokens`
  - `DELETE /api/v1/push/device-tokens/{deviceTokenId}`
  - `GET /api/v1/admin/push-tasks`
  - `GET /api/v1/admin/push-deliveries`
- 站内通知生成后派生 Push 任务；当前 `dev_noop` Push provider 不真实发送，有设备 token 时任务和投递记录保持 `pending`，无 token 或通知开关关闭时记录 `skipped` 原因。
- 扩展 PhaseOneApiTests，覆盖审核任务创建、公开流过滤、人工通过/拒绝、日常同步审核、Push token 注册、通知派生 Push、通知开关拦截、后台查询权限边界。

### 新增/修改文件

- 新增审核服务端文件：
  - `server/src/main/java/com/petlife/server/modules/moderation/controller/ModerationCallbackController.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/converter/ModerationTaskConverter.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/domain/entity/ModerationTaskEntity.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/dto/request/AdminReviewModerationTaskRequest.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/dto/request/ModerationProviderCallbackRequest.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/dto/response/ModerationTaskResponse.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/persistence/ModerationTaskPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/persistence/command/CreateModerationTaskCommand.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/persistence/command/UpdateModerationTaskReviewCommand.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/persistence/dataobject/ModerationTaskDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/service/ModerationTaskApplicationService.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/service/provider/ContentModerationProvider.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/service/provider/DevelopmentNoopContentModerationProvider.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/service/provider/ModerationSubmissionRequest.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/service/provider/ModerationSubmissionResult.java`
- 新增 Push 服务端文件：
  - `server/src/main/java/com/petlife/server/modules/notification/controller/AdminPushNotificationController.java`
  - `server/src/main/java/com/petlife/server/modules/notification/controller/PushDeviceTokenController.java`
  - `server/src/main/java/com/petlife/server/modules/notification/converter/PushNotificationConverter.java`
  - `server/src/main/java/com/petlife/server/modules/notification/domain/entity/PushDeviceTokenEntity.java`
  - `server/src/main/java/com/petlife/server/modules/notification/domain/entity/PushTaskEntity.java`
  - `server/src/main/java/com/petlife/server/modules/notification/domain/entity/PushDeliveryRecordEntity.java`
  - `server/src/main/java/com/petlife/server/modules/notification/dto/request/RegisterPushDeviceTokenRequest.java`
  - `server/src/main/java/com/petlife/server/modules/notification/dto/response/PushDeviceTokenResponse.java`
  - `server/src/main/java/com/petlife/server/modules/notification/dto/response/PushTaskResponse.java`
  - `server/src/main/java/com/petlife/server/modules/notification/dto/response/PushDeliveryRecordResponse.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/PushNotificationPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/command/UpsertPushDeviceTokenCommand.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/command/CreatePushTaskCommand.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/command/CreatePushDeliveryRecordCommand.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/dataobject/PushDeviceTokenDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/dataobject/PushTaskDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/dataobject/PushDeliveryRecordDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/notification/service/PushNotificationApplicationService.java`
  - `server/src/main/java/com/petlife/server/modules/notification/service/provider/PushProvider.java`
  - `server/src/main/java/com/petlife/server/modules/notification/service/provider/DevelopmentNoopPushProvider.java`
- 修改服务端文件：
  - `server/src/main/java/com/petlife/server/common/response/ResponseCode.java`
  - `server/src/main/java/com/petlife/server/config/GlobalExceptionHandler.java`
  - `server/src/main/java/com/petlife/server/modules/admin/persistence/AuditLogPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/admin/service/AuditLogApplicationService.java`
  - `server/src/main/java/com/petlife/server/modules/auth/security/DevelopmentTokenAuthenticationFilter.java`
  - `server/src/main/java/com/petlife/server/modules/community/service/CommunityApplicationService.java`
  - `server/src/main/java/com/petlife/server/modules/moderation/controller/ModerationController.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/NotificationPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/notification/service/NotificationApplicationService.java`
  - `server/src/test/java/com/petlife/server/bootstrap/PhaseOneApiTests.java`
- 新增/修改文档：
  - `docs/technical/11-moderation-push-foundation.sql`
  - `docs/technical/03-ddl-draft.sql`
  - `docs/technical/02-api-and-events.md`
  - `docs/api/petlife-openapi.yaml`
  - `docs/project/server-thread-summary.md`
  - `docs/project/admin-web-thread-summary.md`
  - `docs/project/mobile-app-thread-summary.md`
  - `docs/project/02-feature-completion-checklist.md`
  - `docs/project/01-current-delivery-status.md`

### 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`
  - 结果：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`
  - 结果：通过，`Tests run: 94, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`
  - 结果：通过。
- `git diff --check`
  - 结果：通过。

### 未完成事项

- 未接第三方内容审核 SDK，`dev_noop` 不会自动给出真实审核结论；新公开内容需要后台人工通过后才进入用户侧公开流。
- 未接 APNs、FCM、华为、小米等 Push SDK；当前 Push 任务和投递记录只沉淀状态，不真实发送。
- admin-web 尚未接入审核任务管理页、Push 任务排查页和 Push 投递记录排查页。
- mobile-app 尚未接入系统 Push 权限、设备 token 上报和解绑；本轮只提供服务端契约。

### 风险或阻塞

- 发布公开社区内容后默认 `pending_review`，如果后台没有人工审核入口接入，用户侧公开流不会展示新内容。
- 当前回调入口作为供应商抽象预留，未接第三方签名校验；真实供应商接入前不能对公网开放无签名回调。
- Push 任务状态为 `pending` 不代表已投递，只表示服务端已沉淀待发送任务；后台页面必须区分 `pending` 与真实成功。

### 下一步建议

1. admin-web 接入审核任务列表/详情/人工通过/拒绝页面，以及 Push 任务和投递记录排查页面。
2. mobile-app 后续接入系统 Push 权限、设备 token 注册/解绑和通知点击落点。
3. 接入真实内容审核或 Push 供应商时，基于现有 provider 抽象扩展，并补签名、回调验签、调度和失败重试。

## 2026-05-19 短信验证码安全闭环供应商无关底座

### 新完成内容

- 移除服务端固定验证码 `123456` 登录校验，`POST /api/v1/auth/sms/send` 改为生成 6 位随机验证码。
- 新增 `sms_verification_codes` 与 `sms_send_records` 持久化模型，验证码仅保存 `code_hash` 和 `salt`，不在响应、日志或后台查询中返回明文。
- 新增验证码状态机：`active`、`verified`、`expired`、`locked`、`send_failed`；支持过期、使用后失效、错误次数递增和达到上限锁定。
- 新增发送频控：同手机号 + 同 scene 60 秒内不可重复发送、每小时最多 5 次；同 IP + 同 scene 每小时最多 20 次。
- 新增供应商无关 `SmsProvider` 抽象与 `dev_noop` 实现；当前不接真实短信 SDK，只记录发送受理状态和边界。
- 新增后台排查接口：
  - `GET /api/v1/admin/sms-verifications`
  - `GET /api/v1/admin/sms-send-records`
- 新增 PhaseOneApiTests 覆盖短信发送不泄露明文、频控、错误次数、过期、成功后失效、错误验证码无法登录、后台查询不泄露明文和权限边界。

### 新增/修改文件

- 新增服务端文件：
  - `server/src/main/java/com/petlife/server/modules/auth/controller/AdminSmsVerificationController.java`
  - `server/src/main/java/com/petlife/server/modules/auth/converter/SmsVerificationConverter.java`
  - `server/src/main/java/com/petlife/server/modules/auth/domain/entity/SmsVerificationCodeEntity.java`
  - `server/src/main/java/com/petlife/server/modules/auth/domain/entity/SmsSendRecordEntity.java`
  - `server/src/main/java/com/petlife/server/modules/auth/dto/response/SmsVerificationRecordResponse.java`
  - `server/src/main/java/com/petlife/server/modules/auth/dto/response/SmsSendRecordResponse.java`
  - `server/src/main/java/com/petlife/server/modules/auth/persistence/SmsVerificationPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/auth/persistence/command/CreateSmsVerificationCodeCommand.java`
  - `server/src/main/java/com/petlife/server/modules/auth/persistence/command/CreateSmsSendRecordCommand.java`
  - `server/src/main/java/com/petlife/server/modules/auth/persistence/command/IncrementSmsVerificationAttemptCommand.java`
  - `server/src/main/java/com/petlife/server/modules/auth/persistence/command/UpdateSmsVerificationStatusCommand.java`
  - `server/src/main/java/com/petlife/server/modules/auth/persistence/dataobject/SmsVerificationCodeDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/auth/persistence/dataobject/SmsSendRecordDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/auth/service/SmsVerificationApplicationService.java`
  - `server/src/main/java/com/petlife/server/modules/auth/service/sms/SmsProvider.java`
  - `server/src/main/java/com/petlife/server/modules/auth/service/sms/SmsSendResult.java`
  - `server/src/main/java/com/petlife/server/modules/auth/service/sms/DevelopmentNoopSmsProvider.java`
  - `docs/technical/09-sms-verification-security.sql`
- 修改服务端文件：
  - `server/src/main/java/com/petlife/server/common/response/ResponseCode.java`
  - `server/src/main/java/com/petlife/server/config/GlobalExceptionHandler.java`
  - `server/src/main/java/com/petlife/server/modules/auth/controller/AuthController.java`
  - `server/src/main/java/com/petlife/server/modules/auth/dto/request/AuthSmsLoginRequest.java`
  - `server/src/main/java/com/petlife/server/modules/auth/dto/request/AuthSmsSendRequest.java`
  - `server/src/main/java/com/petlife/server/modules/auth/dto/response/AuthSmsSendResponse.java`
  - `server/src/main/java/com/petlife/server/modules/auth/service/AuthApplicationService.java`
  - `server/src/test/java/com/petlife/server/bootstrap/PhaseOneApiTests.java`
- 修改文档：
  - `docs/api/petlife-openapi.yaml`
  - `docs/api/QUICK_START.md`
  - `docs/api/POSTMAN_SETUP.md`
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
  - 结果：通过，`Tests run: 89, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`
  - 结果：通过。
- `git diff --check`
  - 结果：通过。

### 未完成事项

- 真实短信供应商 SDK、签名、模板备案、回执和发送回调未接入。
- admin-web 尚未接入短信验证码发送记录 / 校验记录排查页面。
- mobile-app 仍需移除固定验证码展示、自动填充和 `mocked_code` 读取逻辑。

### 风险或阻塞

- 当前 `dev_noop` 供应商只表示服务端已受理发送请求；没有真实短信送达能力，生产环境必须先接入真实供应商再对外开放短信登录。
- 服务端响应已不返回 `mocked_code`，移动端未适配前，用户无法从接口响应中看到验证码。
- 后台查询接口不会返回明文验证码、`code_hash` 或 `salt`；排查只能基于状态、次数、供应商和时间线。

### 下一步建议

1. mobile-app 按 OpenAPI 移除 `mocked_code` 依赖，并调整登录页不再展示固定验证码。
2. admin-web 按 OpenAPI 增加短信验证码排查页，只展示安全字段。
3. 接入真实短信供应商时，基于 `SmsProvider` 增加厂商实现，并保持验证码明文只在发送瞬间存在于内存中。

## 2026-05-19 通知与消息配置闭环服务端能力补齐

### 新完成内容

- 基于现有 `message_templates` 表新增后台消息模板管理接口：
  - `GET /api/v1/admin/message-templates`
  - `GET /api/v1/admin/message-templates/{templateId}`
  - `POST /api/v1/admin/message-templates`
  - `PATCH /api/v1/admin/message-templates/{templateId}`
  - `PATCH /api/v1/admin/message-templates/{templateId}/status`
- 新增消息模板字段校验与唯一性规则：`template_code + channel_type` 唯一，重复创建或更新返回明确业务异常；渠道类型仅支持 `inbox`、`sms`、`push`。
- 确认全量 DDL 草案此前没有通知渠道配置表，新增 `notification_channel_configs` 增量 SQL 草案与服务端真实模型。
- 新增后台通知渠道配置接口：
  - `GET /api/v1/admin/notification-channels`
  - `GET /api/v1/admin/notification-channels/{channelConfigId}`
  - `POST /api/v1/admin/notification-channels`
  - `PATCH /api/v1/admin/notification-channels/{channelConfigId}`
  - `PATCH /api/v1/admin/notification-channels/{channelConfigId}/status`
- 新增通知配置审计查询接口 `GET /api/v1/admin/notification/audit-logs`，配置写操作记录 `message_template`、`notification_channel` 审计目标。
- 欢迎消息、提醒完成/跳过通知、服务预约通知、审核结果通知已改为优先读取启用的 `inbox` 消息模板。
- 模板缺失策略已收口：仅内置模板白名单使用服务端默认模板，避免后台未配置模板阻断现有登录、提醒、预约和审核主链路；未知模板缺失仍抛业务异常。
- 新增服务端测试覆盖消息模板 CRUD、启停、重复校验、通知渠道 CRUD、启停、审计查询、权限边界和通知生成读取模板。

### 新增/修改文件

- 新增服务端文件：
  - `server/src/main/java/com/petlife/server/modules/notification/controller/AdminNotificationConfigController.java`
  - `server/src/main/java/com/petlife/server/modules/notification/converter/MessageTemplateConverter.java`
  - `server/src/main/java/com/petlife/server/modules/notification/converter/NotificationChannelConfigConverter.java`
  - `server/src/main/java/com/petlife/server/modules/notification/domain/entity/MessageTemplateEntity.java`
  - `server/src/main/java/com/petlife/server/modules/notification/domain/entity/NotificationChannelConfigEntity.java`
  - `server/src/main/java/com/petlife/server/modules/notification/dto/request/AdminUpsertMessageTemplateRequest.java`
  - `server/src/main/java/com/petlife/server/modules/notification/dto/request/AdminUpdateMessageTemplateStatusRequest.java`
  - `server/src/main/java/com/petlife/server/modules/notification/dto/request/AdminUpsertNotificationChannelRequest.java`
  - `server/src/main/java/com/petlife/server/modules/notification/dto/request/AdminUpdateNotificationChannelStatusRequest.java`
  - `server/src/main/java/com/petlife/server/modules/notification/dto/response/MessageTemplateResponse.java`
  - `server/src/main/java/com/petlife/server/modules/notification/dto/response/NotificationChannelConfigResponse.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/MessageTemplatePersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/NotificationChannelConfigPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/command/UpsertMessageTemplateCommand.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/command/UpdateMessageTemplateStatusCommand.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/command/UpsertNotificationChannelConfigCommand.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/command/UpdateNotificationChannelConfigStatusCommand.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/dataobject/MessageTemplateDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/notification/persistence/dataobject/NotificationChannelConfigDataObject.java`
  - `server/src/main/java/com/petlife/server/modules/notification/service/NotificationConfigApplicationService.java`
- 修改服务端文件：
  - `server/src/main/java/com/petlife/server/common/response/ResponseCode.java`
  - `server/src/main/java/com/petlife/server/config/GlobalExceptionHandler.java`
  - `server/src/main/java/com/petlife/server/modules/admin/persistence/AuditLogPersistenceMapper.java`
  - `server/src/main/java/com/petlife/server/modules/admin/service/AuditLogApplicationService.java`
  - `server/src/main/java/com/petlife/server/modules/notification/service/NotificationApplicationService.java`
  - `server/src/test/java/com/petlife/server/bootstrap/PhaseOneApiTests.java`
- 修改文档：
  - `docs/api/petlife-openapi.yaml`
  - `docs/technical/02-api-and-events.md`
  - `docs/technical/03-ddl-draft.sql`
  - `docs/technical/08-notification-config-upgrade.sql`
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
  - 结果：通过，`Tests run: 81, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`
  - 结果：通过。
- `git diff --check`
  - 结果：通过。

### 未完成事项

- admin-web 尚未接入消息模板管理、通知渠道配置和通知配置审计页面。
- 本轮不接真实短信、不接真实 Push、不接第三方供应商 SDK；短信和 Push 只保留模板与渠道配置契约。
- 真实外部推送通道、短信服务、对象存储云厂商适配器、第三方内容审核和地图能力仍未完成。

### 风险或阻塞

- `inbox` 和 `push` 模板要求 `title_template` 非空；短信模板允许标题为空。
- 模板占位符目前用于站内通知渲染，未知占位符会返回业务异常；admin-web 接入模板编辑时应按 OpenAPI 和技术文档限制内置模板编码。
- 通知渠道配置启用时必须为 `ready`，停用时不能为 `ready`；状态入口会自动把启用归一为 `ready`、停用归一为 `disabled`。

### 下一步建议

1. admin-web 按 OpenAPI 接入消息模板管理、通知渠道配置和通知配置审计页面。
2. 后续如进入真实短信或 Push 发送，基于 `notification_channel_configs` 增加供应商适配器和发送任务，不在业务服务中硬编码厂商。
3. 继续保留 `notification_switch` 为用户通知总开关，真实外部推送接入时也要复用该边界。

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
