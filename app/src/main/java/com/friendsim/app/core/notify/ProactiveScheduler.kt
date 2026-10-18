package com.friendsim.app.core.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * 真实的主动消息：每 3 小时醒一次，条件都满足才发一条。
 * 条件（全部在 Receiver 里二次确认）：主动消息开关开、不在深夜免打扰、
 * 这个朋友确实隔了很久没聊。
 */
object ProactiveScheduler {

    private const val REQUEST_CODE = 4242
    private const val INTERVAL_MS = 3 * 60 * 60 * 1000L

    fun ensureScheduled(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context) ?: return
        val triggerAt = System.currentTimeMillis() + 30 * 60 * 1000L
        runCatching {
            manager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                INTERVAL_MS,
                pending,
            )
        }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        pendingIntent(context)?.let { runCatching { manager.cancel(it) } }
    }

    private fun pendingIntent(context: Context): PendingIntent? {
        val intent = Intent(context, ProactiveReceiver::class.java).apply {
            action = ProactiveReceiver.ACTION_TICK
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0,
        )
    }
}
