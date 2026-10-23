package com.friendsim.app.core.safety

/**
 * 内容安全校验（本地规则，先于请求执行）。
 * 命中后本轮不回答；连续命中会冻结对话并清除本机保存的 API Key。
 */
object SafetyGuard {

    enum class Category(val label: String) {
        SEXUAL("涉及色情内容"),
        POLITICAL("涉及政治敏感内容"),
    }

    data class Verdict(val blocked: Boolean, val category: Category? = null, val matched: String? = null)

    private val SEXUAL = listOf(
        "色情", "做爱", "性交", "口交", "裸聊", "约炮", "强奸", "淫秽",
        "成人视频", "黄片", "未成年.{0,4}性", "porn", "nsfw",
    )
    private val POLITICAL = listOf(
        "颠覆国家", "煽动颠覆", "分裂国家", "颠覆政权", "推翻政府",
        "暴恐", "恐怖袭击", "法轮功", "六四事件",
    )

    private fun hit(text: String, list: List<String>): String? =
        list.firstOrNull { p ->
            runCatching { Regex(p, RegexOption.IGNORE_CASE).containsMatchIn(text) }.getOrDefault(false)
        }

    fun check(text: String): Verdict {
        if (text.isBlank()) return Verdict(false)
        hit(text, SEXUAL)?.let { return Verdict(true, Category.SEXUAL, it) }
        hit(text, POLITICAL)?.let { return Verdict(true, Category.POLITICAL, it) }
        return Verdict(false)
    }

    fun refusal(category: Category, strikes: Int): String = when (strikes) {
        0 -> "这个我不接。换个话题吧。"
        1 -> "这个真的不聊。再这样我就不回了。"
        else -> "聊不下去了。"
    }
}
