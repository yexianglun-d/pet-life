# API 契约与领域事件设计

## 0. 当前实施范围说明

1. 当前实现用户端 App 所需接口，并同步建设后台必需接口。
2. 商城接口与设备接口保留为预留契约，不进入当前开发批次。
3. 后台 Web 仅覆盖当前必需能力：举报处理、审核处理、健康/日常/时间轴/提醒查询、服务商数据维护和基础配置。

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

## 2.7 获取当前用户设置

`GET /me/settings`

返回关键字段：

- `user_id`
- `mobile`
- `nickname`
- `city_code`
- `city_name`
- `notification_enabled`
- `privacy_level`

说明：

- 该接口提供“个人中心设置页”所需的稳定读模型
- 当前 `privacy_level` 仅开放 `normal` 与 `private`

## 2.8 修改当前用户资料

`PATCH /me/profile`

请求：

```json
{
  "nickname": "奶油"
}
```

说明：

- 当前批次仅开放昵称编辑，不提前暴露未完成的头像上传链路
- 服务端会在入库前做去空白、长度与空值校验

## 2.9 修改当前城市

`PATCH /me/settings/city`

请求：

```json
{
  "city_code": "310000",
  "city_name": "上海"
}
```

说明：

- 当前城市作为个人中心与后续服务中心的统一上下文
- 服务端要求 `city_code` 与 `city_name` 同时提交，避免脏城市数据

## 2.10 修改通知与隐私设置

`PATCH /me/settings/notifications`

请求：

```json
{
  "notification_enabled": true,
  "privacy_level": "normal"
}
```

说明：

- `notification_enabled=false` 表示用户主动关闭站内提醒偏好
- 当前批次先沉淀用户真实偏好，后续通知模块与系统推送接入时直接复用
- `privacy_level=normal` 表示正常展示主页，`privacy_level=private` 表示更严格的个人主页可见范围

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

响应记录关键字段：

- `health_record_id`
- `record_type`
- `title`
- `value`
- `unit`
- `hospital_name`
- `doctor_name`
- `severity_level`
- `result_summary`
- `attachment_asset_ids`
- `attachment_assets`
- `next_reminder_id`
- `next_reminder_at`
- `next_reminder_status`
- `occurred_at`
- `notes`

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
  "severity_level": null,
  "result_summary": "接种完成",
  "notes": "无异常",
  "attachment_asset_ids": ["90001"],
  "next_reminder_at": "2027-04-20T10:30:00Z",
  "next_reminder_title": "下次狂犬疫苗"
}
```

说明：

- `record_type` 当前支持 `vaccine`、`deworming`、`examination`、`medication`、`weight`、`observation`
- `hospital_name`、`doctor_name`、`result_summary` 主要用于疫苗、驱虫和体检记录
- `value`、`unit` 主要用于用药剂量、体重等数值型记录
- `severity_level` 主要用于异常观察记录
- `attachment_asset_ids` 必须引用当前用户已上传完成、业务类型为 `health_report` 的媒体资产；图片与 PDF 可用于健康记录附件
- 响应会同时返回兼容字段 `attachment_asset_ids` 和用于预览的 `attachment_assets` 元数据；附件预览使用 `attachment_assets[].access_url` 或 `GET /media-assets/{asset_id}/content`
- 媒体读取权限覆盖上传者本人，以及可访问对应健康记录宠物的家庭成员
- 只有 `vaccine`、`deworming`、`examination` 支持通过 `next_reminder_at` 自动派生下一次提醒
- 自动派生提醒以 `source_record_id` 关联源健康记录；更新健康记录时会删除未处理的旧派生提醒并按最新表单重建
- 删除健康记录时会同步删除仍未处理的派生提醒

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

## 5.1 上传媒体资产

`POST /media-assets`

请求类型：`multipart/form-data`

字段：

- `biz_type`：业务类型，当前支持 `avatar`、`health_report`、`daily_log`、`community`、`service_review`
- `file`：上传文件

响应关键字段：

```json
{
  "asset_id": "90001",
  "biz_type": "daily_log",
  "media_type": "image",
  "file_name": "cat.jpg",
  "content_type": "image/jpeg",
  "file_size": 12345,
  "file_hash": "sha256",
  "upload_status": "uploaded",
  "review_status": "pending_review",
  "access_url": "/api/v1/media-assets/90001/content"
}
```

说明：

- `health_report` 支持图片与 PDF
- `daily_log`、`community` 支持图片与视频
- 业务记录只能引用当前用户已上传完成且业务类型匹配的资产
- 元数据和文件内容读取支持上传者本人，也支持通过健康记录或萌宠日常获得宠物访问权的家庭成员
- 当前默认实现为服务端本地直传
- 服务端已预留 `petlife.media.storage.provider=local|object_storage` 配置；`object_storage` 当前统一对象 key、bucket 和 CDN URL 口径，文件仍写入本地过渡目录，后续替换为云厂商上传适配器时保持 `asset_id`、`object_key`、`bucket_name`、`cdn_url` 契约不变
- 当配置 `PETLIFE_MEDIA_PUBLIC_BASE_URL` 时，`access_url` 返回外部 CDN/静态资源地址；未配置时回退为 `GET /media-assets/{asset_id}/content`

## 5.2 获取媒体资产

- `GET /media-assets/{asset_id}`：获取媒体元数据
- `GET /media-assets/{asset_id}/content`：读取媒体文件内容

## 5.3 获取萌宠日常列表

`GET /pets/{pet_id}/daily-logs`

响应记录关键字段：

- `daily_log_id`
- `content`
- `media_asset_ids`
- `media_assets`
- `tags`
- `visibility`
- `sync_to_community`
- `community_post_id`
- `happened_at`

## 5.4 创建萌宠日常

`POST /pets/{pet_id}/daily-logs`

请求关键字段：

```json
{
  "content": "今天第一次散步，出门有点紧张，但表现很好",
  "tags": ["散步", "成长"],
  "media_asset_ids": ["90001"],
  "visibility": "family",
  "sync_to_community": false,
  "happened_at": "2026-04-20T09:00:00Z"
}
```

说明：

- 当前阶段支持文字内容、标签、媒体资产 ID、可见范围和记录时间
- `media_asset_ids` 必须引用当前用户已上传完成、业务类型为 `daily_log` 的图片或视频资产
- 响应会同时返回兼容字段 `media_asset_ids` 和用于图片/视频预览的 `media_assets` 元数据
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

- 当前阶段支持 `all`、`health`、`daily_log`、`service` 四种筛选值
- 已接入健康记录、萌宠日常与服务预约三类派生事件
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

返回：

- `city_code`
- `city_name`
- `opened`
- `unavailable_reason`
- `categories`
- `featured_providers`
- `upcoming_appointments`
- `commerce_placeholder`

说明：

- `commerce_placeholder` 只承接商城当前预留说明，不进入交易后端链路
- 当前城市是否开通以后台 `service_city_configs` 配置为准，不再由服务商数量隐式推断
- 未开通城市返回 `opened=false` 与后台配置的 `unavailable_reason`

## 7.2 获取服务商列表

`GET /providers?provider_type=hospital&city_code=310000&sort=distance`

说明：

- `provider_type` 支持 `hospital`、`boarding`、`grooming`、`training`
- 响应会返回服务项目和近期可预约时段摘要
- 城市未开通时返回空列表，不展示后台已维护但尚未开放的服务商

## 7.3 获取服务商详情

`GET /providers/{provider_id}`

## 7.3.1 获取服务商可预约时段

`GET /providers/{provider_id}/slots?appointment_type=hospital&start_date=2026-04-28&end_date=2026-05-11`

说明：

- 时段名额由服务端读取 `provider_schedule_slots`
- `available_quota=quota-booked_count`
- 只有 `status=open` 且剩余名额大于 0 时 `bookable=true`

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

说明：

- 创建预约会校验当前用户是否有该宠物访问权
- 创建预约会校验服务商所在城市是否已开通
- 创建预约会校验服务商状态、预约类型和时段名额
- 同一时段通过数据库行锁和名额递增防止超卖
- 预约创建后会生成站内预约通知，并派生宠物时间轴 `service` 事件

## 7.5 获取预约记录

`GET /appointments?status=pending_confirm`

说明：

- `status` 支持 `all`、`pending_confirm`、`confirmed`、`completed`、`canceled`

## 7.6 取消预约

`PATCH /appointments/{appointment_id}/cancel`

请求：

```json
{
  "cancel_reason": "临时改期"
}
```

说明：

- 仅 `pending_confirm` 和 `confirmed` 状态允许取消
- 取消后会回补原时段名额，并更新时间轴事件为取消态

## 7.7 获取服务商评价

`GET /providers/{provider_id}/reviews`

说明：

- 仅返回 `visible` 状态且未删除的评价
- 用户端服务商详情页通过该接口展示真实评价列表

## 7.8 创建预约评价

`POST /appointments/{appointment_id}/review`

请求：

```json
{
  "rating": 5,
  "content": "医生解释很清楚，整体等待时间也可控。"
}
```

说明：

- 仅预约所属用户可以评价该预约
- 仅 `completed` 状态预约允许评价
- 同一预约只允许评价一次
- 评价创建后会刷新服务商 `rating_avg` 与 `review_count`

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

`GET /notifications?notify_type=all|system|reminder&read_status=all|unread|read`

返回关键字段：

- `items`
- `unread_count`
- `system_unread_count`
- `reminder_unread_count`

`items` 单项关键字段：

- `notification_id`
- `notify_type`
- `biz_type`
- `biz_id`
- `title`
- `content`
- `read`
- `sent_at`
- `read_at`

当前已接入的通知来源：

- 首次登录欢迎消息：`notify_type=system`，`biz_type=user_welcome`
- 提醒完成/跳过：`notify_type=reminder`，`biz_type=reminder_completed|reminder_skipped`
- 审核结果：`notify_type=system`，`biz_type=moderation_report`

说明：

- 通知生成尊重用户 `notification_switch`
- 提醒通知会按宠物访问范围投递给有权限的家庭成员
- 当前未接入系统推送通道，接口表示站内消息中心

## 10.5 单条已读

`PATCH /notifications/{notification_id}/read`

## 10.6 批量已读

`PATCH /notifications/read`

请求：

```json
{
  "notify_type": "all"
}
```

说明：

- `notify_type=all` 表示全部已读
- 也可传 `system` 或 `reminder` 按类型批量已读

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

当前已落地的后台用户查询接口：

- `GET /api/v1/admin/users?keyword=Momo&mobile=13800000000&nickname=Momo&city_code=330100&notification_enabled=true&privacy_level=normal`
- `GET /api/v1/admin/users/{user_id}`

响应约定：

- 返回用户基础资料、城市、账号状态、最近登录时间和创建时间。
- `settings` 返回 `current_pet_id`、`notification_enabled`、`privacy_level`。
- `primary_family` 返回用户主要家庭上下文；用户暂无可用家庭时返回 `null`。
- `current_pet` 复用后台宠物上下文结构，包含宠物、家庭和主人信息；用户暂无当前宠物时返回 `null`。
- 列表接口只读查询真实已存在数据，不触发用户初始化或写入默认家庭/宠物。

当前已落地的后台家庭查询接口：

- `GET /api/v1/admin/families?keyword=Momo&family_name=Momo&member_mobile=13800000000&member_role=owner&status=1`
- `GET /api/v1/admin/families/{family_id}`

响应约定：

- 返回家庭基础资料、拥有者、状态、成员数、宠物数、成员关系和家庭宠物列表。
- `members` 返回成员关系 ID、用户 ID、昵称、手机号、角色、加入状态和加入时间。
- `pets` 返回家庭下未软删宠物的 ID、名称、类型、品种、状态和主人信息。
- 该接口是后台只读治理能力，不复用用户端当前家庭权限视角，也不触发家庭初始化。

当前已落地的后台宠物主档查询接口：

- `GET /api/v1/admin/pets?keyword=Momo&pet_name=Momo&pet_type=cat&status=active&owner_mobile=13800000000&family_id=30001`
- `GET /api/v1/admin/pets/{pet_id}`

响应约定：

- `pet` 返回宠物主档详情，与用户端宠物详情字段保持一致。
- `owner` 返回主人用户上下文。
- `family` 返回家庭 ID、名称、状态和成员数；宠物暂无家庭时返回 `null`。
- 该接口查询未软删宠物，包含 `active`、`memorial`、`rehomed` 状态，用于后台归属核查；不提供问题数据修复写操作。

当前已落地的后台举报处理接口：

- `GET /api/v1/admin/moderation/reports?status=pending|processed|rejected|all`
- `PATCH /api/v1/admin/moderation/reports/{report_id}`
- `GET /api/v1/admin/moderation/audit-logs?operator_id=risk-admin&target_type=moderation_report&action=moderation_report_confirm_violation`

`PATCH /api/v1/admin/moderation/reports/{report_id}` 请求关键字段：

```json
{
  "action": "confirm_violation",
  "admin_notes": "确认为违规售卖内容"
}
```

补充：

- `action=confirm_violation`：举报记为 `processed`，目标帖子审核状态改为 `rejected`
- `action=dismiss_report`：举报记为 `rejected`，目标帖子保持当前状态
- `admin_notes` 会入库并随举报列表、处理响应回显
- 当前阶段处理人通过请求头 `X-Admin-Operator` 回写，用于后台操作审计标识
- 处理动作会写入 `audit_logs`，目标类型为 `moderation_report`，动作值为 `moderation_report_confirm_violation` 或 `moderation_report_dismiss_report`

当前已落地的后台内容查询接口：

- `GET /api/v1/admin/health-records?record_type=examination&pet_id=10001&operator_user_id=20001&keyword=体重`
- `GET /api/v1/admin/health-records/{health_record_id}`
- `GET /api/v1/admin/daily-logs?visibility=public&sync_to_community=false&pet_id=10001&author_user_id=20001&keyword=日常`
- `GET /api/v1/admin/daily-logs/{daily_log_id}`
- `GET /api/v1/admin/timeline/events?event_type=health&source_type=health_record&pet_id=10001&source_id=70001`
- `GET /api/v1/admin/timeline/events/{event_id}`

响应约定：

- 健康记录返回 `health_record`、`pet`、`operator`；`health_record.attachment_assets` 可直接用于后台预览附件。
- 萌宠日常返回 `daily_log`、`pet`、`author`；`daily_log.media_assets` 可直接用于后台预览图片或视频。
- 时间轴事件返回 `timeline_event`、`pet`、`source_status`，其中 `source_status` 支持 `active`、`deleted`、`missing`、`unsupported`，用于排查派生事件与源记录是否一致。
- 当前内容查询接口为后台只读治理能力，不引入删除、恢复或修复写操作；后续如要做数据修复，需先定义问题类型、状态机与审计动作。

当前已落地的后台提醒查询接口：

- `GET /api/v1/admin/reminders?keyword=驱虫&status=completed&reminder_type=deworming&reminder_mode=single&pet_id=10001&family_id=30001&owner_user_id=20001&handler_user_id=20001&source_record_id=70001&due_from=2026-05-01T00:00:00+08:00&due_to=2026-05-31T23:59:59+08:00`
- `GET /api/v1/admin/reminders/{reminder_id}`

响应约定：

- `reminder` 返回提醒主体，接口层统一使用 `completed` 表示数据库 `done` 状态。
- `pet` 返回宠物、家庭和主人上下文，用于后台按归属排查。
- `handler` 返回提醒完成或跳过时的处理人；未处理提醒返回 `null`。
- `source_record` 返回健康记录派生提醒的来源记录上下文；手工创建提醒返回 `null`。

说明：

- 该接口为后台只读查询能力，不新增提醒模板写能力。
- 后台查询跨家庭读取未软删提醒和未软删宠物，不复用用户端宠物访问权限视角。

当前已落地的后台服务中心接口：

- `GET /api/v1/admin/service/cities?city_code=310000&opened=true`
- `POST /api/v1/admin/service/cities`
- `GET /api/v1/admin/service/providers?provider_type=hospital&city_code=310000&status=online`
- `POST /api/v1/admin/service/providers`
- `PATCH /api/v1/admin/service/providers/{provider_id}`
- `POST /api/v1/admin/service/providers/{provider_id}/items`
- `PATCH /api/v1/admin/service/providers/{provider_id}/items/{service_item_id}`
- `POST /api/v1/admin/service/providers/{provider_id}/slots`
- `PATCH /api/v1/admin/service/providers/{provider_id}/slots/{slot_id}`
- `GET /api/v1/admin/service/appointments?status=pending_confirm&provider_type=hospital&city_code=310000`
- `PATCH /api/v1/admin/service/appointments/{appointment_id}/status`
- `GET /api/v1/admin/service/reviews?status=visible&provider_type=hospital&city_code=310000`
- `PATCH /api/v1/admin/service/reviews/{review_id}/status`
- `GET /api/v1/admin/service/audit-logs?operator_id=service-admin&target_type=service_provider&action=service_provider_update`

`POST /api/v1/admin/service/cities` 请求关键字段：

```json
{
  "city_code": "310000",
  "city_name": "上海",
  "opened": true,
  "unavailable_reason": null,
  "sort_order": 0
}
```

`POST /api/v1/admin/service/providers` 与 `PATCH /api/v1/admin/service/providers/{provider_id}` 请求关键字段：

```json
{
  "provider_type": "hospital",
  "provider_name": "安心宠物医院",
  "city_code": "310000",
  "address": "上海市徐汇区宠物友好路 88 号",
  "latitude": 31.218,
  "longitude": 121.402,
  "contact_phone": "021-12345678",
  "business_hours": "09:00-20:00",
  "rating_avg": 4.8,
  "review_count": 16,
  "status": "online"
}
```

服务项目请求关键字段：

```json
{
  "service_code": "hospital_basic",
  "service_name": "基础问诊",
  "service_desc": "面向日常照护的基础问诊服务",
  "price_min": 99,
  "price_max": 199,
  "status": "active"
}
```

预约时段请求关键字段：

```json
{
  "appointment_type": "hospital",
  "slot_date": "2026-05-02",
  "start_time": "10:00:00",
  "end_time": "11:00:00",
  "quota": 2,
  "status": "open"
}
```

预约状态更新请求关键字段：

```json
{
  "status": "confirmed",
  "remark": "后台已确认服务商和预约时段"
}
```

评价状态更新请求关键字段：

```json
{
  "status": "hidden"
}
```

服务中心审计日志响应关键字段：

```json
{
  "audit_log_id": "90001",
  "operator_type": "admin",
  "operator_id": "service-admin",
  "target_type": "service_provider",
  "target_id": "70001",
  "action": "service_provider_update",
  "detail_json": "{\"status\":\"online\"}",
  "ip_address": "127.0.0.1",
  "user_agent": "Mozilla/5.0",
  "created_at": "2026-04-28T11:20:00Z"
}
```

补充：

- `provider_type` 和 `appointment_type` 当前支持 `hospital`、`boarding`、`grooming`、`training`。
- 城市开通状态由 `service_city_configs.opened` 控制；关闭城市不会删除服务商、项目、时段和历史预约。
- 服务商状态支持 `online`、`rest`、`offline`；只有用户端可预约链路会把 `online` 视为可创建预约。
- 服务项目状态支持 `active`、`inactive`。
- 时段状态支持 `open`、`closed`、`full`；后台降低时段名额时，如果已预约数不小于新名额，开放状态会自动归一为 `full`。
- 后台将预约从 `pending_confirm` 或 `confirmed` 调整为 `canceled` 时，会释放对应时段名额并同步服务预约时间轴事件。
- 已取消预约不能从后台恢复为非取消状态，需重新创建预约，避免库存和用户操作链路被反向篡改。
- 评价状态支持 `visible`、`hidden`；后台隐藏或恢复评价后会重新计算服务商评分均值与评价数。
- 城市、服务商、服务项目、预约时段、预约状态和评价状态的后台写操作都会写入 `audit_logs`。

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
