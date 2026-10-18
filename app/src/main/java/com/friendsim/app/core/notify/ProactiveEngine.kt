package com.friendsim.app.core.notify

import android.content.Context
import com.friendsim.app.AppGraph
import com.friendsim.app.core.model.ChatMessage
import com.friendsim.app.core.model.Friend
import com.friendsim.app.core.model.Roles
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.UUID

/** 主动消息的核心逻辑：Receiver 和设置里的「测试」按钮共用。 */
object ProactiveEngine {

    data class Sent(val friend: Friend, val text: String)

    /** 设置里的「现在试一条」：忽略间隔与免打扰。 */
    fun fire(context: Context, ignoreGap: Boolean = true, ignoreQuiet: Boolean = true): com.friendsim.app.core.model.Friend? =
        maybeSend(context, force = true)?.friend

    fun maybeSend(context: Context, force: Boolean = false): Sent? {
        val settings = AppGraph.settings
        val store = AppGraph.friends
        val cfg = runBlocking { settings.snapshot() }
        if (!cfg.proactive && !force) return null

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (cfg.quietHours && !force && (hour >= 23 || hour < 8)) return null

        val candidates = store.friends.value.filter { friend ->
            if (force) return@filter true
            val last = store.messages(friend.id).lastOrNull { it.role != Roles.NOTICE }
            System.currentTimeMillis() - (last?.at ?: 0L) > 4 * 3600_000L
        }
        if (candidates.isEmpty()) return null

        val friend = candidates.random()
        val line = when {
            hour in 5..10 -> "早，醒了没"
            hour in 11..17 -> listOf("在忙吗", "吃饭了没", "刚看到一个东西想到你").random()
            hour in 18..22 -> "今天过得怎么样"
            else -> "还没睡"
        }
        runBlocking {
            store.append(friend.id, ChatMessage(UUID.randomUUID().toString(), Roles.FRIEND, line))
        }
        ChatNotifier.notifyMessage(context, friend.id, friend.name, line)
        return Sent(friend, line)
    }
}
