# petlife-server

服务端采用 `Java 21 + Spring Boot 3.x`，当前按模块化单体组织。

## 包结构

```text
com.petlife.server
├─ bootstrap
├─ common
├─ config
└─ modules
   ├─ admin
   ├─ auth
   ├─ community
   ├─ dailylog
   ├─ family
   ├─ health
   ├─ moderation
   ├─ notification
   ├─ pet
   ├─ reminder
   ├─ service
   ├─ timeline
   └─ user
```

## 当前实现边界

- `commerce` 与 `device` 仅保留设计和数据库预留，不进入当前实现批次。
- `admin` 模块承接当前开发范围内的后台管理能力，不扩成完整运营平台。

## 启动命令

```bash
mvn spring-boot:run
```
