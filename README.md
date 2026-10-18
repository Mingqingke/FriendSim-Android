# FriendSim

<p align="center">
  一款可以让你跟朋友对话的简易聊天工具
</p>

<p align="center">
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/badge/License-GPL--3.0-blue.svg"></a>
  <img alt="API" src="https://img.shields.io/badge/API-26%2B-brightgreen.svg">
  <a href="https://github.com/Mingqingke/FriendSim/actions"><img alt="Build" src="https://github.com/Mingqingke/FriendSim/actions/workflows/build.yml/badge.svg"></a>
</p>

---

## 它是什么

导入一张照片 一段声音，就能和一个「像 TA」的朋友继续聊下去。

不用注册，不用填 Key，断网也能回你话。

## 主要功能

**聊天**

- 流式回复，先显示「正在输入」，再逐字出现
- 语音条、图片消息、长消息折叠
- 引用回复 重新生成 从这里重来 复制 删除
- 发送失败点红点重发，不会丢消息

**聊得下去**

- 自备 Key（DeepSeek 等）优先，没有就自动走免 Key 公共端点，再不行切本地预设
- 全程静默降级，不在聊天里弹错误
- 断网只在输入框上方留一行小字

**像一个人**

- 关系 现状 性格 口头禅 喜好 雷区 专属回忆，都可编辑
- 分层记忆：最近上下文 + 长期记忆，聊久了会自动压缩成「回忆」
- 好感度与关系阶段，语气随相处时间变化
- 心境：兴奋 平静 低落，影响回复速度和长度
- 会看时间、天气、你们的纪念日

**外形**

- iMessage 气泡（连续圆角 + 泪滴尾巴），可切换普通气泡
- 圆角 字号 颜色 字体 可调；聊天背景支持相册图片，带模糊与暗度
- 应用图标可伪装成计算器或备忘录

**数据**

- 聊天记录本地加密保存
- 一键导出聊天长图
- 一键备份 恢复（含照片与录音）
- 一键清除全部数据

## 下载

到 [Releases](https://github.com/Mingqingke/FriendSim/releases) 下载 `app-release.apk`。

Android 8.0 以上，arm64。

## 构建

需要 JDK 21 与 Android SDK 37（`platforms;android-37`、`build-tools;36.0.0`）。根目录新建 `local.properties`：

```properties
sdk.dir=/path/to/android-sdk
```

```bash
./gradlew assembleDebug     # 调试包
./gradlew assembleRelease   # 正式包（需 keystore.properties）
```

## 许可

GPL-3.0，见 [LICENSE](LICENSE)。
