package com.friendsim.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friendsim.app.AppGraph
import com.friendsim.app.core.data.MemoryItem
import com.friendsim.app.core.model.Friend
import com.friendsim.app.ui.components.CardBody
import com.friendsim.app.ui.components.FsInput
import com.friendsim.app.ui.components.FsPage
import com.friendsim.app.ui.components.FsSpacing
import com.friendsim.app.ui.components.section
import com.friendsim.app.ui.motion.bouncyClickable
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val moodOptions = listOf(
    "兴奋" to "EXCITED",
    "平静" to "NORMAL",
    "低落" to "LOW",
)

/** 二级页：灵魂档案（性格 / 心境 / 纪念日 / 关系阶段 / 记忆）。 */
@Composable
fun SoulScreen(friendId: String, onBack: () -> Unit, notify: (String) -> Unit) {
    val store = AppGraph.friends
    val memory = AppGraph.memory
    val scope = rememberCoroutineScope()
    val friends by store.friends.collectAsState()
    val friend = friends.firstOrNull { it.id == friendId }

    if (friend == null) {
        FsPage(title = "灵魂档案", onBack = onBack) {
            item(key = "gone") { CardBody { Text("档案已删除", fontSize = 14.sp) } }
        }
        return
    }

    var name by remember(friendId) { mutableStateOf(friend.name) }
    var nickname by remember(friendId) { mutableStateOf(friend.nickname) }
    var persona by remember(friendId) { mutableStateOf(friend.persona) }
    var catchphrase by remember(friendId) { mutableStateOf(friend.catchphrase) }
    var background by remember(friendId) { mutableStateOf(friend.background) }
    var anniversary by remember(friendId) { mutableStateOf(friend.anniversary) }
    var newMemory by remember { mutableStateOf("") }
    var items by remember(friendId) { mutableStateOf(memory.list(friendId)) }

    fun save(patch: (Friend) -> Unit) {
        scope.launch {
            val latest = store.find(friendId) ?: return@launch
            patch(latest)
            store.upsert(latest)
        }
    }

    FsPage(title = "灵魂档案", subtitle = friend.name.ifBlank { "朋友" }, onBack = onBack) {
        section(title = "关系", key = "relation") {
            CardBody {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Friend.relationStage(friend.affinity),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "亲密度 ${friend.affinity}/100",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(friend.affinity / 100f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MiuixTheme.colorScheme.primary),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "每聊一轮 +1，聊得越多越亲近。",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            HorizontalDivider()
            CardBody {
                Text(text = "此刻的心境", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    moodOptions.forEach { (label, value) ->
                        val active = friend.mood == value
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (active) MiuixTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                )
                                .bouncyClickable { save { it.mood = value } }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                color = if (active) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        section(title = "档案", key = "profile") {
            CardBody {
                FsInput(value = name, onValueChange = { name = it }, label = "名字", placeholder = "阿杰")
                Spacer(Modifier.height(FsSpacing.Item))
                FsInput(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = "它对你的称呼",
                    placeholder = "小笨蛋",
                )
                Spacer(Modifier.height(FsSpacing.Item))
                FsInput(
                    value = persona,
                    onValueChange = { persona = it },
                    label = "性格与说话方式",
                    placeholder = "话不多，但很会安慰人",
                    singleLine = false,
                    minHeight = 64.dp,
                )
                Spacer(Modifier.height(FsSpacing.Item))
                FsInput(
                    value = catchphrase,
                    onValueChange = { catchphrase = it },
                    label = "口头禅",
                    placeholder = "就那样吧",
                )
                Spacer(Modifier.height(FsSpacing.Item))
                FsInput(
                    value = background,
                    onValueChange = { background = it },
                    label = "背景",
                    placeholder = "在便利店上夜班，喜欢拍照",
                    singleLine = false,
                    minHeight = 64.dp,
                )
                Spacer(Modifier.height(FsSpacing.Item))
                FsInput(
                    value = anniversary,
                    onValueChange = { anniversary = it },
                    label = "纪念日（MM-dd）",
                    placeholder = "09-12",
                )
                Spacer(Modifier.height(FsSpacing.Item))
                Button(
                    onClick = {
                        val oldName = friend.name
                        save {
                            it.name = name.trim().ifBlank { it.name }
                            it.nickname = nickname.trim()
                            it.persona = persona.trim()
                            it.catchphrase = catchphrase.trim()
                            it.background = background.trim()
                            it.anniversary = anniversary.trim()
                        }
                        if (oldName.isNotBlank() && oldName != name.trim()) {
                            scope.launch { AppGraph.memory.replaceName(friendId, oldName, name.trim()) }
                        }
                        notify("已保存")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "保存")
                }
            }
        }

        section(title = "记忆", key = "memory") {
            CardBody {
                if (items.isEmpty()) {
                    Text(
                        text = "还没有记忆。聊天时会自动记下你的喜好和近况。",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                } else {
                    items.forEach { item ->
                        MemoryRow(item) {
                            scope.launch {
                                memory.remove(friendId, item.id)
                                items = memory.list(friendId)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                FsInput(
                    value = newMemory,
                    onValueChange = { newMemory = it },
                    placeholder = "手动记一条，比如：怕冷、爱喝冰美式",
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val text = newMemory.trim()
                            if (text.isNotEmpty()) {
                                scope.launch {
                                    memory.add(friendId, text)
                                    newMemory = ""
                                    items = memory.list(friendId)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = "添加记忆")
                    }
                    if (items.isNotEmpty()) {
                        Button(
                            onClick = {
                                scope.launch {
                                    memory.clear(friendId)
                                    items = emptyList()
                                    notify("记忆已清空")
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "清空", color = MiuixTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryRow(item: MemoryItem, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.text,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .bouncyClickable(scaleDown = 0.94f, onClick = onDelete)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(text = "删除", fontSize = 12.sp, color = MiuixTheme.colorScheme.error)
        }
    }
}
