package com.friendsim.app.core.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.friendsim.app.MainActivity
import com.friendsim.app.R

/**
 * 系统通知 + 通知栏快捷回复（RemoteInput）。
 * 回复文本由 [ReplyReceiver] 收下，暂存到 SharedPreferences，
 * 聊天页恢复时读取并自动发出。
 */
object ChatNotifier {

    const val CHANNEL_ID = "friend_messages"
    const val ACTION_REPLY = "com.friendsim.app.action.REPLY"
    const val EXTRA_FRIEND_ID = "friend_id"
    const val KEY_REPLY_TEXT = "reply_text"
    private const val PREFS = "pending_replies"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "朋友消息", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "朋友发来的消息"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
        )
    }

    fun notifyMessage(context: Context, friendId: String, friendName: String, text: String) {
        ensureChannel(context)
        val openIntent = PendingIntent.getActivity(
            context,
            friendId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_FRIEND_ID, friendId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val remoteInput = RemoteInput.Builder(KEY_REPLY_TEXT)
            .setLabel("回复")
            .build()

        val replyIntent = PendingIntent.getBroadcast(
            context,
            friendId.hashCode() + 1,
            Intent(context, ReplyReceiver::class.java).apply {
                action = ACTION_REPLY
                putExtra(EXTRA_FRIEND_ID, friendId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        val replyAction = NotificationCompat.Action.Builder(0, "回复", replyIntent)
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        val person = androidx.core.app.Person.Builder()
            .setName(friendName.ifBlank { "朋友" })
            .build()
        val style = NotificationCompat.MessagingStyle(
            androidx.core.app.Person.Builder().setName("我").build(),
        )
            .setConversationTitle(friendName.ifBlank { "朋友" })
            .addMessage(text, System.currentTimeMillis(), person)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(friendName.ifBlank { "朋友" })
            .setContentText(text)
            .setStyle(style)
            .setContentIntent(openIntent)
            .apply {
                val file = runCatching {
                    com.friendsim.app.AppGraph.friends.photoFile(friendId)
                }.getOrNull()
                file?.let { f ->
                    runCatching {
                        android.graphics.BitmapFactory.decodeFile(f.absolutePath)?.let { setLargeIcon(it) }
                    }
                }
            }
            .addAction(replyAction)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setOnlyAlertOnce(false)
            .setGroup(CHANNEL_ID)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(friendId.hashCode(), notification)
        }
    }

    fun cancel(context: Context, friendId: String) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(friendId.hashCode())
        }
    }

    /** 取出并清空通知栏回复。 */
    fun consumePending(context: Context, friendId: String): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val text = prefs.getString(friendId, null)
        if (!text.isNullOrBlank()) {
            prefs.edit().remove(friendId).apply()
            return text
        }
        return null
    }

    internal fun storePending(context: Context, friendId: String, text: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(friendId, text)
            .apply()
    }
}
