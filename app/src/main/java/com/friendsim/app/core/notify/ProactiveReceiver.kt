package com.friendsim.app.core.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.friendsim.app.AppGraph
import com.friendsim.app.core.model.ChatMessage
import com.friendsim.app.core.model.Roles
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.UUID

/**
 * 主动消息：每 3 小时醒一次，满足条件就让朋友真的发一条。
 * 条件：开关开、不在 23:00-08:00、这个朋友超过 4 小时没聊。
 */
class ProactiveReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TICK && intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ProactiveScheduler.ensureScheduled(context)
            return
        }

        val app = context.applicationContext as? android.app.Application ?: return
        runCatching { AppGraph.attach(app) }
        val settings = AppGraph.settings
        val store = AppGraph.friends

        val pending = goAsync()
        Thread {
            try {
                val cfg = runBlocking { settings.snapshot() }
                if (!cfg.proactive) return@Thread

                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (cfg.quietHours && (hour >= 23 || hour < 8)) return@Thread

                val candidates = store.friends.value.filter { friend ->
                    val last = store.messages(friend.id).lastOrNull { it.role != Roles.NOTICE }
                    val gap = System.currentTimeMillis() - (last?.at ?: 0L)
                    gap > 4 * 3600_000L
                }
                if (candidates.isEmpty()) return@Thread

                val friend = candidates.random()
                val line = when {
                    hour in 5..10 -> "早，醒了没"
                    hour in 11..17 -> listOf("在忙吗", "吃饭了没", "刚看到一个东西想到你").random()
                    hour in 18..22 -> "今天过得怎么样"
                    else -> "还没睡"
                }

                runBlocking {
                    store.append(
                        friend.id,
                        ChatMessage(UUID.randomUUID().toString(), Roles.FRIEND, line),
                    )
                }
                ChatNotifier.notifyMessage(context, friend.id, friend.name, line)
            } catch (_: Throwable) {
                // 后台任务不抛给系统
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_TICK = "com.friendsim.app.PROACTIVE_TICK"
    }
}
