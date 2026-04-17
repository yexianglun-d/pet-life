# Current Delivery Status

## 文档目的

该文档用于记录 `PetLife / 宠物生活管家` 当前阶段已经完成的交付内容、验证结果、延期范围和下一步建议，作为首轮工程提交的状态说明。

## 当前完成情况

### 1. 产品与技术文档基线已完成

- 已完成产品总览、信息架构、页面清单、PRD 骨架、低保真线框、业务实体模型。
- 已完成系统设计、接口契约、DDL 草案、执行计划。
- 已将项目级 UI 设计规范固化到根目录 `DESIGN.md`。

### 2. 工程骨架已完成

- `server`：Java 21 + Spring Boot 模块化单体服务端骨架。
- `mobile-app`：Flutter 用户端工程骨架与 Android / iOS 原生壳。
- `admin-web`：Vue 3 + Vite + Element Plus 后台管理端骨架。
- 根目录已补齐 `README.md`、`AGENTS.md`、`.editorconfig`、`.gitignore`。

### 3. 服务端首批主链路已完成

当前已实现以下接口与模块：

- 认证：
  - `POST /api/v1/auth/sms/send`
  - `POST /api/v1/auth/login/sms`
- 当前用户：
  - `GET /api/v1/me`
  - `PATCH /api/v1/me/settings/current-pet`
- 宠物主档：
  - `GET /api/v1/pets`
  - `POST /api/v1/pets`
  - `GET /api/v1/pets/{petId}`
  - `PATCH /api/v1/pets/{petId}`
  - `GET /api/v1/pets/{petId}/summary`
- 健康记录：
  - `GET /api/v1/pets/{petId}/health-records`
  - `POST /api/v1/pets/{petId}/health-records`
- 提醒：
  - `GET /api/v1/pets/{petId}/reminders`
  - `POST /api/v1/pets/{petId}/reminders`
  - `PATCH /api/v1/pets/{petId}/reminders/{reminderId}/complete`
- 萌宠日常：
  - `GET /api/v1/pets/{petId}/daily-logs`
  - `POST /api/v1/pets/{petId}/daily-logs`

补充说明：

- 已补齐开发期 Bearer Token 鉴权过滤器，除登录与健康检查外的接口会校验登录态。
- 服务端已移除开发期内存适配器，当前以 MySQL 作为唯一数据源。
- 已补齐 MyBatis 持久化 Mapper 与数据库应用服务实现，登录、当前用户、宠物、健康记录、提醒、萌宠日常都已切到数据库路径。
- 已将登录态持久化落到 `user_sessions`，数据库只保存会话摘要，不保存客户端原始 token。

### 4. 移动端首批主链路已完成

- 已完成应用主导航壳：
  - 首页
  - 宠物
  - 社区占位
  - 服务占位
  - 我的占位
- 已完成首页与宠物页的业务骨架，页面结构已承接：
  - 当前宠物
  - 提醒
  - 健康记录
  - 萌宠日常
- 已完成用户端网络层与仓储层：
  - 统一 `ApiClient`
  - `PetLifeRepository` 抽象
  - `NetworkPetLifeRepository` 实现
- 已完成登录入口与本地会话：
  - `AppEntryPage`
  - `LoginPage`
  - `AppSessionStore`

### 5. 后台管理端首批能力已完成

- 已完成后台登录壳。
- 已完成路由守卫。
- 已完成控制台首页骨架。
- 已完成最小后台布局和基础导航。

## 当前验证结果

### 服务端

- `mvn -Dmaven.repo.local=/tmp/petlife-m2 test` 通过

### 移动端

- `flutter pub get` 通过
- `flutter test` 通过

### 后台

- `npm install` 已完成
- `npm run type-check` 通过
- `npm run build` 通过

补充说明：

- 后台生产构建存在 Vite 大包告警，当前不阻塞开发，但后续需要做拆包优化。

## 当前明确延后

- 商城真实交易后端
- 智能设备厂商接入
- 真实短信服务
- 后台真实业务列表、表单、审核流联调

## 当前项目状态判断

当前项目已经完成“从产品定义到首批业务代码落地”的第一阶段，进入以下状态：

- 可以继续稳定扩写业务代码
- 可以开始用户端与服务端真实联调
- 可以开始后台真实接口接入
- 不再需要等待新的大规划才能开发

## 下一步建议

1. 移动端继续补接口异常态、空态、加载态和真实表单细节。
2. 后台接入审核中心、服务商管理、系统配置的真实接口。
