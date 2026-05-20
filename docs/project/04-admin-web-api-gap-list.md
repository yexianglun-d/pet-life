# 后台管理端接口缺口清单

## 文档目的

本文件只记录 `admin-web` 要补齐真实后台能力时，服务端线程需要先提供或补充到 `docs/api/petlife-openapi.yaml` 的管理端接口。

约束：

- `admin-web` 不用用户端接口假装管理端能力。
- `admin-web` 不造假数据、不写本地 mock 业务数据。
- 接口进入 OpenAPI 前，只能做页面设计准备和缺口记录，不能标记为完成。
- 商城与设备仍按当前预留处理，不进入本清单。

## 当前已同步接口

- 用户治理查询能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/users`
  - `GET /api/v1/admin/users/{userId}`
  - `PATCH /api/v1/admin/users/{userId}/status`
- 家庭治理查询能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/families`
  - `GET /api/v1/admin/families/{familyId}`
  - `PATCH /api/v1/admin/families/{familyId}/status`
  - `POST /api/v1/admin/families/{familyId}/owner-member-repair`
- 宠物主档查询能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/pets`
  - `GET /api/v1/admin/pets/{petId}`
  - `POST /api/v1/admin/pets/{petId}/repair`
- 后台认证能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `POST /api/v1/admin/auth/login`
  - `POST /api/v1/admin/auth/refresh`
  - `POST /api/v1/admin/auth/logout`
- 短信验证码排查能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/sms-verifications`
  - `GET /api/v1/admin/sms-send-records`
- 服务中心现有后台能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/service/cities`
  - `POST /api/v1/admin/service/cities`
  - `GET /api/v1/admin/service/providers`
  - `POST /api/v1/admin/service/providers`
  - `PATCH /api/v1/admin/service/providers/{providerId}`
  - `POST /api/v1/admin/service/providers/{providerId}/items`
  - `PATCH /api/v1/admin/service/providers/{providerId}/items/{serviceItemId}`
  - `POST /api/v1/admin/service/providers/{providerId}/slots`
  - `PATCH /api/v1/admin/service/providers/{providerId}/slots/{slotId}`
  - `GET /api/v1/admin/service/appointments`
  - `PATCH /api/v1/admin/service/appointments/{appointmentId}/status`
  - `GET /api/v1/admin/service/reviews`
  - `PATCH /api/v1/admin/service/reviews/{reviewId}/status`
  - `GET /api/v1/admin/service/audit-logs`
- 内容治理查询能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/health-records`
  - `GET /api/v1/admin/health-records/{healthRecordId}`
  - `GET /api/v1/admin/daily-logs`
  - `GET /api/v1/admin/daily-logs/{dailyLogId}`
  - `GET /api/v1/admin/timeline/events`
  - `GET /api/v1/admin/timeline/events/{eventId}`
- 审核治理能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `PATCH /api/v1/admin/moderation/reports/{reportId}` 支持 `admin_notes` 入库与回显
  - `GET /api/v1/admin/moderation/tasks`
  - `GET /api/v1/admin/moderation/tasks/{taskId}`
  - `PATCH /api/v1/admin/moderation/tasks/{taskId}/status`
  - `GET /api/v1/admin/moderation/audit-logs`
- 社区内容治理能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/community/posts`
  - `GET /api/v1/admin/community/posts/{postId}`
  - `PATCH /api/v1/admin/community/posts/{postId}/status`
  - `GET /api/v1/admin/community/questions`
  - `GET /api/v1/admin/community/questions/{questionId}`
  - `PATCH /api/v1/admin/community/questions/{questionId}/status`
  - `GET /api/v1/admin/moderation/audit-logs` 已支持 `community_post`、`community_question` 审计目标
- 系统提醒查询能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/reminders`
  - `GET /api/v1/admin/reminders/{reminderId}`
- 提醒模板管理能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/reminder-templates`
  - `GET /api/v1/admin/reminder-templates/{templateId}`
  - `POST /api/v1/admin/reminder-templates`
  - `PATCH /api/v1/admin/reminder-templates/{templateId}`
  - `PATCH /api/v1/admin/reminder-templates/{templateId}/status`
- 通知与消息配置能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/message-templates`
  - `GET /api/v1/admin/message-templates/{templateId}`
  - `POST /api/v1/admin/message-templates`
  - `PATCH /api/v1/admin/message-templates/{templateId}`
  - `PATCH /api/v1/admin/message-templates/{templateId}/status`
  - `GET /api/v1/admin/notification-channels`
  - `GET /api/v1/admin/notification-channels/{channelConfigId}`
  - `POST /api/v1/admin/notification-channels`
  - `PATCH /api/v1/admin/notification-channels/{channelConfigId}`
  - `PATCH /api/v1/admin/notification-channels/{channelConfigId}/status`
  - `GET /api/v1/admin/push-tasks`
  - `GET /api/v1/admin/push-deliveries`
  - `GET /api/v1/admin/notification/audit-logs`

## 当前已接入 admin-web 页面

- 后台认证接口已接入登录页与退出流程。
- 用户治理接口已接入用户管理查询页，支持真实查询、详情查看、封禁和恢复。
- 家庭治理接口已接入家庭管理查询页，支持真实查询、详情查看、停用/恢复和 owner 成员关系修复。
- 宠物主档接口已接入宠物档案查询页，支持真实查询、详情查看和问题数据修复。
- 系统提醒查询接口已接入系统提醒查询页，仅支持真实查询和详情查看。
- 提醒模板管理接口已接入提醒模板管理页，支持真实列表、筛选、详情、创建、编辑和启停。
- 短信验证码排查接口已接入验证码排查页，支持真实发送记录、校验记录、状态排查、详情查看和手机号脱敏展示。
- 社区内容治理接口已接入社区帖子治理页和问答治理页，支持真实列表筛选、详情查看、下架/恢复和治理审计查询。
- 审核任务接口已接入内容审核任务页，支持真实任务列表、详情、审核快照、回调 payload、任务审计展示和人工通过/拒绝。
- 通知与消息配置接口已接入消息模板管理页和通知发送配置页，支持真实列表筛选、详情查看、创建、编辑、启停和配置审计查询。
- Push 任务和投递记录接口已接入 Push 投递排查页，支持真实任务/投递记录查询、状态排查、失败原因和设备标识脱敏展示。
- 写治理能力已使用服务端状态机、权限边界和审计动作接口，不使用前端本地 mock。

## 待后续补齐或接入

| 清单项 | 后台页面目标 | 需要补齐的管理端接口 |
| --- | --- | --- |
| 2. 认证与会话 | 短信验证码发送/校验排查按服务端时间范围查询 | 现有接口未提供 `created_from` / `created_to` 或等价时间范围参数，当前 admin-web 基于真实返回时间字段做页面内收窄 |
| 12. 审核与举报处理 | 审核任务列表按关键词和时间范围服务端查询 | 现有 `GET /api/v1/admin/moderation/tasks` 未提供 `keyword`、`created_from`、`created_to` 或等价参数，当前 admin-web 基于真实返回字段做页面内收窄 |
| 14. 通知与消息 | Push 任务和投递记录按 `notify_type` 与时间范围服务端查询 | 现有 `GET /api/v1/admin/push-tasks` 未提供 `notify_type`、`created_from`、`created_to` 参数，`GET /api/v1/admin/push-deliveries` 未提供时间范围参数，当前 admin-web 基于真实返回字段做页面内收窄 |
| 14. 通知与消息 | Push 投递记录展示设备 token 脱敏值 | 现有 `PushDeliveryRecord` 只返回 `device_token_id`，不返回真实 token 或服务端生成的 `masked_device_token`；admin-web 只能脱敏展示设备标识，不能定位真实 token 原文 |
| 14. 通知与消息 | 真实短信 / Push 供应商接入后的密钥配置、发送报表、通道健康检查 | 需服务端先定义供应商接入、发送状态和健康检查接口 |

## admin-web 开发顺序建议

1. 服务端线程先补 OpenAPI 与真实接口。
2. `admin-web` 按 OpenAPI 增加 API 类型与请求函数。
3. 页面只展示真实接口返回的数据；缺接口的按钮和写操作不提前占位。
4. 每完成一组真实页面，同步更新 `docs/project/02-feature-completion-checklist.md` 与本文件。
