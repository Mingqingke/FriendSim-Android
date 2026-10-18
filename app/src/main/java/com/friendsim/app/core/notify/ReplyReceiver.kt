package com.friendsim.app.core.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput

/** 通知栏快捷回复：把文字暂存，等聊天页恢复时自动发出。 */
class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ChatNotifier.ACTION_REPLY) return
        val friendId = intent.getStringExtra(ChatNotifier.EXTRA_FRIEND_ID) ?: return
        val results = RemoteInput.getResultsFromIntent(intent) ?: return
        val text = results.getCharSequence(ChatNotifier.KEY_REPLY_TEXT)?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) {
            ChatNotifier.storePending(context, friendId, text)
            ChatNotifier.cancel(context, friendId)
        }
    }
}
