package com.friendsim.app.core.model

import kotlinx.serialization.Serializable

/** 朋友档案。仅本地保存：名字、照片、参考音频、性格、常用回复。 */
@Serializable
data class Friend(
    val id: String,
    var name: String = "",
    /** 照片（相对 files/friends/photos 的文件名）。 */
    var photo: String? = null,
    /** 声音参考音频（相对 files/friends/voice 的文件名）。 */
    var voice: String? = null,
    /** 性格 / 说话风格描述。 */
    var persona: String = "",
    /** 口头禅。 */
    var catchphrase: String = "",
    /** 常用回复：一行一条，`关键词|回复`，或直接一行回复。 */
    var scripts: String = "",
    /** 背景故事（灵魂档案）。 */
    var background: String = "",
    /** 它对你的称呼。 */
    var nickname: String = "",
    /** 心境：EXCITED / NORMAL / LOW。 */
    var mood: String = "NORMAL",
    /** 好感度 0..100。 */
    var affinity: Int = 0,
    /** 纪念日 MM-dd。 */
    var anniversary: String = "",
    /** 你们的关系（死党 / 室友…）。 */
    var relationship: String = "",
    /** TA 的现状。 */
    var status: String = "",
    /** 喜好。 */
    var likes: String = "",
    /** 雷区。 */
    var dislikes: String = "",
    /** 专属回忆。 */
    var sharedMemories: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    var lastActive: Long = 0L,
    /** 上次打开聊天的时间，用于未读计数。 */
    var lastReadAt: Long = 0L,
) {
    companion object {
        fun relationStage(affinity: Int): String = when {
            affinity < 20 -> "刚认识"
            affinity < 45 -> "熟络"
            affinity < 75 -> "好友"
            else -> "特别的人"
        }
    }
}

/** 一条对话消息。role: user / friend / notice。 */
@Serializable
data class ChatMessage(
    val id: String,
    val role: String,
    val text: String,
    val at: Long = System.currentTimeMillis(),
    /** 产生这条回复的引擎标签（仅留档，界面不显示）。 */
    val engine: String = "",
    /** text / voice / image */
    val kind: String = KIND_TEXT,
    /** 语音文件名（voice 目录内）。 */
    val media: String? = null,
    /** 语音时长（毫秒）。 */
    val durationMs: Long = 0L,
    /** 引用回复的对象。 */
    val quote: String? = null,
    /** 其他候选回复（左右滑切换）。 */
    val alternatives: List<String> = emptyList(),
) {
    companion object {
        const val KIND_TEXT = "text"
        const val KIND_VOICE = "voice"
        const val KIND_IMAGE = "image"
    }
}

object Roles {
    const val USER = "user"
    const val FRIEND = "friend"
    /** 技术性提示（不渲染）。 */
    const val NOTICE = "notice"
    /** 系统事件（拍一拍等，居中灰字渲染）。 */
    const val SYSTEM = "system"
}

/**
 * 导航路由（层级界面）。与 KernelSU manager 的 sealed Route / navigation3 NavKey 同构：
 * 一个 Route 即一个页面，返回栈由 [com.friendsim.app.ui.FsNavHost] 统一维护。
 */
sealed interface Route {
    /** 启动屏（纯 Compose） */
    data object Splash : Route

    /** 首次使用引导（3 页） */
    data object Onboarding : Route

    /** 一级：朋友列表 */
    data object Friends : Route

    /** 一级：设置 */
    data object Settings : Route

    /** 二级：编辑 / 新建朋友（null = 新建） */
    data class Edit(val friendId: String?) : Route

    /** 二级：聊天 */
    data class Chat(val friendId: String) : Route

    /** 二级：关于（从设置进入） */
    data object About : Route

    /** 二级：外观与美化 */
    data object Appearance : Route

    /** 二级：对话设置 */
    data object ChatSettings : Route

    /** 二级：语音设置 */
    data object VoiceSettings : Route

    /** 二级：隐私与数据 */
    data object PrivacyData : Route

    /** 二级：灵魂档案（性格 / 心境 / 记忆） */
    data class Soul(val friendId: String) : Route

    /** 三级：高级设置（Key / Base URL / 连通性验证） */
    data object Advanced : Route

    /** 记忆管理中心 */
    data object Memory : Route

    /** 运行诊断 */
    data object Diagnostics : Route
}
