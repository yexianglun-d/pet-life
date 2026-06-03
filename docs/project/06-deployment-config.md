# PetLife 发布域名与环境配置

## 域名

- Web 站点：`https://pet.howied.me`
- 后台/API：`https://pet.api.howied.me`

## server

生产环境至少需要注入：

```bash
export PETLIFE_SERVER_PORT=8080
export PETLIFE_DATASOURCE_URL='jdbc:mysql://<host>:3306/pet_life?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_0900_ai_ci&serverTimezone=Asia/Shanghai'
export PETLIFE_DATASOURCE_USERNAME='<username>'
export PETLIFE_DATASOURCE_PASSWORD='<password>'
export PETLIFE_CORS_ALLOWED_ORIGIN_PATTERNS='https://pet.howied.me'
```

如启用高德地图 Web 服务、Redis、媒体存储，再按实际环境补充：

```bash
export PETLIFE_AMAP_WEB_SERVICE_KEY='<amap-web-service-key>'
export PETLIFE_REDIS_HOST='<redis-host>'
export PETLIFE_REDIS_PORT=6379
export PETLIFE_REDIS_PASSWORD='<redis-password>'
export PETLIFE_MEDIA_ROOT_PATH='/data/petlife-media'
```

## admin-web

生产构建默认连接：

```bash
https://pet.api.howied.me
```

如需临时覆盖：

```bash
VITE_API_BASE_URL=https://pet.api.howied.me npm run build
```

## mobile-app

移动端默认连接：

```bash
https://pet.api.howied.me
```

如需本地联调：

```bash
flutter run --dart-define=PETLIFE_API_BASE_URL=http://10.0.2.2:8080
```

Android/iOS 高德 Key 仍只能通过本地构建参数或平台配置注入，不提交真实 Key 到仓库。
