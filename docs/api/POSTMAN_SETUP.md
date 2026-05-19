# PetLife API Postman 集合配置指南

## 项目信息

- **工作区 ID**: `f3561cbd-0e5a-4dd4-a029-cda6ce4a8f02`
- **工作区名称**: PetLife 宠物生活管家
- **环境 ID**: `3c049152-48c4-476e-a9ba-e09ddcee3638`
- **环境名称**: Local Development

## 已创建资源

### 1. Postman 工作区
- 名称：PetLife 宠物生活管家
- 类型：个人工作区
- 描述：包含 PetLife 项目完整 API 集合

### 2. 环境变量
已配置以下环境变量：

| 变量名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `base_url` | default | `http://localhost:8080` | API 基础 URL |
| `access_token` | secret | (空) | 访问令牌 |
| `refresh_token` | secret | (空) | 刷新令牌 |
| `current_user_id` | default | (空) | 当前用户 ID |
| `current_pet_id` | default | (空) | 当前宠物 ID |
| `test_mobile` | default | `13800000000` | 测试手机号 |

## OpenAPI 规范文件

完整的 API 规范已创建在：`docs/api/petlife-openapi.yaml`

该文件包含：
- ✅ 所有 14 个模块的完整接口定义
- ✅ 详细的请求参数说明
- ✅ 响应结构定义
- ✅ 业务规则说明
- ✅ 错误码说明

## 导入 Postman 集合

### 方式一：通过 OpenAPI 规范导入

1. 打开 Postman
2. 点击 **Import** 按钮
3. 选择 **File** 标签
4. 上传 `docs/api/petlife-openapi.yaml` 文件
5. 选择工作区：**PetLife 宠物生活管家**
6. 点击 **Import** 完成导入

### 方式二：通过 Postman Web 导入

1. 访问 [Postman Web](https://web.postman.co/)
2. 进入工作区：**PetLife 宠物生活管家**
3. 点击 **Import** → **Link**
4. 如果 OpenAPI 文件已上传到 Git 仓库，可以直接使用 URL 导入

## API 模块说明

### 用户端模块 (12个)

1. **认证模块 (Auth)** - `/api/v1/auth/*`
   - 发送短信验证码
   - 短信登录
   - 刷新 Token
   - 退出登录

2. **用户模块 (User)** - `/api/v1/me/*`
   - 获取当前用户信息
   - 获取用户设置
   - 修改用户资料
   - 修改当前宠物
   - 修改当前城市
   - 修改通知与隐私设置

3. **宠物模块 (Pet)** - `/api/v1/pets/*`
   - 获取宠物列表
   - 创建宠物
   - 获取宠物详情
   - 更新宠物
   - 归档宠物
   - 删除宠物
   - 获取宠物主页摘要

4. **家庭模块 (Family)** - `/api/v1/family/*`
   - 获取家庭信息
   - 创建或初始化家庭
   - 发起家庭邀请
   - 查看家庭邀请
   - 接受家庭邀请
   - 拒绝家庭邀请
   - 修改成员角色
   - 移除家庭成员

5. **健康模块 (Health)** - `/api/v1/pets/{petId}/health-records/*`
   - 获取健康记录列表
   - 创建健康记录
   - 获取健康记录详情
   - 编辑健康记录
   - 删除健康记录

6. **提醒模块 (Reminder)** - `/api/v1/pets/{petId}/reminders/*`
   - 获取提醒列表
   - 创建提醒
   - 完成提醒
   - 跳过提醒

7. **日常模块 (Daily Log)** - `/api/v1/pets/{petId}/daily-logs/*`
   - 获取萌宠日常列表
   - 创建萌宠日常
   - 获取萌宠日常详情
   - 更新萌宠日常
   - 删除萌宠日常

8. **时间轴模块 (Timeline)** - `/api/v1/pets/{petId}/timeline`
   - 获取成长时间轴

9. **社区模块 (Community)** - `/api/v1/community/*`
   - 获取社区流
   - 获取帖子详情
   - 获取帖子评论列表
   - 评论帖子
   - 点赞帖子
   - 取消点赞
   - 收藏帖子
   - 取消收藏
   - 举报帖子

10. **服务模块 (Service)** - `/api/v1/services/*`, `/api/v1/providers/*`, `/api/v1/appointments/*`
    - 服务首页聚合
    - 获取服务商列表
    - 获取服务商详情
    - 获取服务商可预约时段
    - 获取预约记录
    - 创建预约
    - 取消预约

11. **通知模块 (Notification)** - `/api/v1/notifications/*`
    - 消息通知列表
    - 单条已读
    - 批量已读

12. **首页模块 (Home)** - `/api/v1/home/*`
    - 首页周报
    - 首页月报

### 管理端模块 (2个)

13. **审核模块 (Moderation)** - `/api/v1/admin/moderation/*`
    - 获取举报列表
    - 处理举报

14. **后台服务模块 (Admin Service)** - `/api/v1/admin/service/*`
    - 获取服务商列表
    - 创建服务商
    - 更新服务商
    - 创建服务项目
    - 更新服务项目
    - 创建预约时段
    - 更新预约时段
    - 获取预约列表
    - 更新预约状态

## 使用流程

### 1. 登录获取 Token

```bash
# 1. 发送验证码
POST {{base_url}}/api/v1/auth/sms/send
{
  "mobile": "{{test_mobile}}",
  "scene": "login"
}

# 2. 短信登录
POST {{base_url}}/api/v1/auth/login/sms
{
  "mobile": "{{test_mobile}}",
  "code": "739204"
}

# code 示例不是固定验证码，应填写短信中收到的 6 位验证码。
# 当前默认 dev_noop 短信供应商不会返回调试验证码；联调登录需要真实短信供应商或测试环境安全注入验证码。

# 响应会包含 access_token 和 refresh_token
# 建议在 Tests 脚本中自动保存到环境变量：
pm.environment.set("access_token", pm.response.json().data.access_token);
pm.environment.set("refresh_token", pm.response.json().data.refresh_token);
```

### 2. 后续请求自动携带 Token

所有需要鉴权的接口会自动使用 `{{access_token}}` 环境变量：

```
Authorization: Bearer {{access_token}}
```

### 3. Token 刷新

当 access_token 过期时：

```bash
POST {{base_url}}/api/v1/auth/refresh
{
  "refresh_token": "{{refresh_token}}"
}
```

## 测试建议

### 基础流程测试

1. **用户注册登录流程**
   - 发送验证码 → 短信登录 → 获取当前用户信息

2. **宠物管理流程**
   - 创建宠物 → 获取宠物列表 → 获取宠物详情 → 更新宠物

3. **健康记录流程**
   - 创建健康记录 → 获取健康记录列表 → 查看时间轴

4. **提醒流程**
   - 创建提醒 → 获取提醒列表 → 完成提醒

5. **日常记录流程**
   - 创建日常 → 获取日常列表 → 同步到社区

6. **社区互动流程**
   - 获取社区流 → 查看帖子详情 → 点赞 → 评论

7. **服务预约流程**
   - 获取服务商列表 → 查看可预约时段 → 创建预约

### 管理端测试

1. **审核流程**
   - 获取举报列表 → 处理举报

2. **服务商管理流程**
   - 创建服务商 → 创建服务项目 → 创建预约时段

## 自动化测试脚本

### 登录后自动保存 Token

在登录接口的 **Tests** 标签中添加：

```javascript
if (pm.response.code === 200) {
    const jsonData = pm.response.json();
    if (jsonData.code === "OK" && jsonData.data) {
        pm.environment.set("access_token", jsonData.data.access_token);
        pm.environment.set("refresh_token", jsonData.data.refresh_token);
        if (jsonData.data.user) {
            pm.environment.set("current_user_id", jsonData.data.user.user_id);
        }
        if (jsonData.data.current_pet_id) {
            pm.environment.set("current_pet_id", jsonData.data.current_pet_id);
        }
        console.log("Token 已保存到环境变量");
    }
}
```

### 创建宠物后自动保存 ID

在创建宠物接口的 **Tests** 标签中添加：

```javascript
if (pm.response.code === 200) {
    const jsonData = pm.response.json();
    if (jsonData.code === "OK" && jsonData.data && jsonData.data.pet_id) {
        pm.environment.set("current_pet_id", jsonData.data.pet_id);
        console.log("宠物 ID 已保存：" + jsonData.data.pet_id);
    }
}
```

## 常见问题

### Q1: 如何切换环境？

在 Postman 右上角选择环境下拉菜单，选择 **Local Development** 或其他环境。

### Q2: 如何修改 base_url？

点击环境名称 → 编辑 `base_url` 变量值。

常用值：
- 本地开发：`http://localhost:8080`
- 开发环境：`https://api-dev.petlife.com`
- 生产环境：`https://api.petlife.com`

### Q3: Token 过期怎么办？

调用刷新 Token 接口，或重新登录。

### Q4: 如何批量运行测试？

1. 点击集合右侧的 **...** 菜单
2. 选择 **Run collection**
3. 选择要运行的请求
4. 点击 **Run** 开始批量测试

## 技术规范

### 字段命名
- 统一使用 `snake_case`

### 时间格式
- ISO 8601 (UTC)
- 示例：`2026-04-20T10:30:00Z`

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

### 常见错误码

| 错误码 | 说明 |
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

## 下一步

1. ✅ 已创建 Postman 工作区
2. ✅ 已配置环境变量
3. ✅ 已生成完整 OpenAPI 规范
4. ⏭️ 导入 OpenAPI 规范到 Postman
5. ⏭️ 配置自动化测试脚本
6. ⏭️ 运行完整测试流程

## 相关文件

- OpenAPI 规范：`docs/api/petlife-openapi.yaml`
- Postman 配置：`.postman.json`
- API 文档：`docs/technical/02-api-and-events.md`
- 系统设计：`docs/technical/01-system-design.md`
