# PetLife API 文档中心

## 📖 文档概览

本目录包含 PetLife 宠物生活管家项目的完整 API 文档和 Postman 集合配置。

## 📁 文件说明

### 核心文档

| 文件 | 说明 |
|------|------|
| **petlife-openapi.yaml** | OpenAPI 3.0 规范文件，包含所有 API 接口的完整定义 |
| **QUICK_START.md** | 5 分钟快速开始指南 |
| **POSTMAN_SETUP.md** | Postman 详细配置和使用指南 |
| **README.md** | 本文件，文档导航 |

### 项目根目录文件

| 文件 | 说明 |
|------|------|
| **.postman.json** | Postman 工作区和环境配置信息 |

## 🚀 快速开始

### 1. 查看快速开始指南

```bash
cat docs/api/QUICK_START.md
```

或直接阅读：[QUICK_START.md](./QUICK_START.md)

### 2. 导入 Postman 集合

1. 打开 Postman
2. 点击 **Import**
3. 选择文件：`docs/api/petlife-openapi.yaml`
4. 导入到工作区：**PetLife 宠物生活管家**

### 3. 开始测试

选择环境 **Local Development**，开始测试 API！

## 📊 API 统计

### 总体统计

- **总接口数**：80+
- **模块数量**：14 个
- **用户端模块**：12 个
- **管理端模块**：2 个

### 模块分布

#### 用户端模块 (12个)

| 模块 | 接口数 | 路径前缀 |
|------|--------|----------|
| 认证 (Auth) | 4 | `/api/v1/auth/*` |
| 用户 (User) | 6 | `/api/v1/me/*` |
| 宠物 (Pet) | 7 | `/api/v1/pets/*` |
| 家庭 (Family) | 8 | `/api/v1/family/*` |
| 健康 (Health) | 5 | `/api/v1/pets/{petId}/health-records/*` |
| 提醒 (Reminder) | 4 | `/api/v1/pets/{petId}/reminders/*` |
| 日常 (Daily Log) | 5 | `/api/v1/pets/{petId}/daily-logs/*` |
| 时间轴 (Timeline) | 1 | `/api/v1/pets/{petId}/timeline` |
| 社区 (Community) | 9 | `/api/v1/community/*` |
| 服务 (Service) | 7 | `/api/v1/services/*`, `/api/v1/providers/*`, `/api/v1/appointments/*` |
| 通知 (Notification) | 3 | `/api/v1/notifications/*` |
| 首页 (Home) | 2 | `/api/v1/home/*` |

#### 管理端模块 (2个)

| 模块 | 接口数 | 路径前缀 |
|------|--------|----------|
| 审核 (Moderation) | 2 | `/api/v1/admin/moderation/*` |
| 后台服务 (Admin Service) | 9 | `/api/v1/admin/service/*` |

## 🎯 核心功能

### 用户端核心流程

```
用户注册登录
    ↓
创建宠物档案
    ↓
记录健康数据 → 自动生成提醒 → 同步时间轴
    ↓
记录日常生活 → 可选同步社区
    ↓
社区互动（点赞、评论、收藏）
    ↓
预约服务（医院、寄养、洗护、训练）
    ↓
家庭共养（邀请成员、权限管理）
```

### 管理端核心流程

```
审核管理
    ├─ 查看举报列表
    └─ 处理举报（确认违规/驳回）

服务商管理
    ├─ 维护服务商信息
    ├─ 管理服务项目
    ├─ 配置预约时段
    └─ 处理预约订单
```

## 🔑 Postman 工作区信息

### 已创建资源

| 资源类型 | 名称 | ID |
|----------|------|-----|
| 工作区 | PetLife 宠物生活管家 | `f3561cbd-0e5a-4dd4-a029-cda6ce4a8f02` |
| 环境 | Local Development | `3c049152-48c4-476e-a9ba-e09ddcee3638` |

### 环境变量

| 变量名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `base_url` | default | `http://localhost:8080` | API 基础 URL |
| `access_token` | secret | (空) | 访问令牌 |
| `refresh_token` | secret | (空) | 刷新令牌 |
| `current_user_id` | default | (空) | 当前用户 ID |
| `current_pet_id` | default | (空) | 当前宠物 ID |
| `test_mobile` | default | `13800000000` | 测试手机号 |

### 访问链接

- **工作区 URL**：https://web.postman.co/workspace/f3561cbd-0e5a-4dd4-a029-cda6ce4a8f02

## 📋 技术规范

### 基础约定

- **Base URL**：`/api/v1`
- **协议**：`HTTPS + JSON`
- **字段命名**：`snake_case`
- **时间格式**：ISO 8601 (UTC)
- **ID 类型**：后端 `bigint`，接口返回字符串

### 鉴权约定

```
Authorization: Bearer <access_token>
```

- refresh token 单独通过安全接口换取
- 管理端使用独立的后台账号体系

### 响应结构

成功响应：
```json
{
  "code": "OK",
  "message": "success",
  "data": {}
}
```

失败响应：
```json
{
  "code": "PET_NOT_FOUND",
  "message": "宠物不存在",
  "request_id": "trace_id_123"
}
```

### 分页约定

动态流接口使用游标分页：

```json
{
  "items": [],
  "next_cursor": "opaque_cursor",
  "has_more": true
}
```

适用于：
- 社区流
- 成长时间轴
- 萌宠日常列表
- 通知列表

## 🔍 常见错误码

| 错误码 | 场景 |
|--------|------|
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
| `FAMILY_ROLE_FORBIDDEN` | 家庭角色无权操作 |

## 📚 相关文档

### 技术文档

- [系统设计](../technical/01-system-design.md)
- [API 契约与领域事件](../technical/02-api-and-events.md)
- [数据库设计](../technical/03-ddl-draft.sql)
- [执行计划](../technical/04-execution-plan.md)
- [持久化集成](../technical/05-persistence-integration.md)
- [服务端代码结构](../technical/06-server-code-structure.md)

### 产品文档

- [产品概述](../product/01-product-overview.md)
- [信息架构](../product/02-information-architecture.md)
- [核心页面 PRD](../product/03-prd-core-pages.md)
- [低保真线框图](../product/04-low-fidelity-wireframes.md)
- [业务实体](../product/05-business-entities.md)

### 项目文档

- [当前交付状态](../project/01-current-delivery-status.md)
- [功能完成清单](../project/02-feature-completion-checklist.md)
- [UI 闭环清单](../project/03-ui-closure-checklist.md)

## 🛠️ 开发工具

### Postman

- **官网**：https://www.postman.com/
- **Web 版**：https://web.postman.co/
- **桌面版下载**：https://www.postman.com/downloads/

### OpenAPI 工具

- **Swagger Editor**：https://editor.swagger.io/
- **Swagger UI**：可用于生成交互式 API 文档
- **Redoc**：可用于生成美观的 API 文档

## 📞 支持

如有问题，请参考：

1. **快速开始指南**：[QUICK_START.md](./QUICK_START.md)
2. **详细配置指南**：[POSTMAN_SETUP.md](./POSTMAN_SETUP.md)
3. **OpenAPI 规范**：[petlife-openapi.yaml](./petlife-openapi.yaml)
4. **技术设计文档**：[../technical/](../technical/)

## 📝 更新日志

### 2026-04-28

- ✅ 创建 Postman 工作区：PetLife 宠物生活管家
- ✅ 配置环境变量：Local Development
- ✅ 生成完整 OpenAPI 3.0 规范（80+ 接口）
- ✅ 创建快速开始指南
- ✅ 创建详细配置指南
- ✅ 创建文档导航

### 下一步计划

- ⏭️ 添加 Postman Collection Runner 脚本
- ⏭️ 添加自动化测试用例
- ⏭️ 生成 Swagger UI 文档
- ⏭️ 添加 API 性能测试脚本

## 🎉 开始使用

准备好了吗？

1. 阅读 [QUICK_START.md](./QUICK_START.md)
2. 导入 [petlife-openapi.yaml](./petlife-openapi.yaml) 到 Postman
3. 选择环境 **Local Development**
4. 开始测试！

祝您使用愉快！🚀
