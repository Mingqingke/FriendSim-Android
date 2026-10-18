package com.friendsim.app.core.llm

import com.friendsim.app.core.data.MemoryItem
import com.friendsim.app.core.data.MemoryStore
import com.friendsim.app.core.model.ChatMessage
import com.friendsim.app.core.model.Roles

/**
 * 后台记忆压缩：聊到一定长度后，把最早的一批对话压成一条「回忆」。
 * 优先用模型总结（更自然）；没有可用端点时退化为本地摘要，保证永远有结果。
 */
object MemoryCompactor {

    private const val TRIGGER_AFTER = 30
    private const val STEP = 12
    private const val SUMMARY_BATCH = 20

    suspend fun maybeCompact(
        friendId: String,
        messages: List<ChatMessage>,
        store: MemoryStore,
        summarize: suspend (String) -> String?,
    ) {
        if (messages.size < TRIGGER_AFTER) return
        val compacted = store.compactedCount(friendId)
        if (messages.size - compacted < STEP) return

        val batch = messages
            .filter { it.role != Roles.NOTICE && it.role != Roles.SYSTEM }
            .take(SUMMARY_BATCH)
        if (batch.isEmpty()) return

        val transcript = batch.joinToString("\n") { m ->
            (if (m.role == Roles.USER) "我" else "对方") + "：" + m.text.take(60)
        }

        val summary = summarize(transcript)?.takeIf { it.isNotBlank() } ?: localSummary(batch)
        store.add(friendId, summary, MemoryItem.KIND_EPISODE)
        store.markCompacted(friendId, messages.size)
    }

    /** 本地兜底：挑出带信息量的几句话。 */
    private fun localSummary(batch: List<ChatMessage>): String {
        val keys = listOf("喜欢", "不喜欢", "累", "忙", "考试", "工作", "吃", "睡", "生日", "名字", "讨厌", "开心")
        val picks = batch
            .filter { it.role == Roles.USER }
            .filter { line -> keys.any { line.text.contains(it) } }
            .map { it.text.take(24) }
            .distinct()
            .take(3)
        return if (picks.isEmpty()) {
            "回忆：" + batch.lastOrNull()?.text?.take(30).orEmpty()
        } else {
            "回忆：" + picks.joinToString("；")
        }
    }

    fun summaryPrompt(transcript: String): String = """
        把下面这段聊天压缩成一句不超过 40 字的中文回忆，只保留能影响之后聊天的事实（心情、计划、喜好、重要事件）。
        用第三人称陈述，不要引号、不要解释。
        聊天记录：
        $transcript
    """.trimIndent()
}
