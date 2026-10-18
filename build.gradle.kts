// FriendSim — 零配置可聊天的朋友对话模拟器（基于 KernelSU Manager 的 Compose 架构）
// 顶层构建脚本：仅声明插件版本，模块级配置见 app/build.gradle.kts
plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.android") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20" apply false
}
