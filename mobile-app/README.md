# petlife_mobile_app

移动端当前已经补齐 Flutter 工程结构、Android/iOS 平台壳和业务目录。

## 当前说明

- Android `applicationId`：`com.petlife.mobile`
- iOS `bundle identifier`：`com.petlife.mobile`

## 当前范围

- 首页壳
- 路由壳
- 主题壳
- 宠物主链路的模块化目录

商城与设备目前只做预留页，不接真实后端链路。

## 接口地址配置

移动端默认连接生产接口域名：`https://pet.api.howied.me`。

本地联调可通过编译参数覆盖：

```bash
flutter run --dart-define=PETLIFE_API_BASE_URL=http://10.0.2.2:8080
```

## 定位本地运行

移动端当前位置使用系统定位能力，不需要注入高德移动端 SDK Key。高德能力仍保留在服务端地理编码和外部地图导航入口。

Android 本地运行示例：

```bash
cd mobile-app
flutter run
```

iOS 本地运行示例：

```bash
cd mobile-app
flutter run
```

Android debug 构建示例：

```bash
cd mobile-app
flutter build apk --debug
```
