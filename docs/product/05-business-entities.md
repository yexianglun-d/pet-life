# 业务实体关系与字段表

## 文档定位

该文档用于定义产品层的 `业务实体模型`、核心字段语义和对象关系，用来帮助产品、设计、前后端先对系统中的关键对象达成一致。

它不是数据库 DDL 文档。

如果进入数据库实现阶段，以 `docs/technical/03-ddl-draft.sql` 为准。

## 1. 建模目标

本方案围绕 `宠物` 作为核心实体建模，保证以下链路可以自然闭环：

1. 用户创建宠物档案
2. 记录健康事件与日常内容
3. 形成成长时间轴
4. 发布社区内容
5. 发起服务预约和商品购买
6. 绑定智能设备并同步事件
7. 通过家庭共养完成多人协同

## 2. 建模原则

### 2.1 宠物是主实体

健康记录、日常记录、服务预约、设备绑定、社区挂载都优先和 `pet_id` 关联。

### 2.2 时间轴是聚合层

成长时间轴不应直接作为唯一事实来源，而应作为由各来源事件同步生成的聚合读模型。

### 2.3 服务统一抽象

医院、寄养、洗护、训练统一抽象为 `service_provider`，通过 `provider_type` 区分。

### 2.4 商品和服务分离

商品交易链路与服务预约链路独立建模，避免订单和预约混在一张表中。

### 2.5 公共字段约定

除中间表外，所有业务表默认包含以下字段，后文不再重复：

- `id`
- `created_at`
- `updated_at`
- `deleted_at`

## 3. 核心实体关系

```text
users
├─< family_members >─ families
│                     └─< pets
│                         ├─< pet_health_records
│                         ├─< pet_reminders
│                         ├─< pet_daily_logs
│                         ├─< pet_timeline_events
│                         ├─< service_appointments
│                         ├─< device_bindings >─ devices
│                         │                       └─< device_events
│                         └─< community_posts
│
├─< community_posts
│   └─< community_comments
│
├─< cart_items >─ product_skus >─ products
│
└─< orders
    └─< order_items >─ product_skus

service_providers
└─< service_appointments

community_topics
└─< community_posts

notifications
```

## 4. 核心实体字段设计

## 4.1 用户与家庭域

### 4.1.1 `users`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| mobile | varchar(20) | 是 | 手机号，支持验证码登录 |
| nickname | varchar(50) | 是 | 用户昵称 |
| avatar_url | varchar(255) | 否 | 头像地址 |
| city_code | varchar(32) | 否 | 当前城市编码 |
| city_name | varchar(50) | 否 | 当前城市名称 |
| status | tinyint | 是 | 账号状态：正常、禁用、注销中 |
| last_login_at | datetime | 否 | 最近登录时间 |

### 4.1.2 `families`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| family_name | varchar(100) | 是 | 家庭名称，可默认由首位创建者生成 |
| owner_user_id | bigint | 是 | 家庭拥有者用户 ID |
| status | tinyint | 是 | 家庭状态 |

### 4.1.3 `family_members`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| family_id | bigint | 是 | 关联家庭 |
| user_id | bigint | 是 | 家庭成员 |
| role | varchar(20) | 是 | `owner` / `admin` / `member` |
| invite_status | varchar(20) | 是 | 邀请中、已加入、已拒绝 |
| joined_at | datetime | 否 | 加入时间 |

## 4.2 宠物主数据域

### 4.2.1 `pets`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| family_id | bigint | 否 | 所属家庭，可为空表示个人宠物 |
| owner_user_id | bigint | 是 | 主要拥有者 |
| pet_name | varchar(50) | 是 | 宠物名称 |
| pet_type | varchar(20) | 是 | 猫、狗等 |
| breed | varchar(50) | 否 | 品种 |
| gender | varchar(10) | 否 | 性别 |
| birthday | date | 否 | 出生日期 |
| adopt_date | date | 否 | 到家日期 |
| neuter_status | tinyint | 否 | 绝育状态 |
| avatar_url | varchar(255) | 否 | 头像 |
| weight_kg | decimal(5,2) | 否 | 当前体重 |
| allergy_notes | varchar(500) | 否 | 过敏信息 |
| medical_history | text | 否 | 重要病史 |
| status | tinyint | 是 | 正常、离世、送养等状态 |

### 4.2.2 `pet_reminders`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| pet_id | bigint | 是 | 关联宠物 |
| reminder_type | varchar(30) | 是 | 疫苗、驱虫、体检、复查、自定义 |
| title | varchar(100) | 是 | 提醒标题 |
| reminder_mode | varchar(20) | 是 | 单次、周期 |
| cycle_value | int | 否 | 周期间隔值 |
| cycle_unit | varchar(20) | 否 | 天、周、月 |
| remind_at | datetime | 是 | 下次提醒时间 |
| status | varchar(20) | 是 | 待处理、已完成、已跳过 |
| source_record_id | bigint | 否 | 来源记录 ID |
| handler_user_id | bigint | 否 | 最近处理人 |

## 4.3 健康与日常域

### 4.3.1 `pet_health_records`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| pet_id | bigint | 是 | 关联宠物 |
| record_type | varchar(30) | 是 | 疫苗、驱虫、体检、用药、异常观察 |
| title | varchar(100) | 是 | 记录标题 |
| occurred_at | datetime | 是 | 发生时间 |
| operator_user_id | bigint | 否 | 记录操作者 |
| hospital_name | varchar(100) | 否 | 医院名称 |
| doctor_name | varchar(50) | 否 | 医生名称 |
| severity_level | varchar(20) | 否 | 异常严重程度 |
| result_summary | varchar(500) | 否 | 结果摘要 |
| attachments | json | 否 | 图片、报告单等附件 |
| notes | text | 否 | 备注 |

### 4.3.2 `pet_daily_logs`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| pet_id | bigint | 是 | 关联宠物 |
| author_user_id | bigint | 是 | 记录人 |
| media_list | json | 否 | 图片或视频列表 |
| title | varchar(100) | 否 | 日常标题 |
| content | text | 否 | 日常描述 |
| scene_tags | json | 否 | 场景标签，如散步、生日 |
| mood_tags | json | 否 | 状态标签，如开心、活跃 |
| visibility | varchar(20) | 是 | 私密、家庭可见、公开 |
| sync_to_community | tinyint | 是 | 是否同步社区 |
| sync_to_timeline | tinyint | 是 | 是否加入成长时间轴 |
| happened_at | datetime | 是 | 记录发生时间 |

### 4.3.3 `pet_timeline_events`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| pet_id | bigint | 是 | 关联宠物 |
| event_type | varchar(30) | 是 | 健康、日常、服务、设备、纪念日 |
| source_type | varchar(30) | 是 | 来源表类型 |
| source_id | bigint | 是 | 来源记录 ID |
| event_time | datetime | 是 | 事件时间 |
| title | varchar(100) | 是 | 事件标题 |
| summary | varchar(500) | 否 | 摘要 |
| cover_url | varchar(255) | 否 | 封面图 |
| visibility | varchar(20) | 是 | 私密、家庭、公开 |

## 4.4 社区域

### 4.4.1 `community_topics`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| topic_name | varchar(100) | 是 | 话题名称 |
| topic_desc | varchar(255) | 否 | 话题描述 |
| city_code | varchar(32) | 否 | 同城话题可绑定城市 |
| status | tinyint | 是 | 启用、停用 |

### 4.4.2 `community_posts`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| user_id | bigint | 是 | 发布用户 |
| pet_id | bigint | 否 | 关联宠物 |
| topic_id | bigint | 否 | 关联话题 |
| post_type | varchar(20) | 是 | 图文、视频、问答、经验 |
| title | varchar(100) | 否 | 帖子标题 |
| content | text | 否 | 正文 |
| media_list | json | 否 | 图片/视频资源 |
| source_daily_log_id | bigint | 否 | 来源萌宠日常 ID |
| source_service_id | bigint | 否 | 来源服务体验 ID |
| source_product_id | bigint | 否 | 来源商品体验 ID |
| city_code | varchar(32) | 否 | 同城分发城市 |
| visibility | varchar(20) | 是 | 公开、仅粉丝、草稿 |
| review_status | varchar(20) | 是 | 待审、通过、拒绝 |
| published_at | datetime | 否 | 发布时间 |

### 4.4.3 `community_comments`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| post_id | bigint | 是 | 关联帖子 |
| user_id | bigint | 是 | 评论用户 |
| parent_comment_id | bigint | 否 | 父评论，支持回复 |
| content | varchar(1000) | 是 | 评论内容 |
| status | varchar(20) | 是 | 正常、删除、屏蔽 |

## 4.5 服务域

### 4.5.1 `service_providers`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| provider_type | varchar(20) | 是 | hospital、boarding、grooming、training |
| provider_name | varchar(100) | 是 | 服务商名称 |
| city_code | varchar(32) | 是 | 城市编码 |
| address | varchar(255) | 否 | 地址 |
| latitude | decimal(10,6) | 否 | 纬度 |
| longitude | decimal(10,6) | 否 | 经度 |
| contact_phone | varchar(20) | 否 | 联系电话 |
| business_hours | varchar(255) | 否 | 营业时间 |
| rating_avg | decimal(3,2) | 否 | 平均评分 |
| review_count | int | 否 | 评价数 |
| status | varchar(20) | 是 | 营业中、休息中、下线 |
| ext_json | json | 否 | 扩展字段，如医院科室、洗护价格带 |

### 4.5.2 `service_appointments`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| user_id | bigint | 是 | 下单用户 |
| pet_id | bigint | 是 | 关联宠物 |
| provider_id | bigint | 是 | 服务商 |
| appointment_type | varchar(20) | 是 | 医院、寄养、洗护、训练 |
| appointment_date | date | 是 | 预约日期 |
| appointment_slot | varchar(50) | 否 | 时间段 |
| demand_desc | varchar(500) | 否 | 需求描述 |
| contact_name | varchar(50) | 是 | 联系人 |
| contact_mobile | varchar(20) | 是 | 联系电话 |
| status | varchar(20) | 是 | 待确认、已确认、已取消、已完成 |
| remark | varchar(500) | 否 | 备注 |

## 4.6 商城域

### 4.6.1 `products`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| product_name | varchar(150) | 是 | 商品名称 |
| category_code | varchar(32) | 是 | 分类编码 |
| pet_type | varchar(20) | 否 | 适用品类 |
| age_stage | varchar(20) | 否 | 适用年龄阶段 |
| brand_name | varchar(50) | 否 | 品牌 |
| main_image | varchar(255) | 否 | 主图 |
| detail_images | json | 否 | 详情图 |
| status | varchar(20) | 是 | 上架、下架 |
| description | text | 否 | 商品详情 |

### 4.6.2 `product_skus`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| product_id | bigint | 是 | 关联商品 |
| sku_code | varchar(64) | 是 | SKU 编码 |
| sku_name | varchar(100) | 是 | SKU 名称 |
| sale_price | decimal(10,2) | 是 | 销售价 |
| market_price | decimal(10,2) | 否 | 划线价 |
| stock_qty | int | 是 | 库存 |
| weight_g | int | 否 | 重量，便于物流测算 |
| status | varchar(20) | 是 | 有效、无效 |

### 4.6.3 `cart_items`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| user_id | bigint | 是 | 用户 |
| pet_id | bigint | 否 | 关联宠物，用于推荐与复购 |
| sku_id | bigint | 是 | 商品 SKU |
| quantity | int | 是 | 数量 |
| checked | tinyint | 是 | 是否勾选结算 |

### 4.6.4 `orders`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| user_id | bigint | 是 | 下单用户 |
| pet_id | bigint | 否 | 关联宠物 |
| order_no | varchar(64) | 是 | 订单号 |
| order_status | varchar(20) | 是 | 待支付、已支付、已发货、已完成、已取消 |
| total_amount | decimal(10,2) | 是 | 订单总金额 |
| pay_amount | decimal(10,2) | 是 | 实付金额 |
| receiver_name | varchar(50) | 是 | 收货人 |
| receiver_mobile | varchar(20) | 是 | 联系方式 |
| receiver_address | varchar(255) | 是 | 收货地址 |
| pay_at | datetime | 否 | 支付时间 |

### 4.6.5 `order_items`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| order_id | bigint | 是 | 关联订单 |
| product_id | bigint | 是 | 商品 |
| sku_id | bigint | 是 | SKU |
| product_name | varchar(150) | 是 | 下单时商品快照 |
| sku_name | varchar(100) | 是 | 下单时 SKU 快照 |
| sale_price | decimal(10,2) | 是 | 下单时成交价 |
| quantity | int | 是 | 数量 |
| main_image | varchar(255) | 否 | 下单时主图快照 |

## 4.7 设备域

### 4.7.1 `devices`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| device_sn | varchar(64) | 是 | 设备序列号 |
| device_type | varchar(30) | 是 | 喂食器、饮水机、猫砂盆、摄像头等 |
| brand_name | varchar(50) | 否 | 品牌 |
| model_name | varchar(50) | 否 | 型号 |
| firmware_version | varchar(50) | 否 | 固件版本 |
| online_status | varchar(20) | 是 | 在线、离线、异常 |
| last_online_at | datetime | 否 | 最近在线时间 |

### 4.7.2 `device_bindings`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| device_id | bigint | 是 | 设备 |
| user_id | bigint | 是 | 绑定用户 |
| pet_id | bigint | 是 | 关联宠物 |
| bind_name | varchar(50) | 否 | 用户自定义设备名 |
| room_name | varchar(50) | 否 | 所在房间 |
| bind_status | varchar(20) | 是 | 已绑定、解绑、待激活 |
| bound_at | datetime | 否 | 绑定时间 |

### 4.7.3 `device_events`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| device_id | bigint | 是 | 设备 |
| pet_id | bigint | 是 | 关联宠物 |
| event_code | varchar(50) | 是 | 事件编码 |
| event_type | varchar(30) | 是 | 投喂、饮水、清理、离线、异常 |
| event_time | datetime | 是 | 事件时间 |
| event_value | varchar(100) | 否 | 事件值，如毫升数、克数 |
| severity_level | varchar(20) | 否 | 普通、提醒、告警 |
| raw_payload | json | 否 | 原始设备上报内容 |

## 4.8 通知域

### 4.8.1 `notifications`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| user_id | bigint | 是 | 接收用户 |
| notify_type | varchar(30) | 是 | 系统、互动、预约、订单、设备、提醒 |
| biz_type | varchar(30) | 否 | 业务类型 |
| biz_id | bigint | 否 | 业务主键 ID |
| title | varchar(100) | 是 | 通知标题 |
| content | varchar(500) | 是 | 通知内容 |
| read_status | tinyint | 是 | 未读、已读 |
| sent_at | datetime | 是 | 发送时间 |

## 5. 关键索引建议

1. `pets(owner_user_id, status)`
2. `pet_reminders(pet_id, status, remind_at)`
3. `pet_health_records(pet_id, record_type, occurred_at desc)`
4. `pet_daily_logs(pet_id, happened_at desc)`
5. `pet_timeline_events(pet_id, event_time desc)`
6. `community_posts(city_code, review_status, published_at desc)`
7. `community_comments(post_id, created_at asc)`
8. `service_providers(provider_type, city_code, status)`
9. `service_appointments(user_id, status, appointment_date desc)`
10. `orders(user_id, order_status, created_at desc)`
11. `device_bindings(pet_id, bind_status)`
12. `device_events(device_id, event_time desc)`
13. `notifications(user_id, read_status, sent_at desc)`

## 6. 关键状态枚举建议

### 6.1 宠物状态

- `active`
- `memorial`
- `rehomed`

### 6.2 提醒状态

- `pending`
- `done`
- `skipped`

### 6.3 日常可见性

- `private`
- `family`
- `public`

### 6.4 帖子审核状态

- `pending_review`
- `approved`
- `rejected`

### 6.5 预约状态

- `pending_confirm`
- `confirmed`
- `canceled`
- `completed`

### 6.6 订单状态

- `pending_pay`
- `paid`
- `shipped`
- `completed`
- `canceled`

### 6.7 设备在线状态

- `online`
- `offline`
- `alert`

## 7. 实现建议

### 7.1 时间轴采用异步写入

健康记录、萌宠日常、服务预约、设备事件写入后，通过异步任务同步生成 `pet_timeline_events`，避免前台写请求过重。

### 7.2 社区内容与宠物档案解耦

社区帖子即使删除，也不应删除宠物主档案和萌宠日常原记录。

### 7.3 订单与预约不要合表

商城是交易流，服务是预约流，两者生命周期和状态机不同，必须拆表。

### 7.4 设备原始数据保留

`device_events.raw_payload` 建议保留原始设备上报数据，便于后续规则升级和异常排查。
