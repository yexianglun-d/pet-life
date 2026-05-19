# PetLife API 快速开始指南

## 🚀 5 分钟快速上手

### 第 1 步：访问 Postman 工作区

已为您创建好的 Postman 工作区：

- **工作区名称**：PetLife 宠物生活管家
- **工作区 ID**：`f3561cbd-0e5a-4dd4-a029-cda6ce4a8f02`
- **访问链接**：https://web.postman.co/workspace/f3561cbd-0e5a-4dd4-a029-cda6ce4a8f02

### 第 2 步：导入 API 集合

#### 方式 A：通过 OpenAPI 文件导入（推荐）

1. 打开 Postman 应用或 Web 版
2. 点击左上角 **Import** 按钮
3. 选择 **File** 标签
4. 拖拽或选择文件：`docs/api/petlife-openapi.yaml`
5. 确认导入设置，点击 **Import**

#### 方式 B：通过 Git 仓库导入

如果项目已推送到 Git：

1. 在 Postman 中点击 **Import**
2. 选择 **Link** 标签
3. 输入 OpenAPI 文件的 Git URL
4. 点击 **Continue** → **Import**

### 第 3 步：选择环境

在 Postman 右上角的环境下拉菜单中选择：

**Local Development**

环境变量已自动配置：
- `base_url`: `http://localhost:8080`
- `access_token`: (登录后自动填充)
- `refresh_token`: (登录后自动填充)
- `test_mobile`: `13800000000`

### 第 4 步：启动本地服务器

```bash
cd server
./mvnw spring-boot:run
```

服务器将在 `http://localhost:8080` 启动。

### 第 5 步：测试第一个接口

#### 1. 发送验证码

```
POST {{base_url}}/api/v1/auth/sms/send

Body (JSON):
{
  "mobile": "13800000000",
  "scene": "login"
}
```

#### 2. 登录

```
POST {{base_url}}/api/v1/auth/login/sms

Body (JSON):
{
  "mobile": "13800000000",
  "code": "739204"
}
```

`code` 示例不是固定验证码，应填写短信中收到的 6 位验证码。当前服务端默认 `dev_noop` 短信供应商只记录发送受理状态，不返回调试验证码；联调登录需要先接入真实短信供应商或由测试环境提供安全验证码注入方式。

**重要**：在登录接口的 **Tests** 标签添加以下脚本，自动保存 Token：

```javascript
if (pm.response.code === 200) {
    const jsonData = pm.response.json();
    if (jsonData.code === "OK" && jsonData.data) {
        pm.environment.set("access_token", jsonData.data.access_token);
        pm.environment.set("refresh_token", jsonData.data.refresh_token);
        console.log("✅ Token 已保存");
    }
}
```

#### 3. 获取当前用户信息

```
GET {{base_url}}/api/v1/me

Headers:
Authorization: Bearer {{access_token}}
```

✅ 如果返回用户信息，说明配置成功！

## 📋 完整测试流程

### 用户端完整流程

```
1. 认证
   ├─ 发送验证码
   ├─ 短信登录 ✅ (保存 token)
   └─ 获取当前用户信息

2. 宠物管理
   ├─ 创建宠物 ✅ (保存 pet_id)
   ├─ 获取宠物列表
   ├─ 获取宠物详情
   └─ 获取宠物主页摘要

3. 健康记录
   ├─ 创建健康记录（疫苗）
   ├─ 获取健康记录列表
   └─ 查看时间轴（验证自动同步）

4. 提醒管理
   ├─ 创建周期提醒
   ├─ 获取提醒列表
   └─ 完成提醒（验证自动生成下一次）

5. 日常记录
   ├─ 创建日常（公开 + 同步社区）
   ├─ 获取日常列表
   └─ 查看时间轴

6. 社区互动
   ├─ 获取社区流
   ├─ 查看帖子详情
   ├─ 点赞帖子
   ├─ 评论帖子
   └─ 举报帖子

7. 服务预约
   ├─ 获取服务首页
   ├─ 获取服务商列表
   ├─ 查看可预约时段
   └─ 创建预约

8. 家庭共养
   ├─ 初始化家庭
   ├─ 发起邀请
   ├─ 接受邀请
   └─ 修改成员角色
```

### 管理端流程

```
1. 审核管理
   ├─ 获取举报列表
   └─ 处理举报（确认违规/驳回）

2. 服务商管理
   ├─ 创建服务商
   ├─ 创建服务项目
   ├─ 创建预约时段
   ├─ 获取预约列表
   └─ 更新预约状态
```

## 🎯 核心接口速查

### 认证相关
```
POST /api/v1/auth/sms/send          # 发送验证码
POST /api/v1/auth/login/sms         # 短信登录
POST /api/v1/auth/refresh           # 刷新 Token
POST /api/v1/auth/logout            # 退出登录
```

### 用户相关
```
GET  /api/v1/me                     # 获取当前用户
GET  /api/v1/me/settings            # 获取用户设置
PATCH /api/v1/me/profile            # 修改用户资料
PATCH /api/v1/me/settings/current-pet  # 切换当前宠物
```

### 宠物相关
```
GET  /api/v1/pets                   # 获取宠物列表
POST /api/v1/pets                   # 创建宠物
GET  /api/v1/pets/{petId}           # 获取宠物详情
PATCH /api/v1/pets/{petId}          # 更新宠物
GET  /api/v1/pets/{petId}/summary   # 获取宠物主页摘要
```

### 健康记录
```
GET  /api/v1/pets/{petId}/health-records           # 获取健康记录列表
POST /api/v1/pets/{petId}/health-records           # 创建健康记录
GET  /api/v1/pets/{petId}/health-records/{id}      # 获取健康记录详情
```

### 提醒管理
```
GET  /api/v1/pets/{petId}/reminders                # 获取提醒列表
POST /api/v1/pets/{petId}/reminders                # 创建提醒
PATCH /api/v1/pets/{petId}/reminders/{id}/complete # 完成提醒
PATCH /api/v1/pets/{petId}/reminders/{id}/skip     # 跳过提醒
```

### 社区相关
```
GET  /api/v1/community/feed                        # 获取社区流
GET  /api/v1/community/posts/{postId}              # 获取帖子详情
POST /api/v1/community/posts/{postId}/like         # 点赞
POST /api/v1/community/posts/{postId}/comments     # 评论
POST /api/v1/community/posts/{postId}/report       # 举报
```

### 服务预约
```
GET  /api/v1/services/home                         # 服务首页
GET  /api/v1/providers                             # 获取服务商列表
GET  /api/v1/providers/{id}/slots                  # 获取可预约时段
POST /api/v1/appointments                          # 创建预约
```

## 🔧 常用 Postman 技巧

### 1. 使用环境变量

在请求中使用 `{{variable_name}}` 引用环境变量：

```
{{base_url}}/api/v1/pets/{{current_pet_id}}
```

### 2. 自动保存响应数据

在 **Tests** 标签中添加脚本：

```javascript
// 保存宠物 ID
const data = pm.response.json().data;
if (data && data.pet_id) {
    pm.environment.set("current_pet_id", data.pet_id);
}
```

### 3. 批量运行测试

1. 点击集合名称旁的 **...** 菜单
2. 选择 **Run collection**
3. 选择要运行的请求
4. 设置迭代次数和延迟
5. 点击 **Run** 开始

### 4. 导出集合

1. 右键点击集合名称
2. 选择 **Export**
3. 选择格式（推荐 Collection v2.1）
4. 保存为 JSON 文件

## 📚 更多资源

- **完整配置指南**：`docs/api/POSTMAN_SETUP.md`
- **OpenAPI 规范**：`docs/api/petlife-openapi.yaml`
- **API 设计文档**：`docs/technical/02-api-and-events.md`
- **系统设计文档**：`docs/technical/01-system-design.md`

## ❓ 常见问题

### Q: Token 过期怎么办？

A: 调用刷新接口或重新登录：

```
POST {{base_url}}/api/v1/auth/refresh
{
  "refresh_token": "{{refresh_token}}"
}
```

### Q: 如何切换到开发环境？

A: 修改环境变量 `base_url` 为开发环境地址：

```
https://api-dev.petlife.com
```

### Q: 如何查看所有接口？

A: 打开 `docs/api/petlife-openapi.yaml` 文件，或在 Postman 中查看导入的集合。

### Q: 接口返回 401 Unauthorized？

A: 检查：
1. Token 是否已保存到环境变量
2. Token 是否过期
3. Authorization header 是否正确设置

### Q: 如何测试管理端接口？

A: 管理端接口需要额外的权限验证，部分接口需要在 Header 中添加：

```
X-Admin-Operator: admin_name
```

## 🎉 开始使用

现在您已经准备好了！

1. ✅ Postman 工作区已创建
2. ✅ 环境变量已配置
3. ✅ OpenAPI 规范已生成
4. ⏭️ 导入集合到 Postman
5. ⏭️ 启动本地服务器
6. ⏭️ 开始测试 API

祝您使用愉快！🚀
