# Server Code Structure

## 文档目的

该文档用于固化 `petlife-server` 的服务端代码结构规范，避免后续开发继续把数据库读取模型、业务实体、接口响应和应用服务混写在一起。

当前文档是根目录 `AGENTS.md` 在服务端工程中的展开版，作为后续新增模块和代码评审的统一标准。

## 标准模块结构

每个已实现业务模块统一采用如下目录结构：

```text
modules/<domain>
├─ controller
├─ dto
│  ├─ request
│  └─ response
├─ service
├─ domain
│  └─ entity
├─ converter
└─ persistence
   ├─ dataobject
   └─ command
```

说明：

- `controller`：只处理接口入参与出参，不承接业务编排。
- `service`：负责业务流程、权限校验、状态切换和跨 Mapper 协调。
- `domain.entity`：表达业务稳定语义，不直接绑定数据库表字段命名。
- `converter`：统一处理 `DataObject -> Entity -> Response` 转换。
- `persistence.dataobject`：MyBatis 查询结果对象。
- `persistence.command`：MyBatis 写入命令对象。

## 命名规范

### 1. Java 类型后缀

- 控制器：`*Controller`
- 应用服务：`*ApplicationService`
- 领域实体：`*Entity`
- 持久化读模型：`*DataObject`
- 持久化写命令：`*Command`
- 持久化接口：`*PersistenceMapper`
- 请求 DTO：`*Request`
- 响应 DTO：`*Response`
- 转换器：`*Converter`

### 2. 包命名

- 根包固定为 `com.petlife.server`
- 业务模块固定为 `com.petlife.server.modules.<domain>`
- 不使用含义不清的目录名，例如 `util`、`temp`、`handler2`

## 分层边界

### 1. Controller 边界

- `controller` 只能依赖 `service`、`dto.request`、`dto.response`
- 控制器不得直接依赖 `persistence`
- 控制器不得直接拼装数据库字段映射

### 2. Service 边界

- `service` 负责业务编排，不直接把 `DataObject` 暴露给外层
- 服务层可以协调多个 Mapper，但必须在业务层转成 `Entity`
- 跨模块视图复用通过 `converter` 处理，不通过应用服务互相调用视图拼装方法

### 3. Persistence 边界

- `persistence` 只负责 SQL 和表字段映射
- Mapper 查询统一返回 `DataObject`
- Mapper 写入统一使用 `Command` 或明确参数
- 持久化层不得直接返回 `Response DTO`

### 4. Converter 边界

- `converter` 是 `DataObject` 与 `Entity` 之间的唯一转换入口
- `converter` 负责把领域实体转换成对外接口响应
- 枚举归一化、状态值兼容、时间格式映射等非平凡转换逻辑统一放在该层

## 注释规范

遵循阿里巴巴 Java 开发手册，重点要求如下：

1. 核心业务规则必须有 Javadoc 或块注释，例如默认初始化、登录建档、状态归一化。
2. 注释必须解释“为什么这样做”，而不是重复代码字面含义。
3. 简单 getter、setter、显而易见的字段赋值不写无效注释。

## 当前已落地模块

当前已经按该结构完成规范收敛的模块：

- `auth`
- `user`
- `pet`
- `health`
- `reminder`
- `dailylog`

这些模块已经形成以下稳定链路：

- `MyBatis Mapper -> DataObject`
- `Converter -> Entity`
- `ApplicationService -> Response DTO`

## 后续执行要求

1. 新增服务端业务模块时，必须先按本结构建目录再写代码。
2. 若出现跨层直接透传数据库对象，视为结构违规，需要在进入业务扩写前先整改。
3. 若命名、包结构或边界规则发生变化，必须同步更新 `AGENTS.md` 与本文档。
