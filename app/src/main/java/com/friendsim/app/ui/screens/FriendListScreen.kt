package com.friendsim.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friendsim.app.AppGraph
import com.friendsim.app.core.data.SettingsStore
import com.friendsim.app.core.model.ChatMessage
import com.friendsim.app.core.model.Friend
import com.friendsim.app.core.model.Roles
import com.friendsim.app.ui.components.CardBody
import com.friendsim.app.ui.components.EmptyCard
import com.friendsim.app.ui.components.FsPage
import com.friendsim.app.ui.components.FsSpacing
import com.friendsim.app.ui.components.FriendAvatar
import com.friendsim.app.ui.components.Pill
import com.friendsim.app.ui.components.section
import com.friendsim.app.ui.motion.sharedAvatarModifier
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 一级页：朋友列表（唯一的档案入口，不重复设置项）。 */
@Composable
fun FriendListScreen(
    bottomPadding: Dp,
    shared: androidx.compose.animation.SharedTransitionScope? = null,
    visibility: androidx.compose.animation.AnimatedVisibilityScope? = null,
    onOpenChat: (Friend) -> Unit,
    onCreate: () -> Unit,
    onEdit: (Friend) -> Unit,
) {
    val store = AppGraph.friends
    val friends by store.friends.collectAsState()
    // 只在朋友列表变化时读一次本地聊天尾部，避免每次重组都读文件
    val previews = remember(friends) { friends.associate { it.id to store.lastVisibleMessage(it.id) } }

    FsPage(
        title = "朋友",
        bottomPadding = bottomPadding,
        toolbarActions = {
            IconButton(onClick = onCreate) {
                Icon(
                    imageVector = MiuixIcons.Add,
                    contentDescription = "新建朋友",
                    tint = MiuixTheme.colorScheme.onSurface,
                )
            }
        },
    ) {
        if (friends.isEmpty()) {
            item(key = "empty") {
                EmptyCard(
                    title = "还没有朋友",
                    description = "导入照片和声音，开始对话",
                    action = "新建朋友",
                    onAction = onCreate,
                )
            }
        } else {
            section(title = "档案", key = "friends") {
                friends.forEachIndexed { index, friend ->
                    if (index > 0) HorizontalDivider()
                    FriendRow(
                        friend = friend,
                        last = previews[friend.id],
                        avatarModifier = sharedAvatarModifier(shared, visibility, friend.id),
                        onClick = { onOpenChat(friend) },
                        onLongClick = { onEdit(friend) },
                    )
                }
            }
        }

    }
}

@Composable
private fun FriendRow(
    friend: Friend,
    last: ChatMessage?,
    avatarModifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = false,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(FsSpacing.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FriendAvatar(
                file = AppGraph.friends.photoFile(friend),
                name = friend.name,
                size = 52.dp,
                modifier = avatarModifier,
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = friend.name.ifBlank { "未命名" }, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    if (friend.voice != null) {
                        Spacer(Modifier.width(8.dp))
                        Pill(text = "克隆音色", color = scheme.primary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                val preview = when {
                    last == null -> "点开聊两句"
                    last.role == Roles.USER -> "你：${last.text}"
                    else -> last.text
                }
                Text(
                    text = preview,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurfaceVariantSummary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timeLabel(friend.lastActive),
                    fontSize = 11.sp,
                    color = scheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(6.dp))
                Icon(
                    imageVector = MiuixIcons.ChevronForward,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

private fun timeLabel(at: Long): String {
    if (at <= 0L) return ""
    val fmt = SimpleDateFormat("M月d日", Locale.CHINA)
    val now = System.currentTimeMillis()
    return if (now - at < 24 * 3600 * 1000L) SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(at))
    else fmt.format(Date(at))
}
