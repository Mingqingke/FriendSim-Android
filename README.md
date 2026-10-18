# FriendSim

一款可以让你跟朋友对话的简易聊天工具。

导入一张照片、一段声音，就能和一个「像 TA」的朋友接着聊。不填 API Key 也能用，断网也不会没回应。

> 开发中项目，功能已闭环但真机细节仍在打磨。欢迎 issue。

## 特性

**对话**
- 三级降级：自备 Key（DeepSeek 等 OpenAI 兼容）→ 免 Key 公共端点（KeylessAI / Pollinations / OVHcloud）→ 离线预设
- 流式输出，回复按心情加权延迟后逐字显影
- 打字指示器、时间分割线、引用回复、重新生成、从这里重来（回滚分支）
- 长消息折叠「全文」；语音条（按住说话 + 播放波形）；图片消息
- 发送失败红点重发；断网/超时只在输入框上方留一条窄提示

**人设与记忆**
- 灵魂档案：关系、现状、性格、口头禅、喜好、雷区、专属回忆、纪念日、心境
- 分层记忆：最近 10 轮上下文 + 长期记忆（自动提取 + 后台压缩成「回忆」）
- 好感度与关系阶段（刚认识 → 熟络 → 好友 → 特别的人），影响语气
- 情境注入：时间 / 星期 / 天气 / 纪念日

**外观**
- 主题 / 动态取色 / 强调色；iMessage 气泡（连续圆角 + 泪滴尾巴）、圆角档位、气泡配色、字号缩发、字体
- 聊天背景：纯色或相册图片，可调模糊与暗度
- 触感分级、减少动画、长按菜单样式、应用图标伪装（计算器 / 备忘录）

**系统融合**
- 后台消息通知 + 通知栏快捷回复（RemoteInput）
- 导出聊天长图、ZIP 一键备份/恢复（含照片与录音）
- 深色模式适配、边到边布局

## 下载与安装

从 [Releases](https://github.com/) 下载最新的 `app-release.apk` 直接安装（Android 8.0+，arm64）。

> 未上架任何应用商店；安装时系统可能提示"未知来源"，属正常。

## 构建

需要 JDK 21、Android SDK 37。仓库不含 `local.properties`，本地新建一个：

```properties
sdk.dir=/path/to/android-sdk
```

```bash
./gradlew assembleDebug     # 调试包（带 Chucker 抓包）
./gradlew assembleRelease   # 正式包（需 keystore.properties，否则退化为 debug 签名）
```

发版流程见 [docs/RELEASING.md](docs/RELEASING.md)。

本项目在 arm64 Linux（proot）环境下开发，用到的编译参数见 `gradle.properties`。

## 技术栈

Kotlin 2.4.20 · Jetpack Compose（BOM 2026.09.00）· MIUIx 0.9.4 · OkHttp 5.5.0 ·
kotlinx.serialization · DataStore · AGP 9.2.1 / Gradle 9.4.1 · minSdk 26 / targetSdk 35

- 导航：`sealed interface Route` + 返回栈，配合 `SharedTransitionLayout` 共享元素
- 动效：全部弹簧参数集中在 `ui/motion/MotionTokens.kt`
- 存储：档案与记忆为本地文件，聊天记录 AES-256/GCM 加密（密钥由 Android Keystore 托管）
- 网络：debug 包集成 Chucker，可在应用内查看请求

## 隐私

- 不内置任何 API Key；用户填写的密钥只存本机 DataStore
- 照片、录音、聊天记录只在本机，不上传
- 使用公共端点时只有对话文本会离开设备，首次会提示
- 支持一键导出与彻底清除全部数据

## 许可

GPL-3.0。见 [LICENSE](LICENSE)。

架构参考了 [KernelSU](https://github.com/tiann/KernelSU) 的 Compose 组织方式（UI 框架与状态模式），
本项目已剥离其 Root / 模块 / 刷写能力，仅保留客户端界面与导航思路。
界面组件使用 [MIUIx](https://github.com/miuix-kotlin-multiplatform/miuix)。

## 致谢

- KernelSU Manager — 架构与交互参考
- MIUIx — Compose 组件
- GPT-SoVITS — 本地语音克隆桥接（见 `app/src/main/assets/bridge/`）
