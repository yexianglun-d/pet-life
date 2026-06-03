# Admin Web Thread Summary

## 固定交接规则

- 本线程只负责 `admin-web` 后台管理端。
- 每次完成需求、修复问题、调整设计或发现缺口后，必须同步更新本文件。
- 若功能状态变化，必须同步更新 `docs/project/02-feature-completion-checklist.md` 和 `docs/project/01-current-delivery-status.md`。
- 所有状态按完整交付标准记录，不使用阶段性跑通口径。

## 2026-05-31 页面说明文案收口

### 1. 新完成内容

- 清理后台可见页面的说明型标题和边界说明，改为管理类标题。
- 地图排查页删除说明段落，仅保留真实配置、坐标、距离能力数据和明确未完成标识。
- 通知、短信、Push、审核供应商未完成项统一使用“功能未完成：缺少...”格式，不再以说明段落呈现。

### 2. 验证命令与结果

- `npm run type-check`：通过。

### 3. 未完成事项

- 后台深层 Element Plus 组件主题、表格空态和全局弹窗风格仍需继续收口。

## 2026-05-20 地图收口服务端补修同步

### 1. 新完成内容

- 服务端已修复地图收口验收反馈中的服务端缺口：增量 SQL 可补齐缺失的 `idx_service_providers_coordinate_source`，地图配置状态响应新增 `required_config_key=PETLIFE_AMAP_WEB_SERVICE_KEY`，高德 geocode / reverse-geocode 解析增强真实响应兼容。
- 本轮未修改 admin-web 源码。

### 2. 新增/修改文件

- 修改：`docs/api/petlife-openapi.yaml`
- 修改：`docs/technical/02-api-and-events.md`
- 修改：`docs/technical/12-amap-location-foundation.sql`
- 修改：`docs/project/admin-web-thread-summary.md`
- 修改：`docs/project/05-test-plan-and-report.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/01-current-delivery-status.md`

### 3. 验证命令与结果

- 服务端 `mvn -Dmaven.repo.local=/tmp/petlife-m2 -Dtest=AmapWebServiceClientTests,AmapLocationApplicationServiceTests test`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- 使用远程 MySQL 配置运行服务端 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 103, Failures: 0, Errors: 0, Skipped: 0`。
- OpenAPI YAML 解析：通过。
- `git diff --check`：通过。

### 4. 未完成事项

- admin-web 端到端地图排查页仍需真实高德 Web Key、最新服务端实例和服务商坐标数据。

### 5. 风险或阻塞

- 如果运行实例未重启到最新服务端代码，后台地图接口仍可能表现为旧路由不可用。

### 6. 下一步建议

1. 服务端实例重启并注入真实 Key 后，后台重新验收配置状态、地址解析、坐标反查和坐标保存。

## 2026-05-20 地图排查页验收问题修正

### 1. 新完成内容

- 已读取 `docs/project/05-test-plan-and-report.md` 中“地图上线收口验收反馈”，本轮只修复后台地图排查页验收发现的展示与交互问题，不扩展新功能。
- 修正地图配置状态展示，区分 `读取中`、`读取失败`、`待确认`、`已配置`、`未配置`，不再把配置未加载或加载失败误显示为 `未配置`。
- 地址解析和坐标反查统一使用地图配置状态判断；配置读取中、读取失败、未配置时给出对应提示，避免误导为前端已持有或可绕过 Web 服务 Key。
- 继续保持页面不展示假地图、不持有或展示真实 Key、不接 Web JS 地图 SDK。

### 2. 新增/修改文件

- 修改：`admin-web/src/views/service/ServiceMapDebugView.vue`
- 修改：`docs/project/admin-web-thread-summary.md`
- 修改：`docs/project/04-admin-web-api-gap-list.md`
- 修改：`docs/project/03-ui-closure-checklist.md`

### 3. 验证命令与结果

- `npm run type-check`：通过。
- `npm run build`：通过；Vite 仍提示主 chunk 超过 500 kB，这是当前后台工程既有打包体积警告，不影响构建结果。
- `git diff --check`：通过。

### 4. 未完成事项

- 真实地址解析、坐标反查、坐标保存端到端验收仍依赖服务端注入真实 `PETLIFE_AMAP_WEB_SERVICE_KEY` 和测试库服务商坐标数据。
- 后台仍未接 Web JS 地图、地图选点或真实路线距离排查。

### 5. 风险或阻塞

- 测试线程已记录：测试库索引、8080 最新服务实例、真实 Key、带坐标服务商数据和真机环境仍未完全就绪，地图上线收口尚不能按通过口径提交。
- `config=false`、配置加载失败和真实高德接口不可用都只能由页面展示真实状态，admin-web 不做前端兜底模拟。

### 6. 下一步建议

1. 服务端注入真实 Key、补齐测试库服务商坐标数据后，重新验收地图配置、地址解析、坐标反查和坐标保存。
2. 若后续要做后台地图选点或路线距离排查，先明确 Web JS Key 管理和新增后台接口边界。

## 2026-05-20 地图上线收口测试反馈待办

### 1. 测试结论

- 不建议作为“地图上线收口验收通过”提交。
- admin-web 的 `npm run type-check && npm run build` 通过，地图排查页代码检查通过，但真实地址解析、坐标反查和坐标保存仍依赖真实 Key 与服务商数据，尚未完成端到端验收。

### 2. 后台端待处理

- 等服务端注入真实 `PETLIFE_AMAP_WEB_SERVICE_KEY`、测试库补齐服务商坐标数据后，重新验收地图排查页。
- 验收地图配置状态、地址解析、坐标反查、服务商坐标保存。
- 页面继续保持不展示假地图、不持有或展示真实 Key。
- 验收完成后同步更新 `docs/project/05-test-plan-and-report.md`、`docs/project/03-ui-closure-checklist.md` 和 `docs/project/04-admin-web-api-gap-list.md`。

## 2026-05-20 服务商地图坐标维护与地图能力排查页面接入

### 1. 新完成内容

- `admin-web` 新增地图辅助 API 封装，接入真实后台接口：
  - `GET /api/v1/admin/map/config`
  - `GET /api/v1/admin/map/geocode`
  - `GET /api/v1/admin/map/reverse-geocode`
  - `PATCH /api/v1/admin/service/providers/{providerId}/location`
- 新增地图排查页，展示服务商地址、经纬度、坐标来源、坐标覆盖率和距离能力就绪状态。
- 地图排查页支持通过服务端地理编码辅助把地址解析为坐标，也支持坐标反查地址。
- 地图排查页支持手动编辑服务商地址、纬度、经度和坐标来源，保存前二次确认，写入后刷新当前服务商坐标。
- 页面明确标注：高德 Web 服务 Key 只由服务端使用；后台未接 Web JS 地图能力，不展示地图画布或假地图。
- 后台路由、侧栏和系统配置页新增 `地图排查`、`地图坐标排查` 入口。

### 2. 新增/修改文件

- 新增：`admin-web/src/views/service/ServiceMapDebugView.vue`
- 修改：`admin-web/src/shared/api/serviceApi.ts`
- 修改：`admin-web/src/router/index.ts`
- 修改：`admin-web/src/layouts/AdminLayout.vue`
- 修改：`admin-web/src/views/system/SystemConfigView.vue`
- 修改：`docs/project/admin-web-thread-summary.md`
- 修改：`docs/project/04-admin-web-api-gap-list.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/03-ui-closure-checklist.md`

### 3. 验证命令与结果

- `npm run type-check`：通过。
- `npm run build`：通过；Vite 仍提示主 chunk 超过 500 kB，这是当前后台工程既有打包体积警告，不影响构建结果。
- `git diff --check`：通过。

### 4. 未完成事项

- 后台未接入高德 Web JS API、地图选点组件或地图大屏。
- 后台页面不展示真实地图画布；只做配置状态、地址解析、坐标维护和距离能力排查。
- 真实 Key 仍通过环境变量注入服务端，admin-web 不持有、不展示、不硬编码。

### 5. 风险或阻塞

- 高德 Web 服务 Key 缺失时，地理编码和逆地理编码会由服务端返回配置缺失错误，页面只展示错误，不做前端兜底模拟。
- 距离能力排查只检查服务商坐标覆盖和服务端 capability；真实用户端距离排序仍以服务端用户端接口计算为准。

### 6. 下一步建议

1. 如需后台地图选点，先申请并明确 Web JS Key 管理、域名白名单和权限边界，再接前端地图 SDK。
2. 后续如需路线距离或导航状态排查，由服务端先定义对应后台接口后再接入 admin-web。

## 2026-05-20 服务端高德地图与服务商坐标接口可接入

### 1. 新完成内容

- 服务端已新增后台地图配置状态与地理编码辅助接口，并同步到 OpenAPI：
  - `GET /api/v1/admin/map/config`
  - `GET /api/v1/admin/map/geocode`
  - `GET /api/v1/admin/map/reverse-geocode`
- 服务端已新增服务商坐标维护接口：
  - `PATCH /api/v1/admin/service/providers/{providerId}/location`
- 服务商列表接口已支持用户经纬度返回 `distance_meters`，并支持 `sort=distance`。
- 本轮未修改 admin-web 源码，后台地图辅助与服务商坐标维护页面仍待后续接入。

### 2. 新增/修改文件

- 修改：`docs/api/petlife-openapi.yaml`
- 修改：`docs/technical/02-api-and-events.md`
- 新增：`docs/technical/12-amap-location-foundation.sql`
- 修改：`docs/project/admin-web-thread-summary.md`
- 修改：`docs/project/04-admin-web-api-gap-list.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/01-current-delivery-status.md`

### 3. 验证命令与结果

- 服务端 `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 服务端 `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests test`：通过测试编译。
- 使用远程 MySQL 配置运行服务端 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 97, Failures: 0, Errors: 0, Skipped: 0`。
- 服务端 OpenAPI YAML 解析：通过。
- `git diff --check`：通过。

### 4. 未完成事项

- admin-web 尚未接入地图配置状态、地理编码辅助和服务商坐标维护页面。
- 未接前端地图组件、地图选点和导航能力。

### 5. 风险或阻塞

- `GET /api/v1/admin/map/config` 不返回 Key；页面只能展示配置是否就绪。
- 高德 Web 服务 Key 缺失时，地理编码和逆地理编码会返回 `MAP_CONFIGURATION_MISSING`。
- 服务商列表 `distance_meters` 是基于已维护坐标的直线距离，不代表真实导航路线距离。

### 6. 下一步建议

1. admin-web 在服务商编辑页增加坐标维护入口，调用地理编码辅助接口回填经纬度后提交坐标维护接口。
2. 后续如接入地图选点组件，需要先明确高德前端 SDK Key 管理和权限边界。

## 2026-05-20 内容审核任务与 Push 投递排查页面接入

### 1. 新完成内容

- 新增内容审核任务 API 封装，接入真实后台接口：
  - `GET /api/v1/admin/moderation/tasks`
  - `GET /api/v1/admin/moderation/tasks/{taskId}`
  - `PATCH /api/v1/admin/moderation/tasks/{taskId}/status`
  - `GET /api/v1/admin/moderation/audit-logs`
- 新增内容审核任务页，支持 target_type、content_type、review_status、provider_code、关键词和时间筛选，展示任务状态、风险标签、失败原因和目标内容摘要。
- 内容审核任务详情抽屉展示审核快照、审核结果、回调 payload 和 moderation_task 审计记录。
- 内容审核任务页支持人工通过 / 拒绝，提交前二次确认，写操作后刷新任务列表和审计记录。
- 新增 Push 投递排查 API 封装，接入真实后台接口：
  - `GET /api/v1/admin/push-tasks`
  - `GET /api/v1/admin/push-deliveries`
- 新增 Push 投递排查页，支持用户、notify_type、provider_code、task_status、delivery_status 和时间筛选，展示设备标识脱敏、任务状态、投递状态和失败原因。
- 页面明确标注 `dev_noop`、`manual`、Push `sent` 的边界，不把本地占位、人工处理或服务端状态标记包装成真实第三方审核/真实 Push 投递。
- 后台路由、侧栏和系统配置页新增 `审核任务`、`Push 投递排查` 入口。

### 2. 新增/修改文件

- 新增：`admin-web/src/shared/api/adminModerationTaskApi.ts`
- 新增：`admin-web/src/shared/api/adminPushNotificationApi.ts`
- 新增：`admin-web/src/views/moderation/ModerationTaskReviewView.vue`
- 新增：`admin-web/src/views/notification/PushDeliveryDebugView.vue`
- 修改：`admin-web/src/router/index.ts`
- 修改：`admin-web/src/layouts/AdminLayout.vue`
- 修改：`admin-web/src/views/system/SystemConfigView.vue`
- 修改：`docs/project/admin-web-thread-summary.md`
- 修改：`docs/project/04-admin-web-api-gap-list.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/01-current-delivery-status.md`

### 3. 验证命令与结果

- `npm run type-check`：通过。
- `npm run build`：通过；Vite 仍提示主 chunk 超过 500 kB，这是当前后台工程既有打包体积警告，不影响构建结果。
- `git diff --check`：通过。

### 4. 未完成事项

- 真实第三方内容审核供应商仍未接入。
- 真实 APNs / 厂商 Push 推送通道仍未接入，当前只排查服务端 Push 底座任务和投递记录。
- OpenAPI 当前未提供审核任务关键词 / 时间范围、Push notify_type / 时间范围的服务端查询参数，页面基于真实返回字段做前端收窄。
- Push 投递记录当前只返回 `device_token_id`，不返回真实 token 或服务端生成的 `masked_device_token`。

### 5. 风险或阻塞

- 大数据量排查时，关键词和时间范围仍在页面侧过滤，后续需要服务端补查询参数以避免一次性返回过多记录。
- `dev_noop`、`manual` 和 Push `sent` 只能说明底座或人工链路状态，不能作为真实第三方供应商结论。

### 6. 下一步建议

1. 服务端补充审核任务和 Push 排查的服务端分页、关键词、时间范围和 notify_type 查询参数。
2. 等真实内容审核供应商和真实 Push 供应商接入后，再补供应商回调结果、投递回执、失败报表和通道健康检查页面。

## 2026-05-20 服务端内容审核与 Push 排查接口可接入

### 1. 新完成内容

- 服务端已新增后台审核任务查询、详情、人工通过和人工拒绝接口，并同步到 OpenAPI：
  - `GET /api/v1/admin/moderation/tasks`
  - `GET /api/v1/admin/moderation/tasks/{taskId}`
  - `PATCH /api/v1/admin/moderation/tasks/{taskId}/status`
- 服务端已新增 Push 任务和投递记录后台排查接口：
  - `GET /api/v1/admin/push-tasks`
  - `GET /api/v1/admin/push-deliveries`
- 后台审核审计查询已支持 `target_type=moderation_task`。
- 本轮未修改 admin-web 源码，审核任务页面和 Push 排查页面仍待后续接入。

### 2. 新增/修改文件

- 修改：`docs/api/petlife-openapi.yaml`
- 修改：`docs/project/admin-web-thread-summary.md`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 服务端实现和验证见 `docs/project/server-thread-summary.md` 同日记录。

### 3. 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 94, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`：通过。
- `git diff --check`：通过。

### 4. 未完成事项

- admin-web 尚未接入审核任务管理页、Push 任务列表页和 Push 投递记录排查页。
- 真实第三方审核供应商和真实 Push SDK 未接入；后台页面不能把 `dev_noop`、`pending` 展示成真实成功。

### 5. 风险或阻塞

- 新发布公开内容默认进入 `pending_review`，后台没有审核任务页面时，运营无法在后台完成首次曝光审核。
- Push 排查接口只表示服务端任务与投递记录沉淀，不代表厂商真实投递。

### 6. 下一步建议

1. 新增审核任务管理页：列表、详情、按状态/目标/供应商筛选、人工通过、人工拒绝、审计日志查看。
2. 新增 Push 排查页：任务列表、投递记录列表、按用户/通知/状态/供应商筛选，并明确 `pending/skipped/failed` 含义。

## 2026-05-19 短信验证码安全排查页面接入

### 1. 新完成内容

- 新增短信验证码安全排查 API 封装，接入真实后台只读接口：
  - `GET /api/v1/admin/sms-send-records`
  - `GET /api/v1/admin/sms-verifications`
- 新增验证码排查页，支持按手机号、scene、send_status、校验 status、provider_code 和时间范围筛选。
- 页面列表展示手机号脱敏、发送状态、校验状态、错误次数、过期时间、请求 IP、provider_code 和失败原因。
- 详情抽屉展示发送记录与校验记录的完整排查字段；不展示明文验证码、`code_hash` 或 `salt`。
- 后台侧栏和路由新增 `验证码排查` 入口，系统配置页新增对应真实入口。
- 页面明确标注当前是供应商无关安全底座，真实短信供应商尚未接入。

### 2. 新增/修改文件

- 新增：`admin-web/src/shared/api/adminSmsVerificationApi.ts`
- 新增：`admin-web/src/views/auth/SmsVerificationSecurityView.vue`
- 修改：`admin-web/src/router/index.ts`
- 修改：`admin-web/src/layouts/AdminLayout.vue`
- 修改：`admin-web/src/views/system/SystemConfigView.vue`
- 修改：`docs/project/admin-web-thread-summary.md`
- 修改：`docs/project/04-admin-web-api-gap-list.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/01-current-delivery-status.md`

### 3. 验证命令与结果

- `npm run type-check`：通过。
- `npm run build`：通过；Vite 仍提示主 chunk 超过 500 kB，这是当前后台工程既有打包体积警告，不影响构建结果。
- `git diff --check`：通过。

### 4. 未完成事项

- 真实短信供应商、发送回执、失败报表和通道健康检查仍未接入。
- OpenAPI 当前未提供服务端时间范围查询参数；页面时间范围基于真实返回的时间字段做前端收窄。

### 5. 风险或阻塞

- 后台接口不会返回明文验证码、`code_hash` 或 `salt`，页面也未设计查看验证码能力。
- 当前 `provider_code=dev_noop` 只能排查服务端受理、频控、错误次数和状态机，不代表短信真实送达。

### 6. 下一步建议

1. 服务端如需支持大数据量排查，应补充短信发送记录与校验记录的时间范围查询参数。
2. 等真实短信供应商接入后，再补发送回执、失败报表和通道健康检查。

## 2026-05-19 服务端短信验证码排查接口可接入

### 1. 新完成内容

- 服务端已补齐短信验证码发送记录和校验记录后台查询接口，并同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/sms-verifications`
  - `GET /api/v1/admin/sms-send-records`
- 后台查询接口需要后台 access token；普通 App 用户 token 和未登录请求均不能访问。
- 接口只返回手机号、场景、状态、次数、供应商、发送状态、失败原因、请求 IP、User-Agent 和时间，不返回明文验证码、`code_hash` 或 `salt`。
- 本轮未修改 admin-web 源码，短信验证码排查页面仍待后续接入。

### 2. 新增/修改文件

- 修改：`docs/api/petlife-openapi.yaml`
- 修改：`docs/project/admin-web-thread-summary.md`
- 修改：`docs/project/04-admin-web-api-gap-list.md`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 服务端实现和验证见 `docs/project/server-thread-summary.md` 同日记录。

### 3. 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 89, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`：通过。
- `git diff --check`：通过。

### 4. 未完成事项

- admin-web 尚未接入短信验证码排查页面。
- 真实短信供应商、发送回执、失败报表和通道健康检查仍未接入。

### 5. 风险或阻塞

- 查询接口不会提供明文验证码，后台页面不能设计“查看验证码”能力。
- 目前供应商编码为 `dev_noop`，只能排查服务端受理、频控、错误次数和状态机，不代表短信真实送达。

### 6. 下一步建议

1. 新增认证排查或安全治理页面，按 OpenAPI 接入两个只读列表。
2. 页面筛选建议先覆盖手机号、scene、状态、供应商和发送状态。

## 2026-05-19 通知与消息配置页面接入

### 1. 新完成内容

- 新增消息模板管理页，接入真实后台消息模板列表、详情、创建、编辑、启停接口。
- 新增通知发送配置页，接入真实后台通知渠道列表、详情、创建、编辑、启停接口。
- 两个页面均接入 `GET /api/v1/admin/notification/audit-logs`，分别展示消息模板和通知渠道配置审计记录。
- 通知发送配置页明确标注短信和 Push 当前只维护配置，不代表真实供应商已接入。
- 后台侧栏和路由新增 `消息模板`、`通知渠道` 入口；系统配置页从占位说明调整为真实通知配置入口。

### 2. 新增/修改文件

- 新增：`admin-web/src/shared/api/adminNotificationConfigApi.ts`
- 新增：`admin-web/src/views/notification/MessageTemplateManagementView.vue`
- 新增：`admin-web/src/views/notification/NotificationChannelConfigView.vue`
- 修改：`admin-web/src/router/index.ts`
- 修改：`admin-web/src/layouts/AdminLayout.vue`
- 修改：`admin-web/src/views/system/SystemConfigView.vue`
- 修改：`docs/project/admin-web-thread-summary.md`
- 修改：`docs/project/04-admin-web-api-gap-list.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/01-current-delivery-status.md`

### 3. 验证命令与结果

- `npm run type-check`：通过。
- `npm run build`：通过；Vite 仍提示主 chunk 超过 500 kB，这是当前后台工程既有打包体积警告，不影响构建结果。
- `git diff --check`：通过。

### 4. 未完成事项

- 真实短信供应商和 Push 推送通道仍未接入。
- 通知发送配置页只维护后台配置状态，不包含供应商密钥、回调、发送报表或通道健康检查。

### 5. 风险或阻塞

- 消息模板唯一性仍以服务端 `template_code + channel_type` 校验为准，前端只做必填校验。
- 通知渠道启用时必须为 `ready`，停用时不能为 `ready`；页面已做前置校验，但最终规则以服务端为准。

### 6. 下一步建议

1. 等服务端接入真实短信 / Push 供应商后，再补供应商密钥、发送状态和失败告警后台能力。
2. 后续如需要按钮级权限，应基于后台 RBAC 扩展菜单与操作权限。

## 2026-05-19 通知与消息配置接口可接入

### 1. 新完成内容

- 服务端已补齐消息模板管理接口，并同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/message-templates`
  - `GET /api/v1/admin/message-templates/{templateId}`
  - `POST /api/v1/admin/message-templates`
  - `PATCH /api/v1/admin/message-templates/{templateId}`
  - `PATCH /api/v1/admin/message-templates/{templateId}/status`
- 服务端已补齐通知渠道配置接口，并同步到 OpenAPI：
  - `GET /api/v1/admin/notification-channels`
  - `GET /api/v1/admin/notification-channels/{channelConfigId}`
  - `POST /api/v1/admin/notification-channels`
  - `PATCH /api/v1/admin/notification-channels/{channelConfigId}`
  - `PATCH /api/v1/admin/notification-channels/{channelConfigId}/status`
- 新增通知配置审计查询接口：`GET /api/v1/admin/notification/audit-logs`。
- 本轮未修改 admin-web 源码，通知配置页面仍待后续接入。

### 2. 新增/修改文件

- 修改：`docs/api/petlife-openapi.yaml`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/04-admin-web-api-gap-list.md`
- 修改：`docs/project/admin-web-thread-summary.md`
- 服务端实现文件见 `docs/project/server-thread-summary.md` 同日记录。

### 3. 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 81, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`：通过。
- `git diff --check`：通过。

### 4. 未完成事项

- admin-web 尚未实现消息模板管理页、通知渠道配置页和通知配置审计展示。
- 真实短信和 Push 供应商发送能力未接入，本轮只提供配置模型与后台契约。

### 5. 风险或阻塞

- 消息模板 `template_code + channel_type` 唯一，前端保存前不应在本地绕过服务端重复校验。
- 通知渠道启用时必须处于 `ready` 状态，停用时不能处于 `ready` 状态；状态切换接口会自动归一配置状态。

### 6. 下一步建议

1. 增加 admin-web 消息模板 API 类型、列表、详情、创建、编辑和启停交互。
2. 增加通知渠道配置 API 类型、列表、详情、创建、编辑、启停和配置审计查询。

## 2026-05-19 社区内容治理页面接入

### 1. 新完成内容

- 新增社区帖子治理页，接入真实后台帖子列表、详情、下架、恢复接口。
- 新增问答治理页，接入真实后台问答列表、详情、下架、恢复接口。
- 两个治理页均支持列表筛选、详情抽屉、作者/宠物/话题/互动数据展示。
- 接入 `GET /api/v1/admin/moderation/audit-logs`，分别展示 `community_post` 与 `community_question` 治理审计记录。
- 后台侧栏和路由新增 `社区帖子`、`问答治理` 入口，页面沿用 hero、summary、panel 风格。

### 2. 新增/修改文件

- 新增：`admin-web/src/shared/api/adminCommunityApi.ts`
- 新增：`admin-web/src/views/community/CommunityPostGovernanceView.vue`
- 新增：`admin-web/src/views/community/CommunityQuestionGovernanceView.vue`
- 修改：`admin-web/src/router/index.ts`
- 修改：`admin-web/src/layouts/AdminLayout.vue`
- 修改：`docs/project/admin-web-thread-summary.md`
- 修改：`docs/project/04-admin-web-api-gap-list.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/01-current-delivery-status.md`

### 3. 验证命令与结果

- `npm run type-check`：通过。
- `npm run build`：通过；Vite 仍提示主 chunk 超过 500 kB，这是当前后台工程既有打包体积警告，不影响构建结果。
- `git diff --check`：通过。

### 4. 未完成事项

- 社区用户端发布页、话题页、问答详情页仍未接入。
- 第三方内容审核仍未接入。
- 通知模板管理、通知发送配置仍待服务端定义模型和接口。

### 5. 风险或阻塞

- 社区治理写操作依赖管理员登录态与 `X-Admin-Operator`，后续如果扩展 RBAC，需要补按钮级权限。
- 当前治理页只提供下架/恢复，不包含编辑内容、封禁作者或自动审核策略。

### 6. 下一步建议

1. 继续等待服务端补齐通知模板与通知渠道配置接口后接入后台通知配置。
2. 移动端线程补齐社区发布页、话题页和问答详情页。

## 2026-05-19 社区内容治理接口可接入

### 1. 新完成内容

- 服务端已补齐后台社区内容治理接口，并同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/community/posts`
  - `GET /api/v1/admin/community/posts/{postId}`
  - `PATCH /api/v1/admin/community/posts/{postId}/status`
  - `GET /api/v1/admin/community/questions`
  - `GET /api/v1/admin/community/questions/{questionId}`
  - `PATCH /api/v1/admin/community/questions/{questionId}/status`
- 审核审计日志接口已扩展支持 `target_type=community_post` 和 `target_type=community_question`。
- 本轮未修改 admin-web 源码，社区内容治理页面仍待后续接入。

### 2. 新增/修改文件

- 修改：`docs/api/petlife-openapi.yaml`
- 修改：`docs/project/04-admin-web-api-gap-list.md`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/admin-web-thread-summary.md`
- 服务端实现文件见 `docs/project/server-thread-summary.md` 同日记录。

### 3. 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 77, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`：通过。
- admin-web 构建未执行：本轮没有修改 admin-web 源码。

### 4. 未完成事项

- admin-web 尚未接入社区帖子治理、问答治理、下架/恢复和治理审计查看页面。
- 通知模板管理、通知发送配置仍待服务端定义模型和接口。

### 5. 风险或阻塞

- 社区治理页面只能使用本轮进入 OpenAPI 的真实接口，不应本地 mock 内容或治理状态。
- 下架/恢复只提交动作 `take_down` / `restore`，不要让前端直接提交 `review_status`。

### 6. 下一步建议

1. 按 OpenAPI 增加社区治理 API 类型和请求函数。
2. 新增社区帖子治理和问答治理页面，支持筛选、详情、下架/恢复和审计日志查看。

## 2026-05-18 后台真实登录与治理操作入口接入

### 1. 新完成内容

- 后台登录页改为调用真实 `POST /api/v1/admin/auth/login`，保存后台 access token、refresh token、操作者和角色编码。
- 后台退出改为调用 `POST /api/v1/admin/auth/logout`，本地会话清理保留。
- 用户管理页新增封禁/恢复入口。
- 家庭管理页新增停用/恢复和 owner 成员关系修复入口。
- 宠物主档查询页新增数据修复下拉入口，支持家庭缺失、主人成员缺失和当前宠物上下文重建。

### 2. 验证命令与结果

- `npm run type-check`：通过。
- `npm run build`：通过；Vite 仍提示主 chunk 超过 500 kB，这是当前后台工程既有打包体积警告，不影响构建结果。

### 3. 未完成事项

- 后台细粒度 RBAC 菜单与按钮级权限尚未展开；当前仅完成独立后台账号登录态与治理操作接入。

## 2026-05-18 系统提醒查询与提醒模板管理页面接入

### 1. 新完成内容

- 新增 `admin-web` 提醒 API 封装，接入系统提醒查询和提醒模板管理真实接口。
- 新增系统提醒查询页，支持状态、类型、模式、宠物、家庭、处理人、时间范围和关键词筛选，并支持详情抽屉查看宠物归属、处理人和来源健康记录。
- 新增提醒模板管理页，支持模板列表、筛选、详情、创建、编辑和启停，写操作使用服务端模板接口并保留审计请求头。
- 后台侧边栏和路由已新增 `系统提醒`、`提醒模板` 两个入口。
- 已同步 `docs/project/01-current-delivery-status.md`、`docs/project/02-feature-completion-checklist.md`、`docs/project/03-ui-closure-checklist.md` 和 `docs/project/04-admin-web-api-gap-list.md`。

### 2. 新增/修改文件

- 新增：`admin-web/src/shared/api/adminReminderApi.ts`
- 新增：`admin-web/src/views/reminder/SystemReminderQueryView.vue`
- 新增：`admin-web/src/views/reminder/ReminderTemplateManagementView.vue`
- 修改：`admin-web/src/router/index.ts`
- 修改：`admin-web/src/layouts/AdminLayout.vue`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/03-ui-closure-checklist.md`
- 修改：`docs/project/04-admin-web-api-gap-list.md`
- 修改：`docs/project/admin-web-thread-summary.md`

### 3. 验证命令与结果

- `git diff --check`：通过。
- `npm run build`：通过；Vite 仍提示主 chunk 超过 500 kB，这是当前后台工程既有打包体积警告，不影响本轮页面编译。

### 4. 未完成事项

- 后台真实账号体系仍未完成，当前页面仍复用本地占位 token 与 Bearer 登录态。
- 消息模板管理、通知发送配置仍待服务端定义模型和接口。
- 系统提醒查询页当前只读，不提供完成、跳过或改写提醒的后台写操作。

### 5. 风险或阻塞

- 提醒模板当前只是后台配置能力，不代表用户端提醒创建页已经支持模板选择。
- 如果后续要开放后台改写提醒状态，需要服务端先定义状态机、权限边界和审计动作。

## 2026-05-18 提醒模板管理接口可接入

### 1. 新完成内容

- 服务端已补齐提醒模板管理接口，并同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/reminder-templates`
  - `GET /api/v1/admin/reminder-templates/{templateId}`
  - `POST /api/v1/admin/reminder-templates`
  - `PATCH /api/v1/admin/reminder-templates/{templateId}`
  - `PATCH /api/v1/admin/reminder-templates/{templateId}/status`
- 接口支持模板名称、提醒类型、默认提醒模式、默认提前量、默认周期、适用宠物类型、启用状态和排序字段。
- 模板创建、更新、启停使用 `X-Admin-Operator` 记录后台审计标识；当前仍复用 Bearer 登录态。
- 本轮未修改 admin-web 代码，提醒查询页面和提醒模板管理页面仍待接入。

### 2. 新增/修改文件

- 修改：`docs/api/petlife-openapi.yaml`
- 修改：`docs/project/04-admin-web-api-gap-list.md`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/admin-web-thread-summary.md`
- 服务端实现文件见 `docs/project/server-thread-summary.md` 同日记录。

### 3. 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 65, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`：通过。
- `git diff --check`：通过。
- admin-web 构建未执行：本轮没有修改 admin-web 源码。

### 4. 未完成事项

- 系统提醒查询页面尚未在 admin-web 接入。
- 提醒模板管理页面尚未在 admin-web 接入。
- 后台真实账号体系仍未完成，当前页面仍依赖本地占位 token。
- 消息模板管理、通知发送配置仍待服务端定义模型和接口。

### 5. 风险或阻塞

- 接入提醒模板页面时只能使用本轮进入 OpenAPI 的真实模板接口，不应本地 mock 模板。
- 模板写接口当前只负责后台配置，不代表用户端提醒创建已经支持模板选择。
- 后台真实角色权限未完成前，前端仍无法表达真实管理员权限边界。

### 6. 下一步建议

1. 按 OpenAPI 增加提醒查询与提醒模板 API 类型和请求函数。
2. 新增系统提醒查询页面和提醒模板管理页面，先覆盖列表、筛选、详情、创建、编辑和启停。
3. 等服务端定义消息模板和通知渠道配置后，再接入通知配置页面。

## 2026-05-18 系统提醒查询接口可接入

### 1. 新完成内容

- 服务端已补齐 `GET /api/v1/admin/reminders` 和 `GET /api/v1/admin/reminders/{reminderId}`，并同步到 `docs/api/petlife-openapi.yaml`。
- 接口支持关键词、状态、提醒类型、提醒模式、宠物、家庭、主人、处理人、来源健康记录和提醒时间范围筛选。
- 响应包含 `reminder`、`pet`、`handler`、`source_record`，可支撑 admin-web 后续提醒后台列表、详情和筛选页面。
- 本轮未修改 admin-web 代码，提醒后台页面仍待接入。

### 2. 新增/修改文件

- 修改：`docs/api/petlife-openapi.yaml`
- 修改：`docs/project/04-admin-web-api-gap-list.md`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/admin-web-thread-summary.md`
- 服务端实现文件见 `docs/project/server-thread-summary.md` 同日记录。

### 3. 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 62, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`：通过。
- `git diff --check`：通过。
- admin-web 构建未执行：本轮没有修改 admin-web 源码。

### 4. 未完成事项

- 系统提醒查询页面尚未在 admin-web 接入。
- 提醒模板管理仍缺服务端模型和管理端接口。
- 后台真实账号体系仍未完成，当前页面仍依赖本地占位 token。
- 消息模板管理、通知发送配置仍待服务端定义模型和接口。

### 5. 风险或阻塞

- 接入提醒查询页面时只能使用本轮进入 OpenAPI 的两个真实接口，不应使用用户端提醒接口拼装后台数据。
- 当前后台提醒查询接口只提供只读能力，不包含模板新增、编辑、启停或提醒修复写操作。
- 后台真实角色权限未完成前，前端仍无法表达真实管理员权限边界。

### 6. 下一步建议

1. 按 OpenAPI 增加提醒查询 API 类型与请求函数。
2. 新增系统提醒查询页面，仅接入列表、筛选和详情。
3. 等服务端定义提醒模板模型后，再接入提醒模板管理。

## 2026-05-18 后台运营内容页面补齐

### 1. 新完成内容

- 补齐健康记录审查页面，支持按记录类型、宠物 ID、操作人 ID、关键词查询，并查看健康记录详情、宠物归属、操作人和附件元数据。
- 补齐萌宠日常内容页面，支持按可见范围、社区同步、宠物 ID、作者 ID、关键词查询，并查看正文、标签、媒体、作者和宠物上下文。
- 补齐成长时间轴排查页面，支持按事件类型、来源类型、宠物 ID、源记录 ID 查询，并查看事件详情、来源状态和宠物归属。
- 补齐后台侧栏入口与路由：健康记录、萌宠日常、时间轴排查。
- 同步记录当前缺口：后台真实账号、用户/家庭/宠物治理页、提醒后台、通知模板仍依赖服务端管理端接口。

### 2. 新增/修改文件

- 新增 `admin-web/src/shared/api/adminContentApi.ts`
- 新增 `admin-web/src/views/content/HealthRecordReviewView.vue`
- 新增 `admin-web/src/views/content/DailyLogContentView.vue`
- 新增 `admin-web/src/views/content/TimelineEventDebugView.vue`
- 修改 `admin-web/src/router/index.ts`
- 修改 `admin-web/src/layouts/AdminLayout.vue`
- 修改 `docs/project/01-current-delivery-status.md`
- 修改 `docs/project/02-feature-completion-checklist.md`
- 修改 `docs/project/04-admin-web-api-gap-list.md`
- 新增 `docs/project/admin-web-thread-summary.md`

### 3. 验证命令与结果

- `cd admin-web && npm run build`：通过；Vite 仅提示产物 chunk 超过 500 kB 的构建警告。
- `git diff --check -- admin-web/src/layouts/AdminLayout.vue admin-web/src/router/index.ts admin-web/src/shared/api/adminContentApi.ts admin-web/src/views/content/HealthRecordReviewView.vue admin-web/src/views/content/DailyLogContentView.vue admin-web/src/views/content/TimelineEventDebugView.vue docs/project/01-current-delivery-status.md docs/project/02-feature-completion-checklist.md docs/project/04-admin-web-api-gap-list.md`：通过，无空白错误。
- `curl -I --max-time 2 http://127.0.0.1:5173/`：返回 `200 OK`，本地 dev server 可访问。

### 4. 未完成事项

- 后台真实账号体系和真实登录接口仍未完成。
- 用户数据管理页未接入。
- 家庭数据与成员关系管理页未接入。
- 宠物数据查询页和宠物问题数据修复工具未接入。
- 提醒模板管理、系统提醒查询未接入。
- 消息模板管理、通知发送配置未接入。
- 商城后台属于当前预留，不进入本批次。

### 5. 风险或阻塞

- 认证、用户、家庭、宠物、提醒、通知模板等后台能力不能用用户端接口替代，必须等待服务端管理端接口进入 `docs/api/petlife-openapi.yaml` 后再接入。
- 宠物问题数据修复工具缺少服务端定义的可修复问题类型、状态机、写接口和审计动作，前端不能提前硬造写操作。
- 当前后台登录仍为本地占位 token，真实权限边界和审计链路需要服务端线程先定义。

### 6. 下一步建议

1. 优先接入已经进入 OpenAPI 的用户、家庭、宠物管理端查询接口，继续补齐运营查询页面。
2. 等服务端补齐提醒管理端接口后，再实现系统提醒查询和提醒模板管理。
3. 等服务端补齐通知模板和渠道配置模型后，再实现消息模板管理与通知发送配置。
4. 后续每次 admin-web 变更完成后，先更新本文件，再同步检查 `01-current-delivery-status.md` 与 `02-feature-completion-checklist.md` 是否需要变更。

## 2026-05-18 用户、家庭、宠物运营查询页面补齐

### 1. 新完成内容

- 核对 `docs/api/petlife-openapi.yaml`、管理端 Controller、DTO 和服务端测试断言，确认用户、家庭、宠物六个查询接口字段口径一致。
- 新增后台用户管理查询页，支持关键词、手机号、昵称、城市编码、通知开关、隐私级别筛选，并查看用户详情、设置、主要家庭和当前宠物。
- 新增后台家庭管理查询页，支持关键词、家庭名、成员手机号、成员角色、家庭状态筛选，并查看家庭详情、成员关系和家庭宠物。
- 新增后台宠物档案查询页，支持关键词、宠物名、宠物类型、宠物状态、主人手机号、家庭 ID 筛选，并查看宠物详情、主人和家庭归属。
- 三个页面都只做真实查询和详情查看，未增加封禁、恢复、停用、修复工具等写操作。
- 已接入后台侧栏和路由。

### 2. 新增/修改文件

- 新增 `admin-web/src/shared/api/adminGovernanceApi.ts`
- 新增 `admin-web/src/views/operation/UserManagementView.vue`
- 新增 `admin-web/src/views/operation/FamilyManagementView.vue`
- 新增 `admin-web/src/views/operation/PetArchiveQueryView.vue`
- 修改 `admin-web/src/router/index.ts`
- 修改 `admin-web/src/layouts/AdminLayout.vue`
- 修改 `docs/project/01-current-delivery-status.md`
- 修改 `docs/project/02-feature-completion-checklist.md`
- 修改 `docs/project/04-admin-web-api-gap-list.md`
- 修改 `docs/project/admin-web-thread-summary.md`

### 3. 验证命令与结果

- `cd admin-web && npm run build`：通过；Vite 仅提示产物 chunk 超过 500 kB 的构建警告。
- `git diff --check -- admin-web/src/layouts/AdminLayout.vue admin-web/src/router/index.ts admin-web/src/shared/api/adminGovernanceApi.ts admin-web/src/views/operation/UserManagementView.vue admin-web/src/views/operation/FamilyManagementView.vue admin-web/src/views/operation/PetArchiveQueryView.vue docs/project/admin-web-thread-summary.md docs/project/04-admin-web-api-gap-list.md docs/project/02-feature-completion-checklist.md docs/project/01-current-delivery-status.md`：通过，无空白错误。

### 4. 未完成事项

- 后台真实账号体系和真实登录接口仍未完成。
- 用户封禁/恢复等写治理能力未实现，需服务端先定义状态机、权限边界和审计动作。
- 家庭停用、恢复、成员关系修复等写治理能力未实现，需服务端先定义状态机和审计动作。
- 宠物问题数据修复工具未实现，需服务端先定义可修复问题类型、状态机、写接口和审计动作。
- 提醒模板管理、系统提醒查询未接入。
- 消息模板管理、通知发送配置未接入。

### 5. 风险或阻塞

- 本轮只读查询页面依赖当前 OpenAPI 和服务端 DTO 口径；后续字段新增或重命名需先更新 OpenAPI，再同步前端类型。
- 写治理能力不能使用用户端接口或本地 mock 替代，必须等待服务端管理端接口进入 OpenAPI。
- 当前后台登录仍为本地占位 token，真实权限边界和审计链路需要服务端线程先定义。

### 6. 下一步建议

1. 等服务端补齐后台真实账号模型、登录、退出、刷新和权限审计后，替换当前本地占位登录。
2. 等服务端补齐用户、家庭、宠物写治理状态机与审计动作后，再接入封禁、恢复、停用或修复工具。
3. 按接口缺口清单继续等待提醒查询、提醒模板、通知模板和通知渠道配置接口，再补齐对应后台页面。

## 2026-05-18 用户、家庭、宠物运营查询页面复核

### 1. 新完成内容

- 按当前代码重新核对交接文档、接口缺口清单、功能清单、OpenAPI、路由、侧栏、内容页面和服务中心页面风格。
- 确认 `GET /api/v1/admin/users`、`GET /api/v1/admin/users/{userId}`、`GET /api/v1/admin/families`、`GET /api/v1/admin/families/{familyId}`、`GET /api/v1/admin/pets`、`GET /api/v1/admin/pets/{petId}` 已有真实 API 封装并被页面使用。
- 确认用户管理、家庭管理、宠物档案三个页面已接入后台侧栏和路由，页面只做真实查询与详情查看。
- 确认本轮无需新增业务代码变更，当前实现已经满足本轮目标。

### 2. 新增/修改文件

- 修改 `docs/project/admin-web-thread-summary.md`

### 3. 验证命令与结果

- `cd admin-web && npm run build`：通过；Vite 仅提示产物 chunk 超过 500 kB 的构建警告。
- `git diff --check -- admin-web/src/layouts/AdminLayout.vue admin-web/src/router/index.ts admin-web/src/shared/api/adminGovernanceApi.ts admin-web/src/views/operation/UserManagementView.vue admin-web/src/views/operation/FamilyManagementView.vue admin-web/src/views/operation/PetArchiveQueryView.vue docs/project/admin-web-thread-summary.md docs/project/04-admin-web-api-gap-list.md docs/project/02-feature-completion-checklist.md docs/project/01-current-delivery-status.md`：通过，无空白错误。

### 4. 未完成事项

- 后台真实账号体系和真实登录接口仍未完成。
- 用户封禁/恢复等写治理能力仍未实现。
- 家庭停用、恢复、成员关系修复等写治理能力仍未实现。
- 宠物问题数据修复工具仍未实现。
- 提醒模板管理、通知模板管理和通知发送配置仍未接入。

### 5. 风险或阻塞

- 写治理能力仍依赖服务端状态机、权限边界、审计动作和管理端写接口，admin-web 不能提前硬造本地操作。
- 当前后台登录仍为本地占位 token，真实管理员权限边界尚未落地。

### 6. 下一步建议

1. 当前交接文档显示系统提醒查询接口已经进入 OpenAPI，下一轮可优先接入系统提醒查询页。
2. 继续等待服务端补齐提醒模板、通知模板和真实后台认证接口后，再接入对应后台能力。
