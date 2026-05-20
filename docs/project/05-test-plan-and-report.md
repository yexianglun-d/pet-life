# 测试计划与报告

## 测试数据脚本路径

- 测试数据脚本：`docs/technical/10-test-data-seed.sql`
- 结构参考：`docs/technical/03-ddl-draft.sql`
- 真实库结构校验：上轮已通过 JDBC 只读查询 `information_schema.columns`，当时 `pet_life` 库识别到 47 张业务表；当前 DDL 草案已扩展到 50 张表，本轮因本机缺少数据源环境变量和 `mysql` CLI，尚未对真实库重新校验新增表。

## 测试数据隔离策略

- 手机号段：`19900008xxx`
- 名称前缀：`[TEST]`
- 媒体对象路径前缀：`test-data/petlife/`
- 订单号、设备序列号、事件去重键前缀：`PLT-SEED-`
- 后台账号前缀：`plt_`
- 服务城市测试编码：`PLT_SH`、`PLT_HZ`、`PLT_CD`

脚本不使用 `TRUNCATE`，不清空整表；每次执行前只按上述前缀、手机号段、测试编码反向清理测试数据，再重新插入。脚本不使用高位固定主键，避免推高真实库自增 ID。

## 测试数据覆盖说明

- 用户与认证：普通用户、禁用用户、上海/杭州/成都三地用户、用户设置、用户会话、短信验证码发送记录与校验记录。
- 家庭与成员：正常家庭、停用家庭、owner/admin/member/pending 多角色成员、家庭邀请。
- 宠物：猫、犬、其他类型宠物，覆盖 active 和 memorial 状态，包含体重、过敏、病史、头像等真实字段。
- 健康与提醒：疫苗、驱虫、体检、用药、观察记录，覆盖 pending/done/skipped、single/cycle、来源健康记录和提醒模板。
- 媒体、日常、时间轴：健康附件、日常图片、社区视频、萌宠日常、社区同步、成长时间轴健康/日常/服务/设备事件。
- 社区：话题、图文帖、问答帖、评论、回复、点赞、收藏、关注、举报、人工治理任务。
- 服务中心：城市开通配置、医院/洗护服务商、服务项目、时段、待确认预约、已完成预约、已取消预约、评价。
- 通知：站内通知、消息模板、通知渠道配置、outbox 成功/失败样本。
- 内容审核与 Push 底座：审核任务覆盖 `pending`、人工 `approved`、人工 `rejected`，并明确 `dev_noop` 与 `manual` 均不代表真实第三方审核；Push 数据覆盖设备 token、`dev_noop` pending 任务、pending 投递记录和 `notification_switch_off` skipped 排查记录。
- 后台：后台账号、后台会话、审计日志、社区治理记录。
- 预留模块：商城和设备链路提供结构性样本，并在数据内容中标识 `该功能待完善`。

## 已完成可测功能清单

- 用户登录基础链路、用户资料、用户设置、当前宠物读取。
- 家庭、家庭成员、角色数据展示与后台查询。
- 宠物主档、宠物详情、宠物状态与后台宠物查询。
- 健康记录、附件元数据、提醒派生、提醒计划、用户端提醒模板读取与表单预填。
- 萌宠日常、媒体资源引用、社区同步、成长时间轴展示。
- 社区推荐、同城、问答、评论、点赞、收藏、关注、举报与后台治理。
- 内容审核任务底座、公开流审核过滤、人工通过、人工拒绝、后台审核任务查询与详情。
- 服务城市配置、服务商、服务项目、预约、取消、完成、评价与后台运营查询。
- 站内通知、消息模板、通知渠道配置、Push token 注册/解绑、Push 任务和投递排查底座。
- 后台账号、后台会话、审计日志基础查询。

## 当前预留/待完善功能清单

- 真实短信供应商 SDK 接入：该功能待完善。
- 对象存储云厂商上传适配器与 CDN：该功能待完善。
- 第三方内容审核接入：该功能待完善。
- Push 推送通知通道：该功能待完善。
- 地图定位、导航、距离排序：该功能待完善。
- 商城模块：当前预留，该功能待完善。
- 设备联动模块：当前预留，该功能待完善。
- 完整监控告警、outbox 补偿、发布级审计加固：该功能待完善。

上述项目属于明确预留或外部集成未完成范围，不计入缺陷。

## 执行过的命令与结果

- `git status --short --branch`
  - 结果：当前工作区已有服务端、admin-web、mobile-app 的内容审核与 Push 底座改动；测试线程本轮只修改 `docs/technical/10-test-data-seed.sql` 与 `docs/project/05-test-plan-and-report.md`。
- `rg -n "CREATE TABLE|ALTER TABLE" docs/technical/03-ddl-draft.sql`
  - 结果：确认 DDL 草案覆盖 50 张业务表，包含当前测试数据要求的核心链路、内容审核/Push 底座和预留链路。
- `jshell --class-path mysql-connector-j ... information_schema.columns`
  - 结果：上轮真实 `pet_life` 数据库可只读连接，识别到 47 张表，表结构与当时 DDL 草案主干一致；新增内容审核/Push 表尚待重新连接验证。
- `jshell --class-path spring-security-crypto ... BCryptPasswordEncoder`
  - 结果：生成测试后台账号 `plt_ops_admin` 使用的 BCrypt 密码摘要，明文约定为 `petlife123`，仅用于测试数据。
- `jshell --class-path mysql-connector-j ... docs/technical/10-test-data-seed.sql`
  - 结果：上轮将脚本末尾 `COMMIT` 替换为 `ROLLBACK` 后执行真实库回滚验证，202 条语句全部通过；本轮已新增内容审核/Push 数据，尚待可用测试库环境下重新回滚验证。
- `node <<'NODE' ... docs/technical/03-ddl-draft.sql docs/technical/10-test-data-seed.sql`
  - 结果：静态比对 `moderation_tasks`、`user_push_device_tokens`、`push_tasks`、`push_delivery_records` 的测试数据插入列与当前 DDL；4 张表均未发现旧列或不存在列。
- `mvn -q -Dtest=PhaseOneApiTests test`
  - 结果：未进入业务回归断言；当前 shell 未设置 `PETLIFE_DATASOURCE_URL`、`PETLIFE_DATASOURCE_USERNAME`、`PETLIFE_DATASOURCE_PASSWORD`，Spring 使用占位符 `${PETLIFE_DATASOURCE_URL}` 启动导致数据源初始化失败。
- `mysql --version`、`mysql -uroot -e "SHOW DATABASES LIKE 'pet_life';"`
  - 结果：本机当前 shell 未找到 `mysql` 命令，无法在本轮对真实库执行提交式或回滚式 SQL 验证。
- `npm run type-check`（admin-web）
  - 结果：通过。
- `npm run build`（admin-web）
  - 结果：通过；Vite 输出单包超过 500 kB 的体积提示，该提示为构建优化建议，未阻断本轮验收。
- `/Users/deng/development/flutter/bin/flutter test`（mobile-app）
  - 结果：通过，`All tests passed!`。
- `/Users/deng/development/flutter/bin/flutter analyze`（mobile-app）
  - 结果：通过，`No issues found!`。
- `rg -n "review_status = 'approved'|notification_switch|dev_noop|manual|content_snapshot|device_token" server/src/main/java admin-web/src mobile-app/lib docs/api/petlife-openapi.yaml`
  - 结果：静态证据显示用户侧社区公开查询过滤 `approved`；后台审核任务页展示 `content_snapshot`、`callback_payload` 并标注 `dev_noop/manual` 边界；Push 排查页不展示设备 token 原文，只展示 `device_token_id`；服务端 Push provider 当前 `dispatchEnabled=false`。

## 缺陷清单

暂无已确认业务缺陷。

待确认口径：
- `notification_switch` 关闭时，服务端当前实现会记录一条 `push_tasks.task_status=skipped`、`failure_reason=notification_switch_off` 的排查记录，但不会生成站内通知，也不会生成可投递 pending 任务或投递记录。本报告暂按“不可真实派发 Push”为验收口径；如果产品口径要求 `push_tasks` 零记录，则该项应转为服务端缺陷，责任线程为 server。

当前报告没有把预留能力或外部供应商未接入列为缺陷；只有已声明完成但行为不正确的问题才进入缺陷清单。

## 阻断项

- 当前未在真实库执行本轮更新后的 `docs/technical/10-test-data-seed.sql` 提交式或回滚式写入验证；上轮旧脚本曾通过真实库事务回滚验证，正式灌数仍建议在独立测试库执行。
- 如果目标环境没有完整执行 `03-ddl-draft.sql` 及后续增量表结构，脚本会因缺表或缺列失败。需要先补齐表结构迁移。
- 服务端自动化回归当前被本机测试环境阻断：未设置 `PETLIFE_DATASOURCE_URL`、`PETLIFE_DATASOURCE_USERNAME`、`PETLIFE_DATASOURCE_PASSWORD`，且 shell 中没有 `mysql` CLI；需要服务端线程或本机环境提供可用测试库连接后重跑 `PhaseOneApiTests`。

## 下一轮建议

- 在独立测试库执行 `docs/technical/10-test-data-seed.sql`，记录最终 `SELECT` 汇总结果。
- 服务端补齐测试库环境变量后，重跑 `mvn -q -Dtest=PhaseOneApiTests test`，重点确认审核中内容不进入公开流、人工通过可见、人工拒绝不可见、审核任务详情脱敏和 Push 开关边界。
- 基于测试手机号 `19900008001`、`19900008002` 和后台账号 `plt_ops_admin` 进行用户端、后台端手工验收。
- 为脚本中的主链路补充 Postman/HTTP smoke case，覆盖登录、当前宠物、提醒模板、健康记录、社区、服务预约、通知和后台治理查询。
- 待外部供应商能力进入实现阶段后，将 `该功能待完善` 项从预留清单迁移到可测清单，并补充真实供应商回调和失败补偿数据。
