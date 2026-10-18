package com.friendsim.app.core.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.friendsim.app.AppGraph
import kotlinx.coroutines.runBlocking

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

        val pending = goAsync()
        Thread {
            try {
                ProactiveEngine.maybeSend(context)
            } catch (_: Throwable) {
                // 后台不抛给系统
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_TICK = "com.friendsim.app.PROACTIVE_TICK"
    }
}
