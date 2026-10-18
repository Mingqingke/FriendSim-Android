# 贡献指南

## 本地构建
1. JDK 21，Android SDK 37
2. 根目录建 `local.properties`：`sdk.dir=/path/to/android-sdk`
3. `./gradlew assembleDebug`

## 约定
- 动效参数一律从 `ui/motion/MotionTokens.kt` 取，不要新增独立的 spring
- 文案保持克制：标签 + 一行脚注，不写说明书
- 新增存储字段记得给默认值（旧数据要能读）
- 提交前确认没有 `local.properties`、`keystore.properties`、`*.jks`、`*.apk`

## 提交信息
`模块: 做了什么`，例如 `chat: 修复长消息折叠后无法展开`
