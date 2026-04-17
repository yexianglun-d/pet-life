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

## 2.4 获取当前用户信息

`GET /me`

返回：

- 用户基础资料
- 当前城市
- 当前宠物
- 家庭摘要

## 2.5 修改当前宠物

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

## 3.3 获取宠物详情

`GET /pets/{pet_id}`

## 3.4 更新宠物

`PATCH /pets/{pet_id}`

## 3.5 获取宠物主页摘要

`GET /pets/{pet_id}/summary`

返回聚合：

- 基础档案
- 今日待办数
- 最近健康记录
- 最近日常
- 最近时间轴
- 设备状态摘要

## 3.6 获取家庭信息

`GET /family`

## 3.7 创建或初始化家庭

`POST /family`

## 3.8 发起家庭邀请

`POST /family/invitations`

请求：

```json
{
  "invitee_mobile": "13800000000",
  "role": "member",
  "shared_pet_ids": ["10001", "10002"]
}
```

## 3.9 修改成员角色

`PATCH /family/members/{member_id}/role`

## 3.10 移除家庭成员

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

## 4.3 获取提醒列表

`GET /pets/{pet_id}/reminders?status=pending`

## 4.4 创建提醒

`POST /pets/{pet_id}/reminders`

## 4.5 完成提醒

`PATCH /reminders/{reminder_id}/complete`

请求：

```json
{
  "handled_at": "2026-04-20T10:30:00Z",
  "generate_next": true
}
```

## 4.6 跳过提醒

`PATCH /reminders/{reminder_id}/skip`

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

`GET /pets/{pet_id}/daily-logs?visibility=all&cursor=...`

## 5.4 创建萌宠日常

`POST /pets/{pet_id}/daily-logs`

请求关键字段：

```json
{
  "asset_ids": ["90001", "90002"],
  "title": "今天第一次散步",
  "content": "出门有点紧张，但表现很好",
  "scene_tags": ["walk"],
  "mood_tags": ["excited"],
  "visibility": "family",
  "sync_to_timeline": true,
  "sync_to_community": false,
  "happened_at": "2026-04-20T09:00:00Z"
}
```

## 5.5 获取日常详情

`GET /daily-logs/{daily_log_id}`

## 5.6 更新日常

`PATCH /daily-logs/{daily_log_id}`

## 5.7 一键发布到社区

`POST /daily-logs/{daily_log_id}/publish`

## 5.8 获取成长时间轴

`GET /pets/{pet_id}/timeline?event_type=all&cursor=...`

## 6. 社区接口

## 6.1 获取社区流

`GET /community/feed?tab=recommended&city_code=310000&cursor=...`

返回卡片字段：

- `post_id`
- `author`
- `pet_card`
- `title`
- `media_cover`
- `interaction_summary`
- `review_status`

## 6.2 创建社区帖子

`POST /community/posts`

请求关键字段：

```json
{
  "post_type": "image_text",
  "pet_id": "10001",
  "topic_id": "30001",
  "title": "第一次洗澡记录",
  "content": "虽然怕水，但很配合",
  "asset_ids": ["90011"],
  "source_daily_log_id": "50001",
  "city_code": "310000",
  "visibility": "public"
}
```

## 6.3 获取帖子详情

`GET /community/posts/{post_id}`

## 6.4 评论帖子

`POST /community/posts/{post_id}/comments`

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

## 10.2 消息通知列表

`GET /notifications?read_status=unread&cursor=...`

## 10.3 批量已读

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
