package com.friendsim.app.core.llm

import com.friendsim.app.core.data.EngineConfig
import com.friendsim.app.core.model.ChatMessage
import com.friendsim.app.core.model.Friend
import com.friendsim.app.core.model.Roles
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/** 对话过程中抛出的事件。 */
sealed interface ChatEvent {
    /** 当前使用的引擎（level 1/2/3）。 */
    data class Engine(val level: Int, val label: String) : ChatEvent

    /** 增量文本。 */
    data class Delta(val text: String) : ChatEvent

    /** 降级、失败等提示（作为 notice 消息落库，用户可见但不弹窗）。 */
    data class Notice(val text: String) : ChatEvent

    data object Done : ChatEvent
}

/**
 * 对话调度：Level 1 自备 Key → Level 2 免 Key 公共端点 → Level 3 离线静默兜底。
 * 任何一层失败都自动往下走，永远不会出现“发出去没回应”。
 */
class ChatEngine(private val client: OpenAiClient = OpenAiClient()) {

    /** 记忆压缩用：把一段对话压成一句回忆。失败返回 null，由本地兜底。 */
    suspend fun summarize(transcript: String, config: EngineConfig): String? {
        val candidate = if (config.hasKey && config.keyValid) {
            EndpointCandidate("user-key", "DeepSeek", config.baseUrl, config.model, config.apiKey)
        } else {
            EndpointPool.public.firstOrNull { EndpointPool.isAvailable(it.id) }
        } ?: return null
        val builder = StringBuilder()
        runCatching {
            client.stream(
                baseUrl = candidate.baseUrl,
                apiKey = candidate.apiKey,
                model = candidate.model,
                systemPrompt = MemoryCompactor.summaryPrompt(transcript),
                history = emptyList(),
                userText = "总结",
                maxTokens = 120,
            ).collect { delta -> builder.append(delta) }
        }
        return builder.toString().trim().takeIf { it.isNotBlank() }?.take(80)
    }

    /**
     * 导入聊天记录 → 提炼人物档案。
     * 只返回模型原始文本（要求是 JSON），解析交给调用方，失败就当没分析出来。
     */
    suspend fun analyzePersona(raw: String, config: EngineConfig): String? {
        val candidate = if (config.hasKey && config.keyValid) {
            EndpointCandidate("user-key", "DeepSeek", config.baseUrl, config.model, config.apiKey)
        } else {
            EndpointPool.public.firstOrNull { EndpointPool.isAvailable(it.id) }
        } ?: return null
        val builder = StringBuilder()
        runCatching {
            client.stream(
                baseUrl = candidate.baseUrl,
                apiKey = candidate.apiKey,
                model = candidate.model,
                systemPrompt = PersonaPrompt.ANALYZE_PROMPT,
                history = emptyList(),
                userText = raw.take(6000),
                maxTokens = 700,
            ).collect { delta -> builder.append(delta) }
        }
        return builder.toString().trim().takeIf { it.isNotBlank() }
    }

    fun respond(
        friend: Friend,
        history: List<ChatMessage>,
        userText: String,
        config: EngineConfig,
        memories: List<String> = emptyList(),
        imageDataUrl: String? = null,
    ): Flow<ChatEvent> = channelFlow {
        val context = runCatching { ContextFacts.build(friend, config.weatherOn) }.getOrDefault("")
        val system = PersonaPrompt.build(friend, config.userName, memories, context)
        // 每个朋友上下文以 friendId 隔离：历史只取本人最近 10 轮
        val wire = history
            .filter { it.role != Roles.NOTICE }
            .takeLast(20)
            .map { m -> (if (m.role == Roles.USER) "user" else "assistant") to m.text }

        suspend fun attempt(candidate: EndpointCandidate, level: Int, image: String? = null): Boolean {
            send(ChatEvent.Engine(level, if (level == 1) "DeepSeek" else "公共端点·${candidate.label}"))
            var produced = false
            try {
                client.stream(
                    baseUrl = candidate.baseUrl,
                    apiKey = candidate.apiKey,
                    model = candidate.model,
                    systemPrompt = system,
                    history = wire,
                    userText = userText,
                    imageDataUrl = image,
                ).collect { delta ->
                    produced = true
                    send(ChatEvent.Delta(delta))
                }
            } catch (t: Throwable) {
                if (!produced) {
                    val reason = t.message?.take(60)?.ifBlank { t::class.simpleName.orEmpty() } ?: "未知错误"
                    send(ChatEvent.Notice("${candidate.label} 不可用，换下一个"))
                    if (candidate.apiKey == null) EndpointPool.onFailure(candidate, reason)
                }
            }
            if (produced) {
                if (candidate.apiKey == null) EndpointPool.onSuccess(candidate)
                return true
            }
            return false
        }

        // —— 图片优先走视觉端点 ——
        if (imageDataUrl != null) {
            for (candidate in EndpointPool.vision) {
                if (!EndpointPool.isAvailable(candidate.id)) continue
                if (attempt(candidate, 2, imageDataUrl)) {
                    send(ChatEvent.Done)
                    return@channelFlow
                }
            }
            send(ChatEvent.Engine(3, "离线预设"))
            send(ChatEvent.Delta("图我收到了 但这边暂时看不清 你直接说给我听吧"))
            send(ChatEvent.Done)
            return@channelFlow
        }

        // —— Level 1（可选增强）——
        if (!config.offlineOnly && config.hasKey && config.keyValid) {
            val custom = EndpointCandidate(
                id = "user-key",
                label = "DeepSeek",
                baseUrl = config.baseUrl,
                model = config.model,
                apiKey = config.apiKey,
            )
            if (attempt(custom, 1)) {
                send(ChatEvent.Done)
                return@channelFlow
            }
        }

        // —— Level 2（默认路径）——
        if (!config.offlineOnly && config.allowPublic) {
            for (candidate in EndpointPool.public) {
                if (!EndpointPool.isAvailable(candidate.id)) continue
                val wait = EndpointPool.waitMs(candidate)
                if (wait > 8_000L) continue // 节流中，直接跳下一个，不让用户干等
                if (wait > 0L) delay(wait)
                if (attempt(candidate, 2)) {
                    send(ChatEvent.Done)
                    return@channelFlow
                }
            }
        }

        // —— Level 3（静默兜底）——
        send(ChatEvent.Engine(3, "离线预设"))
        val reply = OfflineBrain.reply(friend, userText, config.userName)
        delay(380)
        send(ChatEvent.Delta(reply))
        send(ChatEvent.Done)
    }
}
