package com.friendsim.app.core.llm

import java.util.concurrent.ConcurrentHashMap

/** 一个可用的对话端点。 */
data class EndpointCandidate(
    val id: String,
    val label: String,
    val baseUrl: String,
    val model: String,
    val apiKey: String? = null,
)

/**
 * Level 2：免 Key 公共端点池。
 * 状态机：普通失败冷却 60 秒；连续 3 次失败标记不可用、5 分钟后重新探测；
 * Pollinations 额外限制两次请求间隔 ≥ 15 秒（不够就跳到下一个端点，不阻塞用户）。
 */
object EndpointPool {

    private const val COOLDOWN_MS = 60_000L
    private const val HARD_COOLDOWN_MS = 5 * 60_000L
    private const val POLLINATIONS_INTERVAL_MS = 15_000L

    private class State {
        var fails: Int = 0
        var downUntil: Long = 0L
        var lastUsed: Long = 0L
        var lastError: String = ""
    }

    private val states = ConcurrentHashMap<String, State>()

    /** 能看图的端点（DeepSeek chat 不支持图片，所以图片走这里）。 */
    val vision: List<EndpointCandidate> = listOf(
        EndpointCandidate(
            id = "ovh-vl",
            label = "视觉端点",
            baseUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1",
            model = "Qwen2.5-VL-72B-Instruct",
        ),
        EndpointCandidate(
            id = "pollinations-vl",
            label = "视觉端点",
            baseUrl = "https://text.pollinations.ai/openai",
            model = "openai",
        ),
    )

    private fun state(id: String) = states.getOrPut(id) { State() }

    val public: List<EndpointCandidate> = listOf(
        EndpointCandidate(
            id = "keylessai",
            label = "KeylessAI",
            baseUrl = "https://keylessai.thryx.workers.dev/v1",
            model = "gpt-4o-mini",
        ),
        EndpointCandidate(
            id = "uncloseai",
            label = "UncloseAI",
            baseUrl = "https://api.uncloseai.com/v1",
            model = "gpt-4o-mini",
        ),
        EndpointCandidate(
            id = "pollinations",
            label = "Pollinations",
            baseUrl = "https://text.pollinations.ai/openai",
            model = "gpt-oss-20b",
        ),
        EndpointCandidate(
            id = "ovh",
            label = "OVHcloud",
            baseUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1",
            model = "Meta-Llama-3_3-70B-Instruct",
        ),
    )

    fun isAvailable(id: String): Boolean = state(id).downUntil <= System.currentTimeMillis()

    /** 距离下次可用还需等待的时间（毫秒），0 表示立即可用。 */
    fun waitMs(candidate: EndpointCandidate): Long {
        val s = state(candidate.id)
        val now = System.currentTimeMillis()
        if (s.downUntil > now) return s.downUntil - now
        if (candidate.id == "pollinations") {
            val remain = POLLINATIONS_INTERVAL_MS - (now - s.lastUsed)
            return remain.coerceAtLeast(0L)
        }
        return 0L
    }

    fun onSuccess(candidate: EndpointCandidate) {
        val s = state(candidate.id)
        s.fails = 0
        s.downUntil = 0L
        s.lastUsed = System.currentTimeMillis()
    }

    fun onFailure(candidate: EndpointCandidate, reason: String) {
        val s = state(candidate.id)
        s.fails += 1
        s.lastError = reason
        s.lastUsed = System.currentTimeMillis()
        s.downUntil = System.currentTimeMillis() +
            if (s.fails >= 3) HARD_COOLDOWN_MS else COOLDOWN_MS
    }

    fun available(): List<EndpointCandidate> = public.filter { isAvailable(it.id) }

    fun statusText(): String {
        val now = System.currentTimeMillis()
        val ok = public.count { state(it.id).downUntil <= now }
        return "$ok/${public.size} 可用"
    }

    fun lastError(id: String): String = state(id).lastError
}
