package com.friendsim.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friendsim.app.AppGraph
import com.friendsim.app.core.data.FriendStore
import com.friendsim.app.core.model.Friend
import com.friendsim.app.core.voice.AudioProcessor
import com.friendsim.app.core.voice.Recorder
import com.friendsim.app.ui.components.CardBody
import com.friendsim.app.ui.components.ConfirmDialog
import com.friendsim.app.ui.components.FsInput
import com.friendsim.app.ui.components.FsPage
import com.friendsim.app.ui.components.FsSpacing
import com.friendsim.app.ui.components.FriendAvatar
import com.friendsim.app.ui.components.section
import com.friendsim.app.ui.motion.DissolveEffect
import com.friendsim.app.ui.motion.MotionTokens
import com.friendsim.app.ui.motion.bouncyClickable
import kotlinx.coroutines.launch
import java.io.File
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.UUID
import androidx.compose.foundation.text.KeyboardOptions

/** 二级页：新建 / 编辑朋友档案。 */
@Composable
fun FriendEditScreen(
    friendId: String?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    notify: (String) -> Unit,
) {
    val store = AppGraph.friends
    val scope = rememberCoroutineScope()
    val existing = remember(friendId) { friendId?.let { store.find(it) } }
    val id = remember(friendId) { existing?.id ?: UUID.randomUUID().toString() }

    var name by rememberSaveable(friendId) { mutableStateOf(existing?.name ?: "") }
    var persona by rememberSaveable(friendId) { mutableStateOf(existing?.persona ?: "") }
    var catchphrase by rememberSaveable(friendId) { mutableStateOf(existing?.catchphrase ?: "") }
    var scripts by rememberSaveable(friendId) { mutableStateOf(existing?.scripts ?: "") }
    var nickname by rememberSaveable(friendId) { mutableStateOf(existing?.nickname ?: "") }
    var relationship by rememberSaveable(friendId) { mutableStateOf(existing?.relationship ?: "") }
    var status by rememberSaveable(friendId) { mutableStateOf(existing?.status ?: "") }
    var likes by rememberSaveable(friendId) { mutableStateOf(existing?.likes ?: "") }
    var dislikes by rememberSaveable(friendId) { mutableStateOf(existing?.dislikes ?: "") }
    var sharedMemories by rememberSaveable(friendId) { mutableStateOf(existing?.sharedMemories ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var photoName by rememberSaveable(friendId) { mutableStateOf(existing?.photo ?: "") }
    var voiceName by rememberSaveable(friendId) { mutableStateOf(existing?.voice ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    val recorder = remember { Recorder(AppGraph.app) }
    var recording by remember { mutableStateOf(false) }
    val recordFile = remember { File(AppGraph.app.cacheDir, "ref_voice.wav") }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            recording = runCatching { recorder.start(scope, recordFile) }.getOrDefault(false)
        } else {
            notify("需要麦克风权限")
        }
    }

    fun toggleRecord() {
        if (!recording) {
            if (!recorder.hasPermission()) {
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                recording = runCatching { recorder.start(scope, recordFile) }.getOrDefault(false)
                if (!recording) notify("无法开始录音")
            }
        } else {
            scope.launch {
                runCatching { recorder.stop() }
                val duration = runCatching { AudioProcessor.trimAndGate(recordFile) }.getOrDefault(0L)
                recording = false
                if (duration < 1000) {
                    notify("太短了，至少说一秒")
                } else {
                    val name = store.adoptFile(recordFile, FriendStore.Kind.AUDIO)
                    if (name != null) {
                        voiceName = name
                        notify("已录好")
                    } else {
                        notify("保存失败")
                    }
                }
            }
        }
    }
    var dissolving by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val imported = store.importFile(uri, FriendStore.Kind.PHOTO)
            if (imported != null) {
                photoName = imported
                notify("照片已导入")
            } else {
                notify("照片导入失败")
            }
        }
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val imported = store.importFile(uri, FriendStore.Kind.AUDIO)
            if (imported != null) {
                voiceName = imported
                notify("参考音频已导入")
            } else {
                notify("音频导入失败")
            }
        }
    }

    fun save() {
        val trimmed = name.trim()
        val oldName = existing?.name.orEmpty()
        if (trimmed.isEmpty()) {
            notify("先给朋友起个名字")
            return
        }
        scope.launch {
            store.upsert(
                Friend(
                    id = id,
                    name = trimmed,
                    photo = photoName.ifBlank { null },
                    voice = voiceName.ifBlank { null },
                    persona = persona.trim(),
                    catchphrase = catchphrase.trim(),
                    nickname = nickname.trim(),
                    relationship = relationship.trim(),
                    status = status.trim(),
                    likes = likes.trim(),
                    dislikes = dislikes.trim(),
                    sharedMemories = sharedMemories.trim(),
                    scripts = scripts,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    lastActive = existing?.lastActive ?: 0L,
                ),
            )
            if (oldName.isNotBlank() && oldName != trimmed) {
                runCatching { AppGraph.memory.replaceName(id, oldName, trimmed) }
            }
            notify("已保存")
            onSaved(id)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    FsPage(
        title = if (existing == null) "新建朋友" else "编辑档案",
        onBack = onBack,
        toolbarActions = {},
    ) {
        item(key = "hero") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                FriendAvatar(
                    file = AppGraph.friends.photoFile(photoName),
                    name = name,
                    size = 84.dp,
                )
            }
        }

        section(title = "照片与声音", key = "media") {
            PickRow(
                title = "照片",
                summary = if (photoName.isBlank()) "未选择" else "已导入",
                onClick = { photoPicker.launch(arrayOf("image/*")) },
            )
            HorizontalDivider()
            PickRow(
                title = "参考音频",
                summary = if (voiceName.isNotBlank()) "已导入" else "mp3 / wav / m4a",
                onClick = { audioPicker.launch(arrayOf("audio/*")) },
            )
            HorizontalDivider()
            PickRow(
                title = if (recording) "停止录音" else "录一段参考音频",
                summary = if (recording) "正在录…再点一下结束" else "5~15 秒最像",
                onClick = { toggleRecord() },
            )
        }

        section(title = "资料", key = "profile") {
            CardBody {
                FsInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "名字（可改）",
                    placeholder = "比如：阿杰",
                )
                Spacer(Modifier.height(FsSpacing.Item))
                FsInput(
                    value = persona,
                    onValueChange = { persona = it },
                    label = "性格 / 说话风格（可选）",
                    placeholder = "话不多但很会安慰人",
                    singleLine = false,
                    minHeight = 64.dp,
                )
                Spacer(Modifier.height(FsSpacing.Item))
                FsInput(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = "TA 怎么称呼你（可选）",
                    placeholder = "比如：小王 老板 宝贝",
                )
                Spacer(Modifier.height(FsSpacing.Item))
                FsInput(
                    value = catchphrase,
                    onValueChange = { catchphrase = it },
                    label = "口头禅（可选）",
                    placeholder = "就那样吧",
                )
            }
        }

        section(title = "深度档案（可选）", key = "deep") {
            CardBody {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyClickable(enabled = true) { expanded = !expanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (expanded) "收起" else "展开更多",
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "关系 现状 喜好 回忆",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntSize>(dampingRatio = 0.9f, stiffness = 300f)) + fadeIn(MotionTokens.fade),
                    exit = shrinkVertically(androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntSize>(dampingRatio = 0.9f, stiffness = 460f)) + fadeOut(MotionTokens.quickFade),
                ) {
                    Column(modifier = Modifier.animateContentSize()) {
                        Spacer(Modifier.height(FsSpacing.Item))
                        FsInput(
                            value = relationship,
                            onValueChange = { relationship = it },
                            label = "你们的关系",
                            placeholder = "比如：死党 大学室友 发小",
                        )
                        Spacer(Modifier.height(FsSpacing.Item))
                        FsInput(
                            value = status,
                            onValueChange = { status = it },
                            label = "TA 的现状",
                            placeholder = "比如：刚毕业在考研 互联网公司加班",
                        )
                        Spacer(Modifier.height(FsSpacing.Item))
                        FsInput(
                            value = likes,
                            onValueChange = { likes = it },
                            label = "TA 喜欢什么",
                            placeholder = "比如：科幻片 冰美式 撸串",
                        )
                        Spacer(Modifier.height(FsSpacing.Item))
                        FsInput(
                            value = dislikes,
                            onValueChange = { dislikes = it },
                            label = "TA 的雷区",
                            placeholder = "比如：香菜 被催婚 早起",
                        )
                        Spacer(Modifier.height(FsSpacing.Item))
                        FsInput(
                            value = sharedMemories,
                            onValueChange = { sharedMemories = it },
                            label = "专属回忆",
                            placeholder = "比如：我们经常去大学城吃烧烤",
                            singleLine = false,
                            minHeight = 64.dp,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "填得越详细 TA 越像本人",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        section(title = "常用回复（离线模式使用）", key = "scripts") {
            CardBody {
                Text(
                    text = "每行一条，可写「关键词|回复」。",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(10.dp))
                FsInput(
                    value = scripts,
                    onValueChange = { scripts = it },
                    placeholder = "早|又起这么早\n累|那就先歇着\n我在呢",
                    singleLine = false,
                    minHeight = 110.dp,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
            }
        }

        item(key = "save") {
            Button(
                onClick = { save() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "保存")
            }
        }

        if (existing != null) {
            item(key = "delete") {
                Button(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "删除这个朋友", color = MiuixTheme.colorScheme.error)
                }
            }
        }

    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "删除档案",
            summary = "$name 以及这段对话记录都会从前这台设备移除。",
            confirmText = "删除",
            danger = true,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                dissolving = true
                scope.launch {
                    store.delete(id)
                    notify("已删除")
                }
            },
        )
    }

        // 删除确认：一次性粒子消散，不常驻任何页面
        DissolveEffect(
            active = dissolving,
            color = MiuixTheme.colorScheme.primary,
        ) { onBack() }
    }
}

@Composable
private fun PickRow(title: String, summary: String, onClick: () -> Unit) {
    PressableRow(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = FsSpacing.CardPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/** 卡片内可点击行（供 section 的 ColumnScope 使用）。 */
@Composable
private fun PressableRow(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) { content() }
}
