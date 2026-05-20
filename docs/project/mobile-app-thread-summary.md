# Mobile App Thread Summary

## 线程边界

- 当前线程只负责 `mobile-app`。
- 不修改 `server` 与 `admin-web` 代码。
- 若发现接口、字段、后台治理或服务端能力缺口，只记录在本文档的“风险或阻塞 / 下一步建议”，并交由对应线程处理。
- 每次完成需求、修复问题、调整设计或发现缺口后，必须同步更新本文档；如功能状态变化，同时更新 `docs/project/01-current-delivery-status.md` 与 `docs/project/02-feature-completion-checklist.md`。

## 2026-05-20 移动端 Push token 底座与社区审核状态展示接入

### 1. 新完成内容

- 新增移动端 Push 设备 Token 注册结果模型，并在 `PetLifeRepository` / `NetworkPetLifeRepository` 中接入：
  - `POST /api/v1/push/device-tokens`
  - `DELETE /api/v1/push/device-tokens/{deviceTokenId}`
- 当前未接 APNs、FCM、华为、小米等 SDK，不申请系统通知权限，也不生成本地 token；只保留真实 SDK token 到位后的仓储调用入口。
- 社区发布成功后按服务端返回的 `review_status` 展示状态：`pending_review` 显示“审核中，不会立即公开”，`approved` 保持发布成功语义，`rejected` 显示不公开。
- 社区首页、话题页和内容卡片兼容 `review_status`，过滤 `rejected` 内容，并为非 `approved` 内容显示审核状态提示。
- 帖子详情和问答详情兼容审核状态：`pending_review` 展示待审核提示，`rejected` 不展示正文内容；加载失败时将社区内容不可见提示与普通网络错误区分。

### 2. 新增/修改文件

- 新增：`mobile-app/lib/shared/domain/models/push_device_token_snapshot.dart`
- 新增：`mobile-app/lib/modules/community/presentation/widgets/community_review_status.dart`
- 修改：`mobile-app/lib/shared/repository/petlife_repository.dart`
- 修改：`mobile-app/lib/shared/repository/network_petlife_repository.dart`
- 修改：`mobile-app/lib/modules/community/presentation/pages/community_post_editor_page.dart`
- 修改：`mobile-app/lib/modules/community/presentation/pages/community_home_page.dart`
- 修改：`mobile-app/lib/modules/community/presentation/pages/community_topic_page.dart`
- 修改：`mobile-app/lib/modules/community/presentation/pages/community_post_detail_page.dart`
- 修改：`mobile-app/lib/modules/community/presentation/pages/community_question_detail_page.dart`
- 修改：`mobile-app/lib/modules/community/presentation/widgets/community_post_card.dart`
- 修改：`mobile-app/test/widget_test.dart`
- 修改：`docs/project/mobile-app-thread-summary.md`
- 修改：`docs/project/03-ui-closure-checklist.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/01-current-delivery-status.md`

### 3. 验证命令与结果

- `/Users/deng/development/flutter/bin/dart format lib/shared/domain/models/push_device_token_snapshot.dart lib/shared/repository/petlife_repository.dart lib/shared/repository/network_petlife_repository.dart lib/modules/community/presentation/widgets/community_review_status.dart lib/modules/community/presentation/pages/community_post_editor_page.dart lib/modules/community/presentation/pages/community_home_page.dart lib/modules/community/presentation/widgets/community_post_card.dart lib/modules/community/presentation/pages/community_topic_page.dart lib/modules/community/presentation/pages/community_post_detail_page.dart lib/modules/community/presentation/pages/community_question_detail_page.dart test/widget_test.dart`：通过，`Formatted 11 files (0 changed)`。
- `/Users/deng/development/flutter/bin/flutter analyze`：通过，`No issues found!`。
- `/Users/deng/development/flutter/bin/flutter test`：通过，`All tests passed!`。
- `rg -n "push-token|requestPermission|APNs|FCM|华为|小米|系统通知权限|申请通知权限|假 Push|假系统通知|伪造.*Push|伪造.*审核成功" mobile-app/lib mobile-app/test`：通过，无匹配。
- `git diff --check`：通过。

### 4. 未完成事项

- 真实 Push SDK、系统通知权限申请、真实设备 token 获取、登录后自动注册和退出/卸载解绑生命周期仍未接入，本轮按边界不实现。
- 第三方内容审核供应商仍未接入，审核结果由服务端当前审核任务和人工处理链路决定。
- 发布后审核状态主动刷新、被拒绝后的编辑重提入口仍未实现。

### 5. 风险或阻塞

- 没有真实 SDK token 前，移动端不能主动调用注册接口，否则会制造无效设备记录。
- 服务端用户侧公开流和详情按 `approved` 过滤；如果后续要让作者查看自己的待审核/拒绝内容，需要新增“我的发布/审核状态”类用户侧接口。

### 6. 下一步建议

1. 等真实 Push SDK 与系统权限策略明确后，再在登录态恢复、token 刷新和退出时接入注册/解绑触发点。
2. 如果产品需要用户查看审核进度，服务端应先补用户侧“我的社区内容审核状态”接口，移动端再做列表和重提入口。

## 2026-05-20 服务端 Push Token 契约与社区审核状态变化

### 1. 新完成内容

- 服务端已新增 App Push 设备 Token 注册和解绑接口，并同步到 OpenAPI：
  - `POST /api/v1/push/device-tokens`
  - `DELETE /api/v1/push/device-tokens/{deviceTokenId}`
- 服务端站内通知生成后会派生 Push 任务；当前不接真实 Push SDK，任务只沉淀 `pending/skipped/failed` 等状态。
- 服务端社区公开内容发布后改为 `pending_review` 并创建审核任务，用户侧公开流和详情只展示 `approved` 内容。
- 本轮服务端侧未修改 mobile-app 源码；移动端已在后续记录中接入 Push token 仓储适配和发布后待审核提示。

### 2. 新增/修改文件

- 修改：`docs/api/petlife-openapi.yaml`
- 修改：`docs/project/mobile-app-thread-summary.md`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 服务端实现和验证见 `docs/project/server-thread-summary.md` 同日记录。

### 3. 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 94, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`：通过。
- `git diff --check`：通过。

### 4. 未完成事项

- mobile-app 尚未接入真实 Push SDK token 获取、注册触发点、解绑生命周期和通知点击落点。
- 发布社区内容后，移动端已针对 `pending_review` 做待审核提示；审核状态主动刷新仍未实现。
- 真实 Push SDK 未接入，当前服务端 `dev_noop` 不会真实推送到设备。

### 5. 风险或阻塞

- 独立发布社区帖子和日常同步社区后，内容默认不会立即出现在公开流；移动端需要避免把发布成功误表达为“已公开展示”。
- 服务端只返回设备 token 后缀用于排查，移动端不应依赖接口回显完整 token。

### 6. 下一步建议

1. 真实 Push SDK 与系统权限策略明确后，移动端再接入 token 获取、上报触发点和解绑生命周期。
2. 如需用户查看审核进度，服务端先提供用户侧审核状态列表，移动端再做刷新和重提入口。

## 2026-05-19 移动端短信验证码登录体验收口

### 1. 新完成内容

- 登录页移除固定验证码 `123456` 初始化、自动填入和演示验证码提示。
- 发送验证码响应模型按 OpenAPI 改为读取 `sent`、`expires_in_seconds`、`resend_in_seconds`、`provider_code`，不再读取 `mocked_code`。
- 发送成功只提示“验证码已发送，请留意短信”，倒计时继续使用服务端返回的 `resend_in_seconds`，不替代服务端频控。
- 网络层 `ApiException` 保留服务端响应 `code`，登录页按 `AUTH_SMS_CODE_INVALID`、`AUTH_SMS_CODE_EXPIRED`、`AUTH_SMS_CODE_USED`、`AUTH_SMS_CODE_ATTEMPT_LIMITED`、`AUTH_SMS_SEND_RATE_LIMITED`、`AUTH_SMS_SEND_FAILED` 展示不同中文提示。
- 登录成功流程保持不变，仍由仓储保存 `access_token` / `refresh_token` 后进入主应用。

### 2. 新增/修改文件

- 修改：`mobile-app/lib/modules/auth/presentation/pages/login_page.dart`
- 修改：`mobile-app/lib/shared/domain/models/auth_sms_send_snapshot.dart`
- 修改：`mobile-app/lib/shared/repository/network_petlife_repository.dart`
- 修改：`mobile-app/lib/shared/network/api_client.dart`
- 修改：`mobile-app/lib/shared/network/api_exception.dart`
- 修改：`mobile-app/test/widget_test.dart`
- 修改：`docs/project/mobile-app-thread-summary.md`
- 修改：`docs/project/03-ui-closure-checklist.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/01-current-delivery-status.md`

### 3. 验证命令与结果

- `/Users/deng/development/flutter/bin/dart format lib/modules/auth/presentation/pages/login_page.dart lib/shared/domain/models/auth_sms_send_snapshot.dart lib/shared/repository/network_petlife_repository.dart lib/shared/network/api_client.dart lib/shared/network/api_exception.dart test/widget_test.dart`：通过，`Formatted 6 files (0 changed)`。
- `/Users/deng/development/flutter/bin/flutter analyze`：通过，`No issues found!`。
- `/Users/deng/development/flutter/bin/flutter test`：通过，`All tests passed!`。
- `rg -n "演示环境|演示验证码|mocked_code|mockedCode|自动填入 123456|当前演示" mobile-app/lib mobile-app/test`：通过，无匹配。
- `git diff --check`：通过。

### 4. 未完成事项

- 真实短信供应商仍未接入，当前服务端 `dev_noop` 不保证真实短信送达；本轮按边界不接短信 SDK。

### 5. 风险或阻塞

- 服务端不返回明文验证码后，开发环境如果没有真实短信供应商，用户无法从 App 或接口响应中看到验证码；排查需依赖后台短信验证码排查页。
- 若后续服务端错误码或频控返回结构扩展出剩余秒数，移动端需再按 OpenAPI 展示更精确的等待时间。

### 6. 下一步建议

1. 服务端接入真实短信供应商后，移动端无需改 SDK，只继续消费发送和登录接口。
2. 若产品需要更明确的频控剩余时间，服务端可在频控错误响应中增加用户可见的剩余等待秒数字段。

## 2026-05-19 服务端短信验证码安全底座对移动端影响

### 1. 新完成内容

- 服务端已移除固定验证码 `123456` 登录校验，`POST /api/v1/auth/sms/send` 改为生成随机验证码并只保存 hash。
- `POST /api/v1/auth/sms/send` 响应不再返回 `mocked_code`，新增 `sent`、`expires_in_seconds`、`resend_in_seconds`、`provider_code` 等字段。
- `POST /api/v1/auth/login/sms` 会真实校验验证码状态，支持过期、已使用、错误次数过多和频控错误码。
- 本轮未修改 mobile-app 源码。

### 2. 新增/修改文件

- 修改：`docs/api/petlife-openapi.yaml`
- 修改：`docs/project/mobile-app-thread-summary.md`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 服务端实现和验证见 `docs/project/server-thread-summary.md` 同日记录。

### 3. 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 89, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`：通过。
- `git diff --check`：通过。

### 4. 未完成事项

- mobile-app 登录页仍需移除固定验证码展示、自动填充和 `mocked_code` 读取。
- 真实短信供应商尚未接入，当前 `dev_noop` 不会真实送达短信。

### 5. 风险或阻塞

- 移动端未适配前，用户无法从发送验证码响应中获得调试验证码。
- 后续接入真实短信供应商前，生产环境不能依赖 `dev_noop` 对外提供短信送达。

### 6. 下一步建议

1. mobile-app 按 OpenAPI 调整验证码发送响应模型，移除 `mocked_code` 字段。
2. 登录页改为用户手动输入短信验证码，并按新错误码展示过期、错误次数过多和发送频控提示。

## 2026-05-19 移动端通知与消息配置闭环收口

### 1. 新完成内容

- 消息中心已兼容服务端模板化后的通知字段，稳定展示 `title`、`content`、`notify_type`、`biz_type`、`sent_at` 和 `read_status`。
- 通知消息模型新增 `read_status` 读取，并保留旧 `read` 布尔字段兜底，避免服务端字段切换时已读态丢失。
- 消息中心保留系统、提醒、预约类型筛选和已读筛选；单条已读、全部已读增加统一成功/错误反馈和处理中态。
- 通知设置页确认继续读写 `/api/v1/me/settings/notifications` 的 `notification_enabled` 与 `privacy_level`，文案明确当前只控制站内消息中心与提醒接收偏好。
- 通知设置页新增渠道只读说明，明确短信和系统 Push 未在 App 内提供配置入口；本轮未接入 Firebase、APNs、厂商 Push 或短信 SDK。

### 2. 新增/修改文件

- 修改：`mobile-app/lib/shared/domain/models/notification_inbox_snapshot.dart`
- 修改：`mobile-app/lib/shared/repository/network_petlife_repository.dart`
- 修改：`mobile-app/lib/modules/notification/presentation/pages/message_center_page.dart`
- 修改：`mobile-app/lib/modules/profile/presentation/pages/notification_settings_page.dart`
- 修改：`mobile-app/lib/modules/profile/presentation/pages/settings_page.dart`
- 修改：`mobile-app/test/widget_test.dart`
- 修改：`docs/project/mobile-app-thread-summary.md`
- 修改：`docs/project/03-ui-closure-checklist.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/01-current-delivery-status.md`

### 3. 验证命令与结果

- `/Users/deng/development/flutter/bin/dart format lib/shared/domain/models/notification_inbox_snapshot.dart lib/shared/repository/network_petlife_repository.dart lib/modules/notification/presentation/pages/message_center_page.dart lib/modules/profile/presentation/pages/notification_settings_page.dart lib/modules/profile/presentation/pages/settings_page.dart test/widget_test.dart`：通过，`Formatted 6 files (0 changed)`。
- `/Users/deng/development/flutter/bin/flutter analyze`：通过，`No issues found!`。
- `/Users/deng/development/flutter/bin/flutter test`：通过，`All tests passed!`。
- `git diff --check`：通过。

### 4. 未完成事项

- 真实短信通道、系统 Push SDK、设备推送 token 上报和通知点击落点仍未接入，本轮按约束不实现。
- OpenAPI 暂未提供用户侧通知渠道状态查询接口，移动端无法展示真实短信/Push 可用状态。

### 5. 风险或阻塞

- `notification_channel_configs` 当前是后台配置能力，不是用户端设置能力；移动端不能基于后台配置构造本地短信或 Push 开关。
- 如果后续服务端新增用户可见的渠道状态接口，移动端只能按真实接口做只读展示，再评估是否需要系统权限与 token 契约。

### 6. 下一步建议

1. 服务端若需要用户端展示短信/Push 状态，应先在 OpenAPI 增加用户可见的只读渠道状态接口。
2. 真实 Push 闭环需服务端先明确 token 上报、设备解绑、通知点击落点和失效处理契约，再由移动端接入 SDK。

## 2026-05-19 服务端通知配置能力补齐对移动端影响

### 1. 新完成内容

- 服务端已补齐消息模板管理、通知渠道配置和通知配置审计接口。
- 站内欢迎、提醒完成/跳过、服务预约、审核结果通知已改为优先读取启用的 `inbox` 消息模板。
- 移动端通知列表、已读接口和返回字段保持不变；本轮未修改 mobile-app 源码。

### 2. 新增/修改文件

- 修改：`docs/project/mobile-app-thread-summary.md`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 服务端实现和接口文档见 `docs/project/server-thread-summary.md` 同日记录。

### 3. 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 81, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`：通过。
- `git diff --check`：通过。

### 4. 未完成事项

- 真实 Push 发送仍未接入；移动端暂不需要接入设备推送 token 或系统推送权限上报。
- admin-web 尚未接入消息模板与通知渠道配置页面。

### 5. 风险或阻塞

- 后台若修改 `inbox` 消息模板文案，移动端消息中心会展示新的服务端渲染结果；移动端不应硬编码这些文案。
- 真实 Push 接入前，移动端仍只消费站内消息中心接口。

### 6. 下一步建议

1. 移动端无需跟随本轮做代码调整。
2. 后续服务端明确 Push token 上报和系统推送契约后，再由移动端接入系统推送权限、token 上传和通知点击落点。

## 2026-05-19 移动端社区用户闭环接入

### 1. 新完成内容

- 社区首页新增独立发帖入口，发布成功后按帖子类型刷新推荐或问答流。
- 新增独立社区发布页，接入 `POST /api/v1/community/posts`，支持图文、视频、问答、经验类型、公开 / 关注可见、关联宠物、同城 city_code 和 `biz_type=community` 媒体上传。
- 新增话题页，接入 `GET /api/v1/community/topics/{topicId}`，展示话题信息、话题帖子列表，并支持从话题页携带 `topic_id` 参与发布。
- 新增问答详情页，接入 `GET /api/v1/community/questions/{questionId}`，展示问题、回答列表，并通过评论接口发布回答。
- 帖子详情页补齐话题入口、媒体预览、关注 / 取消关注和统一互动成功/错误反馈。
- 社区推荐、关注、同城、问答 tab 已与帖子详情、问答详情、话题页和发布页打通；问答类型内容进入问答详情，其余内容进入帖子详情。
- 社区媒体展示新增真实 `media_assets.access_url` 图片/视频预览，不使用本地 mock 或假数据。

### 2. 新增/修改文件

- 新增：`mobile-app/lib/modules/community/presentation/pages/community_post_editor_page.dart`
- 新增：`mobile-app/lib/modules/community/presentation/pages/community_topic_page.dart`
- 新增：`mobile-app/lib/modules/community/presentation/pages/community_question_detail_page.dart`
- 新增：`mobile-app/lib/modules/community/presentation/widgets/community_media_preview_grid.dart`
- 新增：`mobile-app/lib/modules/community/presentation/widgets/community_post_card.dart`
- 新增：`mobile-app/lib/modules/community/presentation/widgets/community_author_follow_button.dart`
- 修改：`mobile-app/lib/modules/community/presentation/pages/community_home_page.dart`
- 修改：`mobile-app/lib/modules/community/presentation/pages/community_post_detail_page.dart`
- 修改：`mobile-app/lib/shared/domain/models/community_post_snapshot.dart`
- 修改：`mobile-app/lib/shared/repository/petlife_repository.dart`
- 修改：`mobile-app/lib/shared/repository/network_petlife_repository.dart`
- 修改：`mobile-app/lib/modules/common/presentation/widgets/media_attachment_picker.dart`
- 修改：`mobile-app/test/widget_test.dart`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/03-ui-closure-checklist.md`
- 修改：`docs/project/mobile-app-thread-summary.md`

### 3. 验证命令与结果

- `cd mobile-app && /Users/deng/development/flutter/bin/dart format ...`
  - 结果：通过。
- `cd mobile-app && /Users/deng/development/flutter/bin/flutter analyze`
  - 结果：通过，`No issues found!`。
- `cd mobile-app && /Users/deng/development/flutter/bin/flutter test`
  - 结果：通过，`All tests passed!`。
- `git diff --check`
  - 结果：通过。

### 4. 未完成事项

- 第三方内容审核仍未接入，由服务端或第三方能力线程继续推进。
- 当前 OpenAPI 没有话题列表 / 话题搜索接口；移动端本轮只从服务端返回的帖子话题入口进入话题页，或从话题页发布时携带 `topic_id`，不构造本地话题假列表。

### 5. 风险或阻塞

- 发布页媒体上传必须使用 `biz_type=community`，服务端会校验 asset_id 归属和业务类型。
- `review_status` 只作为服务端返回展示字段读取，移动端发布时不提交审核态。
- 若产品后续要求用户在发帖页主动选择或搜索话题，需要服务端补充话题列表 / 搜索 OpenAPI。

### 6. 下一步建议

1. 服务端补充话题列表 / 搜索接口后，移动端可在发布页加入真实话题选择器。
2. 第三方内容审核接入后，移动端可按 `review_status` 增加更细的审核中、驳回原因和重新编辑入口。

## 2026-05-19 服务端社区闭环能力补齐待移动端接入

### 1. 新完成内容

- 服务端已补齐移动端社区后续页面所需接口：
  - `POST /api/v1/community/posts`
  - `GET /api/v1/community/posts/{postId}`
  - `POST /api/v1/community/users/{userId}/follow`
  - `DELETE /api/v1/community/users/{userId}/follow`
  - `GET /api/v1/community/users/{userId}/follow-status`
  - `GET /api/v1/community/topics/{topicId}`
  - `GET /api/v1/community/questions/{questionId}`
- `GET /api/v1/community/feed` 已支持 `recommended`、`following`、`city`、`qa` 四类真实服务端流。
- 社区帖子响应新增 `topic`、`media_asset_ids`、`media_assets`、`review_status` 字段；既有 `post_id`、`author`、`pet`、`like_count`、`comment_count`、`favorite_count`、`liked`、`favorited` 保持可用。
- 本轮未修改 mobile-app 源码。

### 2. 新增/修改文件

- 修改：`docs/api/petlife-openapi.yaml`
- 修改：`docs/technical/02-api-and-events.md`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/mobile-app-thread-summary.md`
- 服务端实现文件见 `docs/project/server-thread-summary.md` 同日记录。

### 3. 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 77, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`：通过。
- mobile-app 分析与测试未执行：本轮没有修改 mobile-app 源码。

### 4. 未完成事项

- mobile-app 尚未接入社区独立发布页、话题页、问答详情页和关注 / 取消关注交互。
- 第三方内容审核和推送通道仍未接入，不阻塞当前用户端社区读取与发布接口。

### 5. 风险或阻塞

- 独立发布的媒体必须先通过 `biz_type=community` 上传，不能复用日常或健康附件 asset_id。
- 用户端不能提交 `review_status`，帖子审核态由服务端控制。

### 6. 下一步建议

1. 按 OpenAPI 更新移动端社区数据模型，兼容新增 `topic` 与媒体元数据字段。
2. 接入发布页、话题页、问答详情页和关注交互，所有数据走真实服务端接口。

## 2026-05-18 首页专用聚合接口接入

### 1. 新完成内容

- 新增移动端首页聚合快照模型 `HomeAggregateSnapshot`。
- 仓储层新增 `getHomeAggregate()`，网络实现读取 `GET /api/v1/home`。
- App 主壳首页加载流程改为消费首页聚合接口，不再由首页入口串联当前用户与宠物面板多接口。
- 测试假仓储已同步补齐首页聚合方法。

### 2. 验证命令与结果

- `flutter analyze`：通过，`No issues found!`。
- `flutter test`：通过，`All tests passed!`。

## 2026-05-18 用户端提醒模板选择接入

### 1. 新完成内容

- 移动端新增提醒模板快照模型与仓储方法，读取 `GET /api/v1/pets/{petId}/reminder-templates`。
- 提醒新建页新增“从模板开始”区域，展示后台启用且适配当前宠物的模板。
- 选择模板后会预填提醒标题、提醒类型、提醒模式、周期值和周期单位；用户仍需确认提醒时间并手动保存。
- 模板加载态、空状态、错误重试和选中态已按当前陪伴式 UI 规范收口。

### 2. 新增/修改文件

- 新增：`mobile-app/lib/shared/domain/models/reminder_template_snapshot.dart`
- 修改：`mobile-app/lib/shared/repository/petlife_repository.dart`
- 修改：`mobile-app/lib/shared/repository/network_petlife_repository.dart`
- 修改：`mobile-app/lib/modules/reminder/presentation/pages/reminder_editor_page.dart`
- 修改：`docs/project/mobile-app-thread-summary.md`
- 服务端接口与 OpenAPI 变更见 `docs/project/server-thread-summary.md` 同日记录。

### 3. 验证命令与结果

- `dart format lib/shared/domain/models/reminder_template_snapshot.dart lib/shared/repository/petlife_repository.dart lib/shared/repository/network_petlife_repository.dart lib/modules/reminder/presentation/pages/reminder_editor_page.dart test/widget_test.dart`：通过。
- `flutter analyze`：通过，`No issues found!`。
- `flutter test`：通过，`All tests passed!`。
- `git diff --check`：通过。

### 4. 未完成事项

- 提醒编辑旧记录能力仍需服务端先定义更新接口和状态边界。
- 模板默认提前量目前作为模板信息展示，不自动推算提醒时间；当前提醒创建表单仍以用户选择的提醒时间为准。

### 5. 风险或阻塞

- 如果模板列表接口不可用，页面会展示错误重试，不回退到本地 mock 模板。
- 后台真实账号体系、通知模板和推送通道仍由服务端/admin-web 线程推进。

## 2026-05-18 服务端后台提醒模板管理补齐对移动端无接口变更

### 1. 新完成内容

- 服务端补齐后台提醒模板管理接口：列表、详情、创建、更新、启停。
- 本轮新增能力仅面向 admin-web 后续提醒模板页面，未修改 mobile-app 代码，未改变用户端提醒创建、列表、完成、跳过接口字段。
- 提醒模板暂未接入用户端模板选择流程。

### 2. 新增/修改文件

- 修改：`docs/project/mobile-app-thread-summary.md`
- 服务端实现与 OpenAPI 变更见 `docs/project/server-thread-summary.md` 同日记录。
- 本轮未修改 `mobile-app` 目录源码。

### 3. 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 65, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`：通过。
- `git diff --check`：通过。
- mobile-app 分析与测试未执行：本轮没有修改 mobile-app 源码或用户端接口字段。

### 4. 未完成事项

- mobile-app 侧继续按既有计划推进加载骨架、全局反馈组件化、社区发布页、话题页、问答详情页和关注关系。
- 若后续要做用户端模板选择，需要服务端先明确用户端模板读取接口和字段契约。

### 5. 风险或阻塞

- 本轮没有用户端接口字段变更；移动端不能提前假设模板选择能力已经可用。
- 后台真实账号体系、消息模板和通知渠道配置仍由服务端/admin-web 线程推进，不阻塞当前移动端提醒功能。

### 6. 下一步建议

1. mobile-app 继续按原计划推进当前 UI/交互收口任务。
2. 若后续服务端新增用户端提醒模板接口，再由 mobile-app 线程读取 OpenAPI 后接入。

## 2026-05-18 移动端加载骨架组件统一

### 1. 新完成内容

- 新增陪伴式加载组件：`CompanionPageLoading`、`CompanionLoadingState`、`CompanionSkeletonList`、`CompanionSkeletonCard`，覆盖整页、详情、列表和局部卡片加载场景。
- 替换当前 mobile-app 中 `rg` 可定位到的全部 `CircularProgressIndicator`，避免主加载态继续使用纯居中转圈。
- 首页入口、主壳首页数据加载、周/月报、宠物管理/详情、健康记录列表/详情、萌宠日常列表/详情、提醒列表、时间轴、消息中心、服务中心主要列表/详情/预约/评价区、设置页、家庭管理和社区首页/详情加载态已切换为统一骨架语言。
- 保持 `DESIGN.md` 的温暖、轻松、柔和、陪伴感方向，未引入新依赖或复杂状态管理。

### 2. 新增/修改文件

- 新增：`mobile-app/lib/modules/common/presentation/widgets/companion_loading.dart`
- 修改：`mobile-app/lib/app/entry/presentation/pages/app_entry_page.dart`
- 修改：`mobile-app/lib/modules/shell/presentation/pages/app_shell_page.dart`
- 修改：`mobile-app/lib/modules/home/presentation/pages/home_page.dart`
- 修改：`mobile-app/lib/modules/home/presentation/pages/pet_report_page.dart`
- 修改：`mobile-app/lib/modules/pet/presentation/pages/pet_management_page.dart`
- 修改：`mobile-app/lib/modules/pet/presentation/pages/pet_detail_page.dart`
- 修改：`mobile-app/lib/modules/health/presentation/pages/health_record_list_page.dart`
- 修改：`mobile-app/lib/modules/health/presentation/pages/health_record_detail_page.dart`
- 修改：`mobile-app/lib/modules/dailylog/presentation/pages/daily_log_list_page.dart`
- 修改：`mobile-app/lib/modules/dailylog/presentation/pages/daily_log_detail_page.dart`
- 修改：`mobile-app/lib/modules/reminder/presentation/pages/reminder_list_page.dart`
- 修改：`mobile-app/lib/modules/timeline/presentation/pages/timeline_page.dart`
- 修改：`mobile-app/lib/modules/notification/presentation/pages/message_center_page.dart`
- 修改：`mobile-app/lib/modules/profile/presentation/pages/settings_page.dart`
- 修改：`mobile-app/lib/modules/service/presentation/pages/service_placeholder_page.dart`
- 修改：`mobile-app/lib/modules/community/presentation/pages/community_home_page.dart`
- 修改：`mobile-app/lib/modules/community/presentation/pages/community_post_detail_page.dart`
- 修改：`mobile-app/lib/modules/family/presentation/pages/family_management_page.dart`
- 修改：`docs/project/03-ui-closure-checklist.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/mobile-app-thread-summary.md`

### 3. 验证命令与结果

- `cd mobile-app && /Users/deng/development/flutter/bin/dart format ...`
  - 结果：通过。
- `cd mobile-app && /Users/deng/development/flutter/bin/flutter analyze`
  - 结果：通过，`No issues found!`。
- `cd mobile-app && /Users/deng/development/flutter/bin/flutter test`
  - 结果：通过，`All tests passed!`。
- `git diff --check -- ...`
  - 结果：通过。

### 4. 未完成事项

- 全局 Snackbar / Toast / Confirm 的全量深层组件化仍未完成。
- 社区发布页、话题页、问答详情页、关注关系能力仍未完成，需等待服务端 OpenAPI 稳定。

### 5. 风险或阻塞

- 本轮仅统一加载视觉和结构，不改变接口、状态机或服务端能力。
- 部分局部“提交中 / 保存中”按钮文案仍保留为动作反馈，未改成骨架加载态。
- 社区发布、话题、问答详情、关注关系依赖服务端接口字段稳定。

### 6. 下一步建议

1. 继续推进全局 Snackbar / Toast / Confirm 的全量深层组件化，覆盖登录、首页快捷记录、社区互动、家庭邀请等剩余反馈。
2. 做一次移动端文案扫尾，重点校正按钮、空状态、风险操作和社区互动提示。
3. 等服务端 OpenAPI 明确后，再继续社区发布页、话题页、问答详情页与关注关系。

## 2026-05-18 服务端后台提醒查询补齐对移动端无接口变更

### 1. 新完成内容

- 服务端补齐后台系统提醒查询接口：`GET /api/v1/admin/reminders`、`GET /api/v1/admin/reminders/{reminderId}`。
- 本轮新增能力仅面向 admin-web 后续提醒后台页面，未修改 mobile-app 代码，未改变用户端提醒接口字段。
- 用户端现有提醒列表、创建、完成、跳过和周期提醒链路保持原接口口径。

### 2. 新增/修改文件

- 修改：`docs/project/mobile-app-thread-summary.md`
- 服务端实现与 OpenAPI 变更见 `docs/project/server-thread-summary.md` 同日记录。
- 本轮未修改 `mobile-app` 目录源码。

### 3. 验证命令与结果

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 -DskipTests compile`：通过。
- 使用提供的远程 MySQL 配置运行 `mvn -Dmaven.repo.local=/tmp/petlife-m2 test`：通过，`Tests run: 62, Failures: 0, Errors: 0, Skipped: 0`。
- `ruby -e "require 'yaml'; YAML.load_file('docs/api/petlife-openapi.yaml'); puts 'openapi yaml ok'"`：通过。
- `git diff --check`：通过。
- mobile-app 分析与测试未执行：本轮没有修改 mobile-app 源码或用户端接口字段。

### 4. 未完成事项

- mobile-app 侧仍按既有缺口继续推进加载骨架、全局反馈深层组件化、社区发布页、话题页、问答详情页和关注关系。
- 提醒模板管理属于后台运营能力，当前不影响 mobile-app 现有提醒功能。

### 5. 风险或阻塞

- 本轮没有用户端接口字段变更；若后续提醒模板能力需要移动端选择模板，需要先更新 OpenAPI 并在 mobile-app 线程单独评估。
- 后台真实账号体系和通知模板能力仍由服务端/admin-web 线程推进，不阻塞当前移动端提醒功能。

### 6. 下一步建议

1. mobile-app 继续按原计划推进加载骨架和全局反馈组件化。
2. 若后续服务端新增提醒模板用户端接口，再由 mobile-app 线程读取 OpenAPI 后接入。

## 2026-05-18 移动端表单校验与成功反馈收口

### 1. 新完成内容

- 完成高频表单校验错误态统一：健康记录编辑、萌宠日常编辑、宠物编辑、提醒编辑均接入 `CompanionFormNotice`，提交失败时展示统一顶部提示，并启用表单交互后自动校验。
- 完成成功反馈统一：保存成功、删除成功、预约提交成功、预约取消成功、评价提交成功、设置保存成功均接入 `showCompanionSuccessFeedback`。
- 目标范围内的接口错误反馈统一改为 `showCompanionErrorFeedback`，减少页面内散落 `SnackBar` 拼装。
- 扩展 `companion_feedback.dart`，集中提供确认弹层、成功/错误反馈、表单提示块和统一反馈色板。

### 2. 新增/修改文件

- 修改：`mobile-app/lib/modules/common/presentation/widgets/companion_feedback.dart`
- 修改：`mobile-app/lib/modules/health/presentation/pages/health_record_editor_page.dart`
- 修改：`mobile-app/lib/modules/dailylog/presentation/pages/daily_log_editor_page.dart`
- 修改：`mobile-app/lib/modules/pet/presentation/pages/pet_editor_page.dart`
- 修改：`mobile-app/lib/modules/reminder/presentation/pages/reminder_editor_page.dart`
- 修改：`mobile-app/lib/modules/health/presentation/pages/health_record_detail_page.dart`
- 修改：`mobile-app/lib/modules/dailylog/presentation/pages/daily_log_detail_page.dart`
- 修改：`mobile-app/lib/modules/pet/presentation/pages/pet_detail_page.dart`
- 修改：`mobile-app/lib/modules/family/presentation/pages/family_management_page.dart`
- 修改：`mobile-app/lib/modules/service/presentation/pages/service_placeholder_page.dart`
- 修改：`mobile-app/lib/modules/profile/presentation/pages/settings_page.dart`
- 修改：`mobile-app/lib/modules/profile/presentation/pages/notification_settings_page.dart`
- 修改：`docs/project/03-ui-closure-checklist.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/mobile-app-thread-summary.md`

### 3. 验证命令与结果

- `cd mobile-app && /Users/deng/development/flutter/bin/dart format ...`
  - 结果：通过。
- `cd mobile-app && /Users/deng/development/flutter/bin/flutter analyze`
  - 结果：通过，`No issues found!`。
- `cd mobile-app && /Users/deng/development/flutter/bin/flutter test`
  - 结果：通过，`All tests passed!`。
- `git diff --check -- ...`
  - 结果：通过。

### 4. 未完成事项

- 全局 Snackbar / Toast / Confirm 的全量深层组件化仍未完成，本轮只统一了目标范围内的成功、错误和表单校验反馈。
- 社区发布页、话题页、问答详情页、关注关系能力仍未完成，需等待服务端 OpenAPI 稳定。

### 5. 风险或阻塞

- 根目录 `DESIGN.md` 已恢复，后续移动端 UI 收口继续按该规范和现有 `AppTheme`、`CompanionCard`、`CompanionPill`、`CompanionEmptyState` 的视觉基线推进。
- 仍有非本轮目标页面保留原有局部 SnackBar，后续做全局 Snackbar / Toast 深层组件化时需要继续替换。
- 社区发布、话题、问答详情、关注关系依赖服务端接口字段稳定。

### 6. 下一步建议

1. 继续推进全局 Snackbar / Toast / Confirm 的全量深层组件化，覆盖登录、社区互动、家庭邀请、首页快捷记录等剩余页面。
2. 等服务端 OpenAPI 明确后，再继续社区发布页、话题页、问答详情页与关注关系。

## 2026-05-18 移动端基础反馈态收口

### 1. 新完成内容

- 完成移动端无网络、请求超时、会话过期、服务端异常、数据结构异常的统一异常分类。
- 调整 refresh token 失败处理：临时网络异常或超时不再被误判为登录失效，不会直接清空本地会话。
- 完成移动端风险操作确认弹层统一，使用陪伴式底部确认弹层替代散落的默认 `AlertDialog` 和局部自定义弹层。
- 覆盖健康记录删除、萌宠日常删除、家庭成员移出、服务预约取消、宠物归档/删除等风险操作。

### 2. 新增/修改文件

- 新增：`mobile-app/lib/modules/common/presentation/widgets/companion_feedback.dart`
- 修改：`mobile-app/lib/shared/network/api_client.dart`
- 修改：`mobile-app/lib/shared/network/api_exception.dart`
- 修改：`mobile-app/lib/modules/pet/presentation/pages/pet_detail_page.dart`
- 修改：`mobile-app/lib/modules/health/presentation/pages/health_record_detail_page.dart`
- 修改：`mobile-app/lib/modules/dailylog/presentation/pages/daily_log_detail_page.dart`
- 修改：`mobile-app/lib/modules/family/presentation/pages/family_management_page.dart`
- 修改：`mobile-app/lib/modules/service/presentation/pages/service_placeholder_page.dart`
- 修改：`docs/project/03-ui-closure-checklist.md`
- 修改：`docs/project/01-current-delivery-status.md`
- 修改：`docs/project/02-feature-completion-checklist.md`
- 新增：`docs/project/mobile-app-thread-summary.md`

### 3. 验证命令与结果

- `cd mobile-app && /Users/deng/development/flutter/bin/dart format ...`
  - 结果：通过。
- `cd mobile-app && /Users/deng/development/flutter/bin/flutter analyze`
  - 结果：通过，`No issues found!`。
- `cd mobile-app && /Users/deng/development/flutter/bin/flutter test`
  - 结果：通过，`All tests passed!`。
- `git diff --check -- ...`
  - 结果：通过。

### 4. 未完成事项

- 全局 Snackbar / Toast / Confirm 的全量深层组件化仍未完成。
- 社区发布页、话题页、问答详情页、关注关系能力仍未完成。

### 5. 风险或阻塞

- 真实对象存储/CDN、短信、推送、地图定位等第三方能力依赖服务端线程继续完成。
- 社区发布、话题、问答详情、关注关系需要服务端 OpenAPI 字段和接口稳定后再接入。
- 当前移动端依赖 `video_player` 预览视频，后续如果要求兼容更低 Flutter SDK，需要重新确认依赖版本边界。

### 6. 下一步建议

1. 继续推进全局 Snackbar / Toast / Confirm 的全量深层组件化。
2. 等服务端 OpenAPI 明确后，再继续社区发布页、话题页、问答详情页与关注关系。
