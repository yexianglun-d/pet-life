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
- 家庭治理查询能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/families`
  - `GET /api/v1/admin/families/{familyId}`
- 宠物主档查询能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/pets`
  - `GET /api/v1/admin/pets/{petId}`
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
  - `GET /api/v1/admin/moderation/audit-logs`
- 系统提醒查询能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/reminders`
  - `GET /api/v1/admin/reminders/{reminderId}`
- 提醒模板管理能力已同步到 `docs/api/petlife-openapi.yaml`：
  - `GET /api/v1/admin/reminder-templates`
  - `GET /api/v1/admin/reminder-templates/{templateId}`
  - `POST /api/v1/admin/reminder-templates`
  - `PATCH /api/v1/admin/reminder-templates/{templateId}`
  - `PATCH /api/v1/admin/reminder-templates/{templateId}/status`

## 当前已接入 admin-web 页面

- 用户治理查询接口已接入用户管理查询页，仅支持真实查询和详情查看。
- 家庭治理查询接口已接入家庭管理查询页，仅支持真实查询和详情查看。
- 宠物主档查询接口已接入宠物档案查询页，仅支持真实查询和详情查看。
- 系统提醒查询接口已接入系统提醒查询页，仅支持真实查询和详情查看。
- 提醒模板管理接口已接入提醒模板管理页，支持真实列表、筛选、详情、创建、编辑和启停。
- 写治理能力仍以服务端状态机、权限边界和审计动作定义为前置条件，不在前端提前占位。

## 待服务端线程补齐

| 清单项 | 后台页面目标 | 需要补齐的管理端接口 |
| --- | --- | --- |
| 2. 认证与会话 | 后台真实账号登录、退出和会话续期 | 需要服务端定义管理员账号模型、权限边界、登录/退出/刷新接口和审计写入 |
| 5. 宠物主档 | 宠物问题数据修复工具 | 需要服务端先定义可修复问题类型、状态机、审计动作和对应写接口 |
| 14. 通知与消息 | 消息模板管理、通知发送配置 | 需要服务端定义模板模型、渠道配置模型后补充查询与维护接口 |

## admin-web 开发顺序建议

1. 服务端线程先补 OpenAPI 与真实接口。
2. `admin-web` 按 OpenAPI 增加 API 类型与请求函数。
3. 页面只展示真实接口返回的数据；缺接口的按钮和写操作不提前占位。
4. 每完成一组真实页面，同步更新 `docs/project/02-feature-completion-checklist.md` 与本文件。
