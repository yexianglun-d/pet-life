# PetLife Workspace

`PetLife` 是 `宠物生活管家` 的工程工作区，当前按照三端并行方式组织：

- `mobile-app`：Flutter 用户端
- `server`：Java 21 + Spring Boot 模块化单体服务端
- `admin-web`：Vue 3 + Vite 后台管理端

移动端原生标识：

- Android `applicationId`：`com.petlife.mobile`
- iOS `bundle identifier`：`com.petlife.mobile`

## 当前开发范围

当前批次聚焦以下链路：

- 登录与用户会话
- 宠物主档与家庭共养
- 健康记录、提醒、萌宠日常、时间轴
- 社区、举报与审核
- 服务目录与预约
- 后台管理端

当前明确延后：

- 商城真实交易后端
- 设备厂商接入与事件链路

其余范围默认不按“先占位即可”处理，必须按完整交付标准推进。

## 目录结构

```text
.
├─ AGENTS.md
├─ admin-web
├─ docs
├─ mobile-app
└─ server
```

## 约定

1. Java 根包固定为 `com.petlife.server`。
2. 服务端采用 `modules` 业务分层，公共能力放在 `common` 与 `config`。
3. 已实现服务端模块统一使用 `controller / dto / service / domain.entity / converter / persistence.dataobject / persistence.command` 结构。
4. 所有核心业务代码必须具备清晰注释和明确命名。
5. 预留能力必须通过占位页或保留接口表达，不能提前暴露半成品链路。
6. 所有 UI 实现默认遵循根目录 `DESIGN.md` 中的视觉和交互规范。
7. 除 `commerce` 与 `device` 外，其余能力不以“主链路打通”作为完成标准，必须覆盖页面、交互、异常态、服务端状态机、后台治理和必要验证。

服务端详细代码结构规范见：

- `AGENTS.md`
- `docs/technical/06-server-code-structure.md`

## 设计基线

- UI 设计规范：`DESIGN.md`
- 产品与页面结构参考：`docs/product/04-low-fidelity-wireframes.md`
- 当前阶段交付说明：`docs/project/01-current-delivery-status.md`
- 功能完成清单：`docs/project/02-feature-completion-checklist.md`
- UI 收口清单：`docs/project/03-ui-closure-checklist.md`
- 当前视觉方向：温暖、亲和、治愈、清晰，不做低幼化或炫技式装饰。

## 本地启动前提

- `server` 需要 `Java 21` 和 `Maven 3.9+`
- `admin-web` 需要 `Node 22+` 和 `NPM 10+`
- `mobile-app` 需要安装 `Flutter SDK` 后再补原生平台壳

## 当前进度口径

- 当前项目已进入正式业务开发阶段。
- 完成度以 `docs/project/02-feature-completion-checklist.md` 为准。
- 除延后范围外，所有模块统一按“完整交付”而不是“核心可用”判断是否完成。
