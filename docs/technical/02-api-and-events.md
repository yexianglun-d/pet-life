# API 契约与领域事件设计

## 0. 当前实施范围说明

1. 当前实现用户端 App 所需接口，并同步建设后台必需接口。
2. 商城接口与设备接口保留为预留契约，不进入当前开发批次。
3. 后台 Web 仅覆盖当前必需能力：举报处理、审核处理、服务商数据维护和基础配置。

## 1. 接口约定

## 1.1 基础约定

- Base URL：`/api/v1`
- 协议：`HTTPS + JSON`
- 字段命名：请求与响应统一使用 `snake_case`
- 时间格式：ISO 8601，统一使用 UTC 存储，客户端按本地时区展示
- ID 类型：后端使用 `bigint`，接口响应统一返回字符串，避免前端精度问题

## 1.2 鉴权约定

- Header：`Authorization: Bearer <access_token>`
- refresh token 单独通过安全接口换取，不混入普通业务接口
- 管理端使用独立的后台账号体系或后台角色声明，不与普通 App 用户混用权限

## 1.3 宠物上下文约定

关键决策：

- 所有修改宠物相关事实数据的接口，必须显式带 `pet_id`
- 只读聚合接口可在缺省时使用用户 `current_pet_id`

原因：

- 避免当前宠物切换时写错宠物
- 让核心写接口天然可审计和可追踪

## 1.4 分页约定

动态流接口统一使用游标分页：

```json
{
  "items": [],
  "next_cursor": "opaque_cursor",
  "has_more": true
}
```

适用：

- 社区流
- 成长时间轴
- 萌宠日常列表
- 通知列表

后台管理列表可以使用页码分页。

## 1.5 幂等约定

当前批次要求支持 `Idempotency-Key`：

- 医院/服务预约创建

预留契约：

- 创建订单
- 创建设备绑定
- 内部设备事件接入

## 1.6 统一响应结构

成功：

```json
{
  "code": "OK",
  "message": "success",
  "data": {}
}
```

失败：

```json
{
  "code": "PET_PERMISSION_DENIED",
  "message": "no permission for current pet",
  "request_id": "trace_id"
}
```

## 2. 认证与用户接口

## 2.1 发送验证码

`POST /auth/sms/send`

请求：

```json
{
  "mobile": "13800000000",
  "scene": "login"
}
```

校验：

- mobile 格式
- 图形验证是否通过
- 发送频率限制

## 2.2 短信登录

`POST /auth/login/sms`

请求：

```json
{
  "mobile": "13800000000",
  "code": "123456"
}
```

响应关键字段：

- `access_token`
- `refresh_token`
- `user`
- `family_summary`
- `pets`
- `current_pet_id`

## 2.3 刷新 token

`POST /auth/refresh`

请求：

```json
{
  "refresh_token": "opaque_refresh_token"
}
```

说明：

- 成功后返回新的 `access_token` 与 `refresh_token`
- 刷新时会吊销旧会话并轮换新会话，避免旧刷新令牌继续复用

## 2.4 退出登录

`POST /auth/logout`

请求：

```json
{
  "refresh_token": "opaque_refresh_token"
}
```

说明：

- 服务端按刷新令牌对应会话执行吊销
- 客户端收到成功后需同步清理本地会话

## 2.5 获取当前用户信息

`GET /me`

返回：

- 用户基础资料
- 当前城市
- 当前宠物
- 家庭摘要

说明：

- 若用户当前没有任何活跃宠物，`current_pet_id` 和 `current_pet` 会返回 `null`

## 2.6 修改当前宠物

`PATCH /me/settings/current-pet`

请求：

```json
{
  "pet_id": "10001"
}
```

## 3. 宠物与家庭接口

## 3.1 获取宠物列表

`GET /pets`

## 3.2 创建宠物

`POST /pets`

请求关键字段：

- `pet_name`
- `pet_type`
- `breed`
- `gender`
- `birthday`
- `adopt_date`
- `neuter_status`
- `avatar_asset_id`
- `weight_kg`
- `allergy_notes`
- `medical_history`

## 3.3 获取宠物详情

`GET /pets/{pet_id}`

## 3.4 更新宠物

`PATCH /pets/{pet_id}`

支持与创建接口相同的扩展字段回写。

## 3.5 获取宠物主页摘要

`GET /pets/{pet_id}/summary`

返回聚合：

- 基础档案
- 今日待办数
- 最近健康记录
- 最近日常
- 最近时间轴
- 设备状态摘要

## 3.6 归档宠物

`PATCH /pets/{pet_id}/archive`

请求：

```json
{
  "archive_status": "memorial"
}
```

说明：

- `archive_status` 仅支持 `memorial` 或 `rehomed`
- 归档后宠物会从活跃列表移出
- 若该宠物正被家庭成员作为当前宠物使用，服务端会自动重建当前宠物上下文

## 3.7 删除宠物

`DELETE /pets/{pet_id}`

说明：

- 服务端执行软删除，不做物理删库
- 删除后会自动重建所有受影响成员的 `current_pet_id`
- 若删除后用户没有任何活跃宠物，`/me` 将返回无宠物态

## 3.8 获取家庭信息

`GET /family`

## 3.9 创建或初始化家庭

`POST /family`

## 3.10 发起家庭邀请

`POST /family/invitations`

请求：

```json
{
  "invitee_mobile": "13800000000",
  "role": "member",
  "shared_pet_ids": ["10001", "10002"]
}
```

## 3.11 查看家庭邀请

`GET /family/invitations/{invite_code}`

## 3.12 接受家庭邀请

`POST /family/invitations/{invite_code}/accept`

## 3.11 拒绝家庭邀请

`POST /family/invitations/{invite_code}/reject`

## 3.12 修改成员角色

`PATCH /family/members/{member_id}/role`

## 3.13 移除家庭成员

`DELETE /family/members/{member_id}`

## 4. 健康记录与提醒接口

## 4.1 获取健康记录列表

`GET /pets/{pet_id}/health-records?record_type=vaccine&cursor=...`

## 4.2 创建健康记录

`POST /pets/{pet_id}/health-records`

请求关键字段：

```json
{
  "record_type": "vaccine",
  "title": "狂犬疫苗",
  "occurred_at": "2026-04-20T10:30:00Z",
  "hospital_name": "某宠物医院",
  "doctor_name": "张医生",
  "result_summary": "接种完成",
  "notes": "无异常",
  "attachment_asset_ids": ["90001"]
}
```

## 4.3 获取健康记录详情

`GET /pets/{pet_id}/health-records/{health_record_id}`

## 4.4 编辑健康记录

`PATCH /pets/{pet_id}/health-records/{health_record_id}`

## 4.5 删除健康记录

`DELETE /pets/{pet_id}/health-records/{health_record_id}`

## 4.6 获取提醒列表

`GET /pets/{pet_id}/reminders?status=pending`

## 4.7 创建提醒

`POST /pets/{pet_id}/reminders`

请求关键字段：

- `reminder_type`
- `title`
- `reminder_mode`
- `cycle_value`
- `cycle_unit`
- `due_at`
- `notes`

说明：

- `reminder_mode=single` 表示单次提醒，可不传 `cycle_value` 和 `cycle_unit`
- `reminder_mode=cycle` 表示周期提醒，必须同时传 `cycle_value` 与 `cycle_unit`
- `cycle_unit` 当前支持 `day`、`week`、`month`

## 4.8 完成提醒

`PATCH /pets/{pet_id}/reminders/{reminder_id}/complete`

说明：

- 仅 `pending` 状态的提醒允许完成
- 周期提醒完成后会自动生成下一条 `pending` 提醒，下一次时间基于原计划时间顺延推算

## 4.9 跳过提醒

`PATCH /pets/{pet_id}/reminders/{reminder_id}/skip`

说明：

- 仅 `pending` 状态的提醒允许跳过
- 周期提醒跳过后同样会自动生成下一条 `pending` 提醒

## 5. 萌宠日常与时间轴接口

## 5.1 申请上传凭证

`POST /media/upload-policy`

请求：

```json
{
  "biz_type": "daily_log",
  "file_name": "cat.jpg",
  "content_type": "image/jpeg"
}
```

响应：

- 上传地址
- 表单签名
- `asset_id`

## 5.2 确认媒体上传

`POST /media/assets/{asset_id}/complete`

## 5.3 获取萌宠日常列表

`GET /pets/{pet_id}/daily-logs`

## 5.4 创建萌宠日常

`POST /pets/{pet_id}/daily-logs`

请求关键字段：

```json
{
  "content": "今天第一次散步，出门有点紧张，但表现很好",
  "tags": ["散步", "成长"],
  "visibility": "family",
  "sync_to_community": false,
  "happened_at": "2026-04-20T09:00:00Z"
}
```

说明：

- 当前阶段先支持文字内容、标签、可见范围和记录时间
- `sync_to_community` 仅在 `visibility=public` 时允许开启
- 打开同步后会生成或更新对应社区帖子；关闭同步或改为非公开时会自动撤回社区帖子

## 5.5 获取萌宠日常详情

`GET /pets/{pet_id}/daily-logs/{daily_log_id}`

## 5.6 更新萌宠日常

`PATCH /pets/{pet_id}/daily-logs/{daily_log_id}`

## 5.7 删除萌宠日常

`DELETE /pets/{pet_id}/daily-logs/{daily_log_id}`

## 5.8 获取成长时间轴

`GET /pets/{pet_id}/timeline?event_type=all`

说明：

- 当前阶段支持 `all`、`health`、`daily_log` 三种筛选值
- 已接入健康记录与萌宠日常两类派生事件
- 时间轴为只读聚合视图，详情编辑仍回到源记录页处理

## 6. 社区接口

## 6.1 获取社区流

`GET /community/feed?tab=recommended`

返回卡片字段：

- `post_id`
- `author`
- `pet`
- `title`
- `content`
- `source_daily_log_id`
- `like_count`
- `comment_count`
- `favorite_count`
- `liked`
- `favorited`
- `published_at`

## 6.2 创建社区帖子

当前批次不开放独立发布接口。

真实发布来源：

- 萌宠日常创建/更新时，若 `visibility=public` 且 `sync_to_community=true`，系统会自动生成或更新社区帖子
- 当前社区帖子类型固定为 `experience`

## 6.3 获取帖子详情

`GET /community/posts/{post_id}`

说明：

- 返回当前用户视角下的 `liked`、`favorited` 状态
- 当前内容详情页已接入真实点赞、收藏、评论交互

## 6.4 评论帖子

`POST /community/posts/{post_id}/comments`

请求：

```json
{
  "content": "这条观察很真实，能看出已经越来越放松了。"
}
```

补充：

- `GET /community/posts/{post_id}/comments` 已提供评论列表读取
- 当前阶段仅支持一级评论，不开放楼中楼

## 6.5 点赞帖子

`POST /community/posts/{post_id}/like`

## 6.6 取消点赞

`DELETE /community/posts/{post_id}/like`

## 6.7 收藏帖子

`POST /community/posts/{post_id}/favorite`

## 6.8 取消收藏

`DELETE /community/posts/{post_id}/favorite`

## 6.9 举报帖子

`POST /community/posts/{post_id}/report`

请求关键字段：

```json
{
  "reason_code": "spam",
  "reason_detail": "连续出现重复引流内容"
}
```

补充：

- 当前阶段支持的 `reason_code`：`spam`、`pornography`、`harassment`、`illegal`、`other`
- 当 `reason_code=other` 时，`reason_detail` 必填
- 同一用户对同一帖子存在 `pending` 举报时，接口返回已有举报，不重复创建新记录

## 6.10 获取话题详情流

`GET /community/topics/{topic_id}`

## 7. 服务与预约接口

## 7.1 服务首页聚合

`GET /services/home?pet_id=10001`

## 7.2 获取服务商列表

`GET /providers?provider_type=hospital&city_code=310000&sort=distance`

## 7.3 获取服务商详情

`GET /providers/{provider_id}`

## 7.4 创建预约

`POST /appointments`

请求关键字段：

```json
{
  "pet_id": "10001",
  "provider_id": "70001",
  "appointment_type": "hospital",
  "appointment_date": "2026-04-22",
  "appointment_slot": "10:00-11:00",
  "demand_desc": "最近轻微拉稀，想检查",
  "contact_name": "李明",
  "contact_mobile": "13800000000"
}
```

## 7.5 获取预约记录

`GET /appointments?status=pending_confirm`

## 7.6 取消预约

`PATCH /appointments/{appointment_id}/cancel`

## 8. 商城与订单接口

## 8.1 商城首页聚合

`GET /mall/home?pet_id=10001`

## 8.2 获取商品列表

`GET /products?category_code=food&pet_type=cat&age_stage=adult`

## 8.3 获取商品详情

`GET /products/{product_id}`

## 8.4 获取购物车

`GET /cart`

## 8.5 加入购物车

`POST /cart/items`

请求：

```json
{
  "pet_id": "10001",
  "sku_id": "80001",
  "quantity": 2
}
```

## 8.6 更新购物车项

`PATCH /cart/items/{cart_item_id}`

## 8.7 删除购物车项

`DELETE /cart/items/{cart_item_id}`

## 8.8 订单预览

`POST /orders/preview`

用途：

- 核价
- 校验库存
- 校验地址

## 8.9 创建订单

`POST /orders`

请求关键字段：

```json
{
  "pet_id": "10001",
  "cart_item_ids": ["60001", "60002"],
  "address_id": "91001",
  "buyer_remark": "工作日白天收货"
}
```

## 8.10 获取订单列表

`GET /orders?status=pending_pay`

## 8.11 获取订单详情

`GET /orders/{order_id}`

## 9. 设备接口

## 9.1 获取设备列表

`GET /pets/{pet_id}/devices`

## 9.2 创建设备绑定

`POST /device-bindings`

请求关键字段：

```json
{
  "pet_id": "10001",
  "device_type": "feeder",
  "device_sn": "SN123456789",
  "bind_name": "客厅喂食器"
}
```

## 9.3 获取设备详情

`GET /devices/{device_id}`

## 9.4 解绑设备

`DELETE /device-bindings/{binding_id}`

## 9.5 设备事件接入接口

内部接口：

`POST /internal/device-events/ingest`

校验：

- 厂商签名
- 幂等键
- 设备绑定状态

## 10. 首页与通知接口

## 10.1 首页聚合

`GET /home`

返回：

- 当前宠物卡
- 今日待办
- 快捷记录配置
- 最近日常
- 社区推荐
- 服务入口
- 设备摘要

说明：

- 当前该接口仍为预留契约，App 首页展示暂时由现有宠物摘要、提醒、健康记录、萌宠日常接口组合得到
- 当前批次首页已落地“快捷记录 / 提醒中心 / 周报 / 月报”，但没有单独新增首页写接口
- 快捷记录复用既有事实接口：
  - `喂食 / 饮水 / 排便` 通过 `POST /pets/{pet_id}/daily-logs` 写入标准化萌宠日常
  - `体重 / 用药` 通过 `POST /pets/{pet_id}/health-records` 写入健康记录
  - `记日常` 直接进入日常编辑流程

## 10.2 首页周报

`GET /home/reports/weekly`

返回关键字段：

- `report_type`
- `pet`
- `window_start`
- `window_end`
- `pending_reminder_count`
- `completed_reminder_count`
- `skipped_reminder_count`
- `health_record_count`
- `daily_log_count`
- `community_sync_count`
- `feed_count`
- `water_count`
- `toilet_count`
- `weight_record_count`
- `medication_record_count`
- `highlights`
- `recent_reminders`
- `recent_health_records`
- `recent_daily_logs`

说明：

- 统计窗口为“最近 7 天滚动窗口”
- 周报以当前用户 `current_pet_id` 对应宠物为唯一主轴
- 快捷记录相关计数来自既有健康记录类型与萌宠日常标签，不创建新事实表

## 10.3 首页月报

`GET /home/reports/monthly`

说明：

- 响应结构与周报一致
- 统计窗口为“最近 30 天滚动窗口”

## 10.4 消息通知列表

`GET /notifications?read_status=unread&cursor=...`

## 10.5 批量已读

`PATCH /notifications/read`

## 11. 管理后台接口范围

后台不展开全部接口细节，但至少需要以下能力：

1. 用户与封禁管理
2. 社区内容审核
3. 举报处理
4. 服务商管理
5. 医院/寄养/洗护/训练目录管理
6. 商品、SKU、库存管理
7. 订单与预约运营处理
8. 设备厂商配置和设备解绑处理
9. 消息模板配置

当前已落地的后台举报处理接口：

- `GET /api/v1/admin/moderation/reports?status=pending|processed|rejected|all`
- `PATCH /api/v1/admin/moderation/reports/{report_id}`

`PATCH /api/v1/admin/moderation/reports/{report_id}` 请求关键字段：

```json
{
  "action": "confirm_violation"
}
```

补充：

- `action=confirm_violation`：举报记为 `processed`，目标帖子审核状态改为 `rejected`
- `action=dismiss_report`：举报记为 `rejected`，目标帖子保持当前状态
- 当前阶段处理人通过请求头 `X-Admin-Operator` 回写，用于后台最小可用审计标识

## 12. 领域事件设计

## 12.1 事件总原则

- 事件由业务事实表提交成功后写入 `outbox_events`
- worker 异步投递给本地消费者
- 事件 payload 只放必要字段，不塞完整大对象

## 12.2 核心事件清单

### 12.2.1 `pet.health_record.created`

生产者：

- health 模块

消费者：

- timeline
- reminder
- notification
- analytics

### 12.2.2 `pet.reminder.completed`

生产者：

- reminder 模块

消费者：

- timeline
- notification
- analytics

### 12.2.3 `pet.daily_log.created`

消费者：

- timeline
- community（条件同步）
- analytics

### 12.2.4 `community.post.published`

消费者：

- feed cache invalidation
- notification
- analytics

### 12.2.5 `service.appointment.created`

消费者：

- timeline
- notification
- ops

### 12.2.6 `commerce.order.created`

消费者：

- notification
- analytics

### 12.2.7 `device.event.ingested`

消费者：

- timeline
- notification
- alert rule engine
- device snapshot updater

### 12.2.8 `family.member.invited`

消费者：

- notification

## 13. 关键错误码

| 错误码 | 场景 |
|---|---|
| `AUTH_INVALID` | token 无效 |
| `AUTH_EXPIRED` | token 过期 |
| `SMS_CODE_INVALID` | 验证码错误 |
| `PET_NOT_FOUND` | 宠物不存在 |
| `PET_PERMISSION_DENIED` | 无宠物访问权限 |
| `REMINDER_ALREADY_PROCESSED` | 提醒已处理 |
| `CONTENT_UNDER_REVIEW` | 内容审核中 |
| `CONTENT_REJECTED` | 内容未通过审核 |
| `PROVIDER_NOT_AVAILABLE` | 服务商不可用 |
| `APPOINTMENT_SLOT_INVALID` | 预约时段失效 |
| `SKU_STOCK_NOT_ENOUGH` | 库存不足 |
| `ORDER_PRICE_CHANGED` | 订单价格变化需重试 |
| `DEVICE_ALREADY_BOUND` | 设备已被绑定 |
| `DEVICE_SIGNATURE_INVALID` | 设备回调签名错误 |
| `FAMILY_ROLE_FORBIDDEN` | 家庭角色无权操作 |

## 14. 需要特别注意的接口原则

1. `preview` 和 `submit` 分离，尤其是订单和预约。
2. 文件上传走对象存储直传，业务接口只接收 `asset_id`。
3. 所有公开内容都必须经过审核态，不允许客户端直写“已发布”。
4. 所有状态迁移由服务端控制，前端只提交动作，不直接提交目标状态。
