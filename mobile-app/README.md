# petlife_mobile_app

移动端当前已经补齐 Flutter 工程结构、Android/iOS 平台壳和业务目录。

## 当前说明

- Android `applicationId`：`com.petlife.mobile`
- iOS `bundle identifier`：`com.petlife.mobile`
- 高德 Android / iOS Key 只允许通过本地构建参数注入，不写入仓库。

## 当前范围

- 首页壳
- 路由壳
- 主题壳
- 宠物主链路的模块化目录

商城与设备目前只做预留页，不接真实后端链路。

## 高德定位本地运行

移动端定位需要按当前平台注入高德 Key，否则服务中心会展示“地图定位还没有完成本地 Key 配置”，并继续允许浏览服务商。

Android 本地运行示例：

```bash
cd mobile-app
ORG_GRADLE_PROJECT_AMAP_ANDROID_KEY="你的 Android Key" \
flutter run \
  --dart-define=AMAP_ANDROID_KEY="你的 Android Key"
```

iOS 本地运行示例：

```bash
cd mobile-app
flutter run \
  --dart-define=AMAP_IOS_KEY="你的 iOS Key"
```

Android debug 构建示例：

```bash
cd mobile-app
ORG_GRADLE_PROJECT_AMAP_ANDROID_KEY="你的 Android Key" \
flutter build apk --debug \
  --dart-define=AMAP_ANDROID_KEY="你的 Android Key"
```

如果同时要验证两端，可以同时传入：

```bash
flutter run \
  --dart-define=AMAP_ANDROID_KEY="你的 Android Key" \
  --dart-define=AMAP_IOS_KEY="你的 iOS Key"
```
