# 发版流程

## 1. 改版本号

`app/build.gradle.kts`：

```kotlin
versionCode = 13          // 每次发版必须 +1
versionName = "2.2.0"
```

同时更新 `CHANGELOG.md`（在最上面加一节）。

## 2. 构建

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64
./gradlew assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`
未配置 `keystore.properties` 时会退化为 debug 签名（仅限自测）。

## 3. 签名

正式包用仓库外的密钥（**绝不入库**）：

- 密钥文件：`friendsim-release.jks`
- 配置：根目录 `keystore.properties`（已在 .gitignore）

```
storeFile=/绝对路径/friendsim-release.jks
storePassword=...
keyAlias=friendsim
keyPassword=...
```

校验：

```bash
apksigner verify --print-certs app-release.apk
```

证书 SHA-256 应与上次发版一致，否则老用户无法覆盖安装。

## 4. 打 tag 并发布

```bash
git tag -a v2.2.0 -m "FriendSim 2.2.0"
git push origin v2.2.0
```

GitHub → Releases → Draft a new release → 选择 tag → 标题 `FriendSim 2.2.0`
→ 正文贴 CHANGELOG 该版本内容 → 附件上传 `app-release.apk` → Publish。

## 5. 注意事项

- `versionCode` 必须递增，否则无法升级覆盖
- 每次发版都用同一把 keystore；丢了就只能让用户卸载重装
- 不要提交 `local.properties`、`keystore.properties`、`*.jks`、`*.apk`
