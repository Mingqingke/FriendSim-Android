package com.friendsim.app.core.llm

import com.friendsim.app.core.model.Friend
import kotlin.random.Random

/**
 * Level 3：完全离线的预设回复引擎（兜底，永远可用）。
 * 不是“假装 AI 还在”——它只做关键词 / 意图匹配，用朋友自己的常用语和通用话术回话，
 * 目的是零配置、断网时依然能玩、能听见克隆的声音。
 */
object OfflineBrain {

    private data class Rule(val keys: List<String>, val replies: List<String>)

    private val rules: List<Rule> = listOf(
        Rule(
            keys = listOf("你好", "在吗", "在么", "嗨", "早", "早安", "hi", "hello", "哈喽"),
            replies = listOf("在的，怎么啦？", "嗯？我在。", "刚看到，说吧。", "哟，{name}，今天怎么想起我了。"),
        ),
        Rule(
            keys = listOf("晚安", "睡了", "困了", "睡觉"),
            replies = listOf("睡吧，别熬太晚。", "晚安，明天再聊。", "早点睡，明天见。"),
        ),
        Rule(
            keys = listOf("干嘛", "做什么", "在干什么", "忙吗", "干啥"),
            replies = listOf("没干嘛，躺着呢。", "刚忙完，现在有空。", "在发呆，你说。"),
        ),
        Rule(
            keys = listOf("累", "烦", "难受", "难过", "哭", "焦虑", "压力", "不开心", "崩", "emo"),
            replies = listOf(
                "怎么啦？慢慢说，我在听。",
                "先别硬撑，歇一会儿吧。",
                "要不先喝口水，缓一缓再说。",
                "听着就挺难受的，辛苦你了。",
            ),
        ),
        Rule(
            keys = listOf("开心", "好耶", "哈哈", "爽", "兴奋", "太好了", "成功了"),
            replies = listOf("哈哈，那就好！", "看出来了，声音都亮。", "行啊{name}，替你高兴。", "可以可以，值得庆祝。"),
        ),
        Rule(
            keys = listOf("吃", "饭", "饿", "外卖", "夜宵", "奶茶"),
            replies = listOf("吃了吗？别又不吃。", "饿了就去吃点，别硬扛。", "泡面也算一顿，别天天这样。"),
        ),
        Rule(
            keys = listOf("加班", "上班", "工作", "课", "作业", "考试", "上班族", "老板"),
            replies = listOf("今天还没完事？", "别把自己逼太紧。", "先把要紧的弄完，剩下的明天再说。"),
        ),
        Rule(
            keys = listOf("谢谢", "谢啦", "感谢", "多谢"),
            replies = listOf("跟我还客气。", "小事。", "行啦，别谢了。"),
        ),
        Rule(
            keys = listOf("想你", "喜欢", "爱你", "抱抱"),
            replies = listOf("突然这么肉麻。", "嗯，我在呢。", "行了行了，肉麻。", "抱一个。"),
        ),
        Rule(
            keys = listOf("再见", "拜拜", "走了", "先忙", "回头聊"),
            replies = listOf("行，去忙吧。", "拜，有事随时说。", "好，回头聊。"),
        ),
        Rule(
            keys = listOf("你是谁", "真的吗", "为什么", "怎么", "是吗", "?"),
            replies = listOf("嗯，我想想。", "这个我还真说不好。", "你先说说你怎么想的。", "大概是吧。"),
        ),
    )

    private val fallback = listOf(
        "嗯嗯，我在听。",
        "然后呢？",
        "有点意思，继续说。",
        "我懂你意思。",
        "这个我得想想。",
        "你说的我记着了。",
        "要不先这样，回头细聊。",
    )

    /** 解析朋友的常用回复：`关键词1,关键词2|回复` 或直接 `回复`。 */
    fun parseScripts(raw: String): List<Pair<List<String>, String>> =
        raw.lines().map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { line ->
            val idx = line.indexOf('|')
            if (idx <= 0) {
                // 没有关键词：任何输入都可能命中（低优先级）
                listOf(emptyList<String>() to line)
            } else {
                val keys = line.substring(0, idx).split(',', '，', ' ', '/')
                    .map { it.trim() }.filter { it.isNotEmpty() }
                val reply = line.substring(idx + 1).trim()
                if (reply.isEmpty()) emptyList() else listOf(keys to reply)
            }
        }.flatten()

    fun reply(friend: Friend, input: String, userName: String): String {
        val text = input.trim()
        val personal = parseScripts(friend.scripts)

        // 1) 朋友自己的常用回复优先（关键词命中数最高者）
        val personalHit = personal
            .map { (keys, reply) -> keys.count { it.isNotEmpty() && text.contains(it, true) } to reply }
            .filter { it.first > 0 }
            .maxByOrNull { it.first }

        val chosen = personalHit?.second
            ?: run {
                // 2) 内置意图规则
                val best = rules
                    .map { rule -> rule.keys.count { text.contains(it, true) } to rule }
                    .filter { it.first > 0 }
                    .maxByOrNull { it.first }
                    ?.second
                best?.let { it.replies.random() }
            }
            ?: personal.filter { it.first.isEmpty() }.map { it.second }.randomOrNull()
            ?: fallback.random()

        return decorate(chosen, friend, userName)
    }

    /** 占位符替换 + 偶尔带上口头禅。 */
    private fun decorate(raw: String, friend: Friend, userName: String): String {
        var out = raw
            .replace("{name}", userName.ifBlank { "你" })
            .replace("{friend}", friend.name.ifBlank { "我" })
        if (friend.catchphrase.isNotBlank() && out.length < 24 && Random.nextInt(100) < 35) {
            out = out.trimEnd('。', '！', '…') + "，" + friend.catchphrase.take(10)
        }
        return out
    }
}
