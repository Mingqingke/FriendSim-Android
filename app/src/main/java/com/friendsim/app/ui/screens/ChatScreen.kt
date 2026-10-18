package com.friendsim.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import com.friendsim.app.AppGraph
import com.friendsim.app.AppVisibility
import com.friendsim.app.core.data.AppearancePrefs
import com.friendsim.app.core.data.FriendStore
import com.friendsim.app.core.media.ChatExporter
import com.friendsim.app.core.model.ChatMessage
import com.friendsim.app.core.model.Friend
import com.friendsim.app.core.model.Roles
import com.friendsim.app.core.notify.ChatNotifier
import com.friendsim.app.core.voice.Recorder
import com.friendsim.app.ui.components.BubbleShape
import com.friendsim.app.ui.components.ConfirmDialog
import com.friendsim.app.ui.components.FriendAvatar
import com.friendsim.app.ui.components.rememberFileBitmap
import com.friendsim.app.ui.motion.Entrance
import com.friendsim.app.ui.motion.LocalMotionPrefs
import com.friendsim.app.ui.motion.MotionTokens
import com.friendsim.app.ui.motion.bouncyClickable
import com.friendsim.app.ui.motion.rememberIosFling
import com.friendsim.app.ui.motion.rubberBand
import com.friendsim.app.ui.motion.sharedAvatarModifier
import com.friendsim.app.ui.theme.LocalAccent
import com.friendsim.app.ui.theme.LocalHapticsEnabled
import com.friendsim.app.ui.theme.LocalIsDark
import com.friendsim.app.ui.theme.chatFontFamily
import com.friendsim.app.ui.theme.parseColor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Mic
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

private const val TIMEOUT_MS = 10_000L

/** 退出聊天页后草稿还在。 */
private object Drafts {
    val map = mutableMapOf<String, String>()
}

/** 二级页：纯一对一聊天。 */
@Composable
fun ChatScreen(
    friendId: String,
    shared: androidx.compose.animation.SharedTransitionScope? = null,
    visibility: androidx.compose.animation.AnimatedVisibilityScope? = null,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenSoul: (String) -> Unit,
    notify: (String) -> Unit,
) {
    val store = AppGraph.friends
    val settings = AppGraph.settings
    val voice = AppGraph.voice
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val friends by store.friends.collectAsState()
    val friend = friends.firstOrNull { it.id == friendId }

    if (friend == null) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "档案已删除", fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            TextButton(text = "返回", onClick = onBack)
        }
        return
    }

    val apiKey by settings.apiKey.collectAsState(initial = "")
    val allowPublic by settings.allowPublic.collectAsState(initial = true)
    val offlineOnly by settings.offlineOnly.collectAsState(initial = false)
    val privacyAck by settings.privacyAck.collectAsState(initial = false)
    val look by settings.appearance.collectAsState(initial = AppearancePrefs())
    val dark = LocalIsDark.current
    val online = rememberOnline()

    val scope = rememberCoroutineScope()
    var messages by remember(friendId) { mutableStateOf(store.messages(friendId)) }
    var input by remember(friendId) { mutableStateOf(Drafts.map[friendId].orEmpty()) }
    var draft by remember { mutableStateOf("") }
    var streaming by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var askPrivacy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf("") }
    var offlineNotified by remember { mutableStateOf(false) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var showHeaderMenu by remember { mutableStateOf(false) }
    var fullImage by remember { mutableStateOf<File?>(null) }
    var quoted by remember { mutableStateOf<String?>(null) }
    var timedOut by remember { mutableStateOf(false) }
    var slow by remember { mutableStateOf(false) }
    var showJump by remember { mutableStateOf(false) }
    var replyAsVoice by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val statuses = remember { mutableStateMapOf<String, String>() }

    val listState = rememberLazyListState()
    val playerRef = remember { arrayOfNulls<MediaPlayer>(1) }

    fun haptic(type: HapticFeedbackType) {
        if (hapticsEnabled) runCatching { haptics.performHapticFeedback(type) }
    }

    fun speak(text: String) {
        scope.launch {
            val cfg = settings.snapshot()
            voice.speak(text, friend, store.voiceFile(friend), cfg)
        }
    }

    fun playVoice(message: ChatMessage) {
        if (playingId == message.id) {
            runCatching { playerRef[0]?.stop() }
            playerRef[0] = null
            playingId = null
            return
        }
        val file = store.chatVoiceFile(message.media) ?: return
        runCatching {
            playerRef[0]?.release()
            playerRef[0] = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    playingId = null
                    runCatching { it.release() }
                }
                start()
            }
            playingId = message.id
        }.onFailure { notify("播放失败") }
    }

    suspend fun runRequest(
        userMsgId: String,
        text: String,
        quote: String?,
        imageDataUrl: String? = null,
        previousReply: String? = null,
    ) = coroutineScope {
        val history = store.messages(friendId)
        messages = history
        streaming = true
        timedOut = false
        slow = false
        statuses[userMsgId] = "pending"
        val cfg = settings.snapshot()
        var acc = ""
        var gotAny = false
        var failed = false
        val collectJob = launch {
            try {
                val memories = runCatching { AppGraph.memory.digest(friendId) }.getOrDefault(emptyList())
                AppGraph.engine.respond(
                    friend = friend,
                    history = history.filter { it.role != Roles.NOTICE }.dropLast(1),
                    userText = text,
                    config = cfg,
                    memories = memories,
                    imageDataUrl = imageDataUrl,
                ).collect { event ->
                        when (event) {
                            is com.friendsim.app.core.llm.ChatEvent.Engine -> {
                                if (event.level == 3 && !offlineNotified) {
                                    offlineNotified = true
                                }
                            }

                            is com.friendsim.app.core.llm.ChatEvent.Delta -> {
                                gotAny = true
                                acc += event.text
                            }

                            is com.friendsim.app.core.llm.ChatEvent.Notice -> Unit
                            com.friendsim.app.core.llm.ChatEvent.Done -> Unit
                        }
                    }
            } catch (t: Throwable) {
                failed = true
            }
        }
        val slowTimer = launch {
            delay(6_000)
            if (!gotAny) slow = true
        }
        val watchdog = launch {
            delay(TIMEOUT_MS)
            if (!gotAny) {
                timedOut = true
                statuses[userMsgId] = "failed"
                streaming = false
                collectJob.cancel()
            }
        }
        collectJob.join()
        watchdog.cancel()
        slowTimer.cancel()
        slow = false

        if (gotAny && acc.isNotBlank()) {
            val moodFactor = when (friend.mood) {
                "LOW" -> 1.5f
                "EXCITED" -> 0.75f
                else -> 1f
            }
            val typingMs = ((1500 + acc.length * 30) * moodFactor).coerceIn(1200f, 6000f).toLong()
            delay(typingMs)
            // 逐字显影：像真人打字一样蹦出来
            val text = acc.trim()
            draft = ""
            var shown = 0
            while (shown < text.length) {
                shown = (shown + 2).coerceAtMost(text.length)
                draft = text.substring(0, shown)
                delay(26)
            }
            draft = ""
            if (!AppVisibility.foreground) {
                ChatNotifier.notifyMessage(context, friendId, friend.name, text)
            }
            val replyText = acc.trim()
            val alts = listOfNotNull(previousReply?.takeIf { it.isNotBlank() })
            if (cfg.voiceReply || replyAsVoice) {
                replyAsVoice = false
                // 把回复合成成语音条：优先克隆音色，退系统语音
                val audio = runCatching {
                    voice.synthesizeToFile(replyText, friend, store.voiceFile(friend), cfg)
                }.getOrNull()
                val stored = audio?.let { store.adoptFile(it, FriendStore.Kind.AUDIO) }
                if (audio != null) runCatching { audio.delete() }
                if (stored != null) {
                    store.append(
                        friendId,
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            role = Roles.FRIEND,
                            text = replyText,
                            kind = ChatMessage.KIND_VOICE,
                            media = stored,
                            durationMs = (replyText.length * 180L).coerceIn(1200L, 60_000L),
                            alternatives = alts,
                        ),
                    )
                } else {
                    store.append(
                        friendId,
                        ChatMessage(UUID.randomUUID().toString(), Roles.FRIEND, replyText, alternatives = alts),
                    )
                }
            } else {
                store.append(
                    friendId,
                    ChatMessage(UUID.randomUUID().toString(), Roles.FRIEND, replyText, alternatives = alts),
                )
            }
            messages = store.messages(friendId)
            statuses.remove(userMsgId)
            if (!AppVisibility.foreground) {
                ChatNotifier.notifyMessage(context, friendId, friend.name, acc.trim())
            }
            if (cfg.autoSpeak) speak(acc.trim())
            // 后台记忆压缩：把最早一批对话压成一条回忆
            scope.launch {
                runCatching {
                    com.friendsim.app.core.llm.MemoryCompactor.maybeCompact(
                        friendId = friendId,
                        messages = store.messages(friendId),
                        store = AppGraph.memory,
                        summarize = { transcript -> AppGraph.engine.summarize(transcript, cfg) },
                    )
                }
            }
        } else if (failed || !gotAny) {
            statuses[userMsgId] = "failed"
        }
        streaming = false
    }

    fun deliver(text: String, quote: String? = null) {
        if (text.isBlank() || streaming) return
        scope.launch {
            val userMsgId = UUID.randomUUID().toString()
            store.append(
                friendId,
                ChatMessage(userMsgId, Roles.USER, text, quote = quote),
            )
            runCatching { store.bumpAffinity(friendId) }
            runCatching { AppGraph.memory.autoExtract(friendId, text) }
            messages = store.messages(friendId)
            runRequest(userMsgId, text, quote)
        }
    }

    fun retry(message: ChatMessage) {
        if (streaming) return
        scope.launch { runRequest(message.id, message.text, message.quote) }
    }

    fun submit() {
        val text = input.trim()
        if (text.isEmpty() || streaming) return
        if (!offlineOnly && (apiKey.isNotBlank() || allowPublic) && !privacyAck) {
            pending = text
            askPrivacy = true
            return
        }
        input = ""
        Drafts.map[friendId] = ""
        val q = quoted
        quoted = null
        haptic(HapticFeedbackType.TextHandleMove)
        deliver(text, q)
    }

    fun sendVoice(file: File, durationMs: Long) {
        scope.launch {
            val name = store.adoptFile(file, FriendStore.Kind.AUDIO) ?: return@launch
            store.append(
                friendId,
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = Roles.USER,
                    text = "[语音]",
                    kind = ChatMessage.KIND_VOICE,
                    media = name,
                    durationMs = durationMs,
                ),
            )
            replyAsVoice = true
            messages = store.messages(friendId)
        }
    }

    fun sendImage(uri: Uri) {
        scope.launch {
            val name = store.importFile(uri, FriendStore.Kind.PHOTO) ?: return@launch
            val msgId = UUID.randomUUID().toString()
            store.append(
                friendId,
                ChatMessage(
                    id = msgId,
                    role = Roles.USER,
                    text = "发了一张图",
                    kind = ChatMessage.KIND_IMAGE,
                    media = name,
                ),
            )
            messages = store.messages(friendId)
            // 图片交给支持视觉的端点识别（DeepSeek chat 本身不吃图）
            val dataUrl = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val file = store.photoFile(name) ?: return@runCatching null
                    if (file.length() > 3_000_000L) return@runCatching null
                    val mime = if (file.name.endsWith("png", true)) "image/png" else "image/jpeg"
                    "data:$mime;base64," + android.util.Base64.encodeToString(
                        file.readBytes(),
                        android.util.Base64.NO_WRAP,
                    )
                }.getOrNull()
            }
            if (dataUrl != null) {
                runRequest(msgId, "我发了一张图片 你看看", null, dataUrl)
            }
        }
    }

    fun poke() {
        haptic(HapticFeedbackType.LongPress)
        scope.launch {
            store.append(
                friendId,
                ChatMessage(UUID.randomUUID().toString(), Roles.SYSTEM, "你拍了拍 ${friend.name.ifBlank { "朋友" }}"),
            )
            messages = store.messages(friendId)
        }
    }

    LaunchedEffect(friendId, messages.size) {
        runCatching { store.markRead(friendId) }
    }

    // 主动消息：隔 4 小时以上、不在免打扰时段
    LaunchedEffect(friendId) {
        runCatching {
            val cfg = settings.snapshot()
            if (!cfg.proactive) return@runCatching
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (cfg.quietHours && (hour >= 23 || hour < 8)) return@runCatching
            val last = messages.lastOrNull { it.role != Roles.NOTICE } ?: return@runCatching
            if (System.currentTimeMillis() - last.at < 4 * 3600_000L) return@runCatching
            val line = when {
                hour in 5..10 -> "早，醒了没？"
                hour in 11..17 -> "在忙吗？"
                hour in 18..22 -> "今天过得怎么样？"
                else -> "还没睡？"
            }
            store.append(friendId, ChatMessage(UUID.randomUUID().toString(), Roles.FRIEND, line))
            messages = store.messages(friendId)
        }
    }

    // 通知栏快捷回复
    LaunchedEffect(friendId) {
        ChatNotifier.cancel(context, friendId)
        val text = ChatNotifier.consumePending(context, friendId)
        if (!text.isNullOrBlank()) {
            input = text
            submit()
        }
    }

    // 滚动：只在贴底时自动滚，否则提示"有新消息"
    // 进页直接贴底：第一次看聊天应该是最新的，而不是从最早一条开始
    LaunchedEffect(friendId) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    val nearBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= info.totalItemsCount - 2
        }
    }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    // 记录上一次的消息条数：只有「变多了」且用户不在底部时才提示
    var lastCount by remember { mutableStateOf(messages.size) }
    LaunchedEffect(messages.size, nearBottom) {
        val grew = messages.size > lastCount
        lastCount = messages.size
        if (!grew) return@LaunchedEffect
        if (nearBottom) showJump = false else showJump = true
    }

    LaunchedEffect(imeBottom, nearBottom, messages.size) {
        if (nearBottom && messages.isNotEmpty()) {
            val count = messages.size + if (streaming) 1 else 0
            listState.animateScrollToItem((count - 1).coerceAtLeast(0))
            showJump = false
        }
    }
    LaunchedEffect(nearBottom) {
        if (nearBottom) showJump = false
    }

    val mineBubble = parseColor(look.mineColor, Color(0xFF007AFF))
    val friendBubble = if (dark && look.friendColor.equals("#E5E5EA", true)) {
        Color(0xFF2C2C2E)
    } else {
        parseColor(look.friendColor, if (dark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
    }
    val friendText = if (dark) Color.White else Color.Black

    Box(modifier = Modifier.fillMaxSize()) {
        when (look.bgMode) {
            AppearancePrefs.BG_COLOR -> Box(
                Modifier.fillMaxSize().background(parseColor(look.bgColor, MiuixTheme.colorScheme.surface)),
            )

            AppearancePrefs.BG_IMAGE -> {
                val file = remember(look.bgImage) { store.backgroundFile(look.bgImage) }
                val bitmap = rememberFileBitmap(file)
                if (bitmap != null) {
                    val bgShift by remember {
                        derivedStateOf {
                            (listState.firstVisibleItemScrollOffset).toFloat()
                        }
                    }
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(look.bgBlur.dp)
                            .graphicsLayer { translationY = -bgShift * 0.12f },
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = (look.bgDim / 100f).coerceIn(0f, 1f))),
                    )
                }
            }

            else -> Unit
        }

        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            if (searching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        if (query.isEmpty()) {
                            Text(text = "搜索聊天记录", fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurface),
                            singleLine = true,
                            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .bouncyClickable(scaleDown = 0.94f) {
                                searching = false
                                query = ""
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(text = "取消", fontSize = 14.sp, color = MiuixTheme.colorScheme.primary)
                    }
                }
            } else {
                ChatHeader(
                    friend = friend,
                    avatarModifier = sharedAvatarModifier(shared, visibility, friendId),
                    typing = streaming,
                    online = online,
                    lowMood = friend.mood == "LOW",
                    onBack = onBack,
                    onPoke = { poke() },
                    onMenu = { showHeaderMenu = true },
                    onSearch = { searching = true },
                )
            }

            LazyColumn(
                state = listState,
                flingBehavior = rememberIosFling(),
                modifier = Modifier.weight(1f).fillMaxWidth().rubberBand(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (messages.isEmpty() && !streaming) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 140.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "和 ${friend.name.ifBlank { "朋友" }} 打个招呼",
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.7f),
                            )
                        }
                    }
                }

                val shown = if (searching && query.isNotBlank()) {
                    messages.filter { it.text.contains(query, true) }
                } else {
                    messages
                }
                itemsIndexed(shown, key = { _, m -> m.id }) { index, message ->
                    val prev = messages.getOrNull(index - 1)
                    val next = messages.getOrNull(index + 1)
                    val sameAsPrev = prev?.role == message.role
                    val sameAsNext = next?.role == message.role
                    val gapTop = if (index == 0 || sameAsPrev) 0.dp else 6.dp
                    val showTime = prev == null ||
                        message.at - prev.at > 10 * 60 * 1000L

                    Column(modifier = Modifier.fillMaxWidth().animateItem()) {
                        if (showTime) {
                            Entrance {
                                Text(
                                    text = chatTimeLabel(message.at),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                    fontSize = 13.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        if (gapTop > 0.dp) Spacer(Modifier.height(gapTop))
                        Entrance {
                            when (message.role) {
                                Roles.NOTICE -> Unit
                                Roles.SYSTEM -> SystemLine(message.text)
                                else -> MessageRow(
                                    message = message,
                                    friend = friend,
                                    photo = store.photoFile(friend),
                                    look = look,
                                    dark = dark,
                                    mineBubble = mineBubble,
                                    friendBubble = friendBubble,
                                    friendText = friendText,
                                    showTail = !sameAsNext,
                                    playing = playingId == message.id,
                                    status = statuses[message.id],
                                    onRetry = { retry(message) },
                                    onPlayVoice = { playVoice(message) },
                                    onOpenImage = { store.photoFile(message.media)?.let { fullImage = it } },
                                    onQuote = { quoted = message.text.take(60) },
                                    onRegenerate = {
                                        if (!streaming) {
                                            scope.launch {
                                                val all = store.messages(friendId)
                                                val lastUser = all.lastOrNull { it.role == Roles.USER }
                                                val keep = all.filter {
                                                    it.role != Roles.FRIEND || it.at <= (lastUser?.at ?: 0L)
                                                }
                                                val oldReply = all.lastOrNull { it.role == Roles.FRIEND }?.text
                                                store.replaceAll(friendId, keep)
                                                messages = store.messages(friendId)
                                                lastUser?.let { runRequest(it.id, it.text, it.quote, previousReply = oldReply) }
                                            }
                                        }
                                    },
                                    onRollback = {
                                        scope.launch {
                                            store.truncateAfter(friendId, message.id)
                                            messages = store.messages(friendId)
                                            notify("已回到这里")
                                        }
                                    },
                                    onSpeak = { speak(message.text) },
                                    onSwipe = {
                                        val variants = listOf(message.text) + message.alternatives
                                        if (variants.size > 1) {
                                            val next = variants.drop(1) + variants.first()
                                            scope.launch {
                                                store.updateMessage(
                                                    friendId,
                                                    message.copy(text = next.first(), alternatives = next.drop(1)),
                                                )
                                                messages = store.messages(friendId)
                                            }
                                        }
                                    },
                                    onCopy = {
                                        val cm = context.getSystemService(ClipboardManager::class.java)
                                        cm?.setPrimaryClip(ClipData.newPlainText("message", message.text))
                                        haptic(HapticFeedbackType.TextHandleMove)
                                        notify("已复制")
                                    },
                                    onDelete = {
                                        heavyHaptic(context)
                                        scope.launch {
                                            store.deleteMessage(friendId, message.id)
                                            messages = store.messages(friendId)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                if (draft.isNotEmpty()) {
                    item(key = "draft") {
                        MessageRow(
                            message = ChatMessage("draft", Roles.FRIEND, draft),
                            friend = friend,
                            photo = store.photoFile(friend),
                            look = look,
                            dark = dark,
                            mineBubble = mineBubble,
                            friendBubble = friendBubble,
                            friendText = friendText,
                            showTail = true,
                            playing = false,
                            status = null,
                            onRetry = {},
                            onPlayVoice = {},
                            onOpenImage = {},
                            onQuote = {},
                            onRegenerate = {},
                            onRollback = {},
                            onCopy = {},
                            onSpeak = {},
                            onDelete = {},
                            onSwipe = {},
                        )
                    }
                }

                if (streaming) {
                    item(key = "typing") {
                        if (timedOut) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "对方似乎走神了",
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        } else {
                            TypingRow(friend = friend, photo = store.photoFile(friend))
                        }
                    }
                }
            }

            // 断网 / 超时：输入框上方一条极简窄条
            AnimatedVisibility(
                visible = !online || slow || timedOut,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(160)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (timedOut) Color(0x14FF9F0A)
                            else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when {
                            timedOut -> "对方似乎走神了，请重试"
                            slow -> "网络有点慢，再等等"
                            offlineOnly -> "等待网络连接"
                            else -> "网络不可用，已切换离线预设"
                        },
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    if (timedOut) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .bouncyClickable(scaleDown = 0.94f) {
                                    timedOut = false
                                    messages.lastOrNull { statuses[it.id] == "failed" }
                                        ?.let { retry(it) }
                                }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(text = "重试", fontSize = 12.sp, color = MiuixTheme.colorScheme.primary)
                        }
                    }
                }
            }

            quoted?.let { q ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "回复：$q",
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .bouncyClickable(scaleDown = 0.94f) { quoted = null }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(text = "取消", fontSize = 12.sp, color = MiuixTheme.colorScheme.primary)
                    }
                }
            }

            ChatInput(
                value = input,
                onValueChange = {
                    input = it
                    Drafts.map[friendId] = it
                },
                sending = streaming,
                onSend = { submit() },
                onSendVoice = { file, duration -> sendVoice(file, duration) },
                onSendImage = { uri -> sendImage(uri) },
                notify = notify,
                photoBg = look.bgMode != AppearancePrefs.BG_NONE,
            )
        }

        // 有历史消息时的新消息提示
        AnimatedVisibility(
            visible = showJump,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
            enter = fadeIn(tween(180)) + scaleIn(MotionTokens.springDefault, initialScale = 0.9f),
            exit = fadeOut(tween(140)) + scaleOut(MotionTokens.springFast, targetScale = 0.92f),
        ) {
            Row(
                modifier = Modifier
                    .shadow(10.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(MiuixTheme.colorScheme.surface)
                    .bouncyClickable(scaleDown = 0.94f) {
                        scope.launch {
                            val count = messages.size + if (streaming) 1 else 0
                            if (count > 0) listState.animateScrollToItem(count - 1)
                            showJump = false
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "有新消息", fontSize = 12.sp, color = MiuixTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(text = "↓", fontSize = 12.sp, color = MiuixTheme.colorScheme.primary)
            }
        }
    }

    if (showHeaderMenu) {
        HeaderMenu(
            hasMessages = messages.isNotEmpty(),
            onDismiss = { showHeaderMenu = false },
            onEdit = {
                showHeaderMenu = false
                onEdit(friendId)
            },
            onSoul = {
                showHeaderMenu = false
                onOpenSoul(friendId)
            },
            onExport = {
                showHeaderMenu = false
                if (!exporting) exporting = true
                scope.launch {
                    notify("正在生成…")
                    val path = ChatExporter.export(
                        context = context,
                        friend = friend,
                        messages = messages,
                        mineColorHex = look.mineColor,
                        friendColorHex = look.friendColor,
                        dark = dark,
                    )
                    exporting = false
                    notify(if (path != null) "已保存到相册" else "导出失败")
                }
            },
            onClear = {
                showHeaderMenu = false
                confirmClear = true
            },
        )
    }

    val preview = fullImage
    if (preview != null) {
        val bitmap = rememberFileBitmap(preview)
        Dialog(onDismissRequest = { fullImage = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black)
                    .bouncyClickable(scaleDown = 0.99f) { fullImage = null },
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "清空对话",
            summary = "只删除本机记录。",
            confirmText = "清空",
            danger = true,
            onDismiss = { confirmClear = false },
            onConfirm = {
                confirmClear = false
                scope.launch {
                    store.clearChat(friendId)
                    messages = emptyList()
                }
            },
        )
    }

    if (askPrivacy) {
        OverlayDialog(
            show = true,
            title = "对话会离开本机",
            summary = "对话文本会发送给第三方服务，请勿输入隐私信息。照片与录音不会上传。",
            onDismissRequest = {
                askPrivacy = false
                pending = ""
            },
        ) {
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        text = "只在本机聊",
                        onClick = {
                            askPrivacy = false
                            pending = ""
                            scope.launch { settings.setOfflineOnly(true) }
                            notify("已切换为离线")
                        },
                        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    TextButton(
                        text = "继续",
                        onClick = {
                            askPrivacy = false
                            scope.launch { settings.setPrivacyAck(true) }
                            val text = pending
                            pending = ""
                            input = ""
                            deliver(text)
                        },
                        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

/** 删除这类破坏性操作给一记重震（设备支持时用系统 HEAVY_CLICK）。 */
private fun heavyHaptic(context: android.content.Context) {
    runCatching {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(60)
        }
    }
}

@Composable
private fun rememberOnline(): Boolean {
    val context = LocalContext.current
    val state = produceState(initialValue = true) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                value = true
            }

            override fun onLost(network: Network) {
                value = false
            }
        }
        runCatching { cm?.registerDefaultNetworkCallback(callback) }
        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            runCatching { cm?.unregisterNetworkCallback(callback) }
        }
    }
    return state.value
}

private fun chatTimeLabel(at: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = at }
    val hm = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(at))
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return hm
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (isYesterday) return "昨天 $hm"
    return SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(at))
}

@Composable
private fun ChatHeader(
    friend: Friend,
    avatarModifier: Modifier,
    typing: Boolean,
    online: Boolean,
    lowMood: Boolean,
    onBack: () -> Unit,
    onPoke: () -> Unit,
    onMenu: () -> Unit,
    onSearch: () -> Unit = {},
) {
    val scheme = MiuixTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val bounce = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FriendAvatar(
                file = AppGraph.friends.photoFile(friend),
                name = friend.name,
                size = 40.dp,
                modifier = avatarModifier
                    .graphicsLayer {
                        scaleX = bounce.value
                        scaleY = bounce.value
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (hapticsEnabled) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onPoke()
                                scope.launch {
                                    bounce.animateTo(0.86f, tween(90))
                                    bounce.animateTo(1f, MotionTokens.springBouncy)
                                }
                            },
                        )
                    },
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = friend.name.ifBlank { "朋友" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (online && !lowMood) Color(0xFF34C759) else Color(0xFFB0B0B8),
                        ),
                )
            }
            AnimatedVisibility(
                visible = typing,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(160)),
            ) {
                Text(
                    text = "正在输入…",
                    fontSize = 11.sp,
                    color = scheme.onSurfaceVariantSummary,
                )
            }
        }

        Row(modifier = Modifier.align(Alignment.TopStart)) {
            IconButton(onClick = onBack) {
                Icon(imageVector = MiuixIcons.Back, contentDescription = "返回", tint = scheme.onSurface)
            }
        }
        Row(modifier = Modifier.align(Alignment.TopEnd), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSearch) {
                Icon(imageVector = MiuixIcons.Search, contentDescription = "搜索", tint = scheme.onSurface)
            }
            IconButton(onClick = onMenu) {
                Icon(imageVector = MiuixIcons.More, contentDescription = "更多", tint = scheme.onSurface)
            }
        }
    }
}

@Composable
private fun HeaderMenu(
    hasMessages: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSoul: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(state.isIdle, state.currentState) {
        if (state.isIdle && !state.currentState) {
            pending?.invoke()
            pending = null
            onDismiss()
        }
    }

    Popup(onDismissRequest = { state.targetState = false }, properties = PopupProperties(focusable = true)) {
        AnimatedVisibility(
            visibleState = state,
            enter = scaleIn(MotionTokens.springMenu, initialScale = 0.8f) + fadeIn(tween(140)),
            exit = scaleOut(MotionTokens.springFast, targetScale = 0.9f) + fadeOut(tween(120)),
        ) {
            Column(
                modifier = Modifier
                    .shadow(14.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.surface)
                    .padding(vertical = 4.dp),
            ) {
                MenuItem("编辑档案", { pending = onEdit; state.targetState = false })
                MenuItem("灵魂档案", { pending = onSoul; state.targetState = false })
                if (hasMessages) {
                    MenuItem("导出长图", { pending = onExport; state.targetState = false })
                    MenuItem("清空对话", { pending = onClear; state.targetState = false }, danger = true)
                }
            }
        }
    }
}

@Composable
private fun SystemLine(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    message: ChatMessage,
    friend: Friend,
    photo: File?,
    look: AppearancePrefs,
    dark: Boolean,
    mineBubble: Color,
    friendBubble: Color,
    friendText: Color,
    showTail: Boolean,
    playing: Boolean,
    status: String?,
    onRetry: () -> Unit,
    onPlayVoice: () -> Unit,
    onOpenImage: () -> Unit,
    onQuote: () -> Unit,
    onSwipe: () -> Unit = {},
    onRegenerate: () -> Unit,
    onRollback: () -> Unit,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onDelete: () -> Unit,
) {
    val mine = message.role == Roles.USER
    val haptics = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val motion = LocalMotionPrefs.current
    var menu by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val isVoice = message.kind == ChatMessage.KIND_VOICE

    val pressScale by animateFloatAsState(
        targetValue = if (pressed && !motion.reduced) 0.96f else 1f,
        animationSpec = if (motion.reduced) MotionTokens.instant else MotionTokens.springFast,
        label = "bubblePress",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!mine) {
            FriendAvatar(file = photo, name = friend.name, size = 28.dp)
            Spacer(Modifier.width(6.dp))
        }

        if (mine && status != null) {
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(18.dp)
                    .bouncyClickable(enabled = status == "failed", scaleDown = 0.9f, onClick = onRetry),
                contentAlignment = Alignment.Center,
            ) {
                if (status == "failed") {
                    Box(
                        modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(0xFFFF3B30)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "!", fontSize = 11.sp, color = Color.White)
                    }
                } else {
                    Spinner()
                }
            }
        }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                    transformOrigin = TransformOrigin(if (mine) 1f else 0f, 1f)
                }
                .widthIn(max = (280 * look.fontScale).dp)
                .clip(
                    BubbleShape(
                        mine = mine,
                        iMessage = look.iMessage,
                        radius = look.radiusDp.dp,
                        showTail = showTail,
                    ),
                )
                .background(if (mine) mineBubble else friendBubble)
                .pointerInput(message.id, message.alternatives.size) {
                    var dragged = 0f
                    detectDragGestures(
                        onDragEnd = {
                            if (kotlin.math.abs(dragged) > 60f) onSwipe()
                            dragged = 0f
                        },
                        onDragCancel = { dragged = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragged += dragAmount.x
                        },
                    )
                }
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        when {
                            message.kind == ChatMessage.KIND_IMAGE -> onOpenImage()
                            isVoice -> onPlayVoice()
                            !mine -> {
                                if (hapticsEnabled) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onSpeak()
                            }
                        }
                    },
                    onLongClick = {
                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menu = true
                    },
                )
                .padding(
                    start = if (look.iMessage && !mine) 20.dp else 14.dp,
                    end = if (look.iMessage && mine) 20.dp else 14.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
        ) {
            message.quote?.takeIf { it.isNotBlank() }?.let { q ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = if (mine) 0.18f else 0.5f))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = q,
                        fontSize = 12.sp,
                        maxLines = 2,
                        color = if (mine) Color.White.copy(alpha = 0.9f) else friendText.copy(alpha = 0.75f),
                    )
                }
                Spacer(Modifier.height(5.dp))
            }

            if (message.kind == ChatMessage.KIND_IMAGE) {
                val imageFile = remember(message.media) { AppGraph.friends.photoFile(message.media) }
                val bitmap = rememberFileBitmap(imageFile)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .width(190.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .bouncyClickable(scaleDown = 0.98f, onClick = onOpenImage),
                        contentScale = ContentScale.Crop,
                    )
                }
            } else if (isVoice) {
                VoiceContent(
                    durationMs = message.durationMs,
                    playing = playing,
                    color = if (mine) Color.White else friendText,
                )
            } else {
                val long = message.text.length > 140 || message.text.count { it == '\n' } > 5
                var expanded by remember(message.id) { mutableStateOf(false) }
                Text(
                    text = message.text,
                    fontSize = (15 * look.fontScale).sp,
                    lineHeight = (21 * look.fontScale).sp,
                    color = if (mine) Color.White else friendText,
                    fontFamily = chatFontFamily(look.font),
                    maxLines = if (long && !expanded) 5 else Int.MAX_VALUE,
                )
                if (long && !expanded) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .bouncyClickable(scaleDown = 0.95f) { expanded = true }
                            .padding(vertical = 2.dp),
                    ) {
                        Text(
                            text = "全文",
                            fontSize = 12.sp,
                            color = if (mine) Color.White.copy(alpha = 0.85f) else friendText.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            if (menu) {
                MessageMenu(
                    style = look.menuStyle,
                    onClosed = { menu = false },
                    onCopy = onCopy,
                    onQuote = onQuote,
                    onSpeak = { if (isVoice) onPlayVoice() else onSpeak() },
                    onRegenerate = onRegenerate,
                    onRollback = onRollback,
                    onDelete = onDelete,
                )
            }
        }
    }
}

/** 极简旋转圈（发送中）。 */
@Composable
private fun Spinner() {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    val color = MiuixTheme.colorScheme.onSurfaceVariantSummary
    Canvas(modifier = Modifier.size(14.dp).rotate(angle)) {
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 280f,
            useCenter = false,
            style = Stroke(width = 2f * density, cap = StrokeCap.Round),
            topLeft = Offset(1.5f * density, 1.5f * density),
            size = androidx.compose.ui.geometry.Size(size.width - 3 * density, size.height - 3 * density),
        )
    }
}

@Composable
private fun VoiceContent(durationMs: Long, playing: Boolean, color: Color) {
    val transition = rememberInfiniteTransition(label = "voice")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart),
        label = "wave",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (playing) MiuixIcons.Stopwatch else MiuixIcons.Play,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(12) { index ->
                val base = 4f + ((index * 7) % 5) * 2.5f
                val wave = if (playing) {
                    base + 8f * abs(kotlin.math.sin((phase * 2f * Math.PI).toFloat() + index * 0.6f))
                } else {
                    base
                }
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(wave.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color.copy(alpha = if (playing) 0.95f else 0.65f)),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(text = "${(durationMs / 1000).coerceAtLeast(1)}″", fontSize = 12.sp, color = color)
    }
}

@Composable
private fun MessageMenu(
    style: String,
    onClosed: () -> Unit,
    onCopy: () -> Unit,
    onQuote: () -> Unit,
    onSpeak: () -> Unit,
    onRegenerate: () -> Unit,
    onRollback: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val motion = LocalMotionPrefs.current
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }

    val enter = when (style) {
        AppearancePrefs.MENU_FADE -> fadeIn(tween(180))
        AppearancePrefs.MENU_SCALE -> scaleIn(MotionTokens.springDefault, initialScale = 0.86f) + fadeIn(MotionTokens.fade)
        else -> scaleIn(MotionTokens.springMenu, initialScale = 0.8f) + fadeIn(tween(140))
    }
    val exit = if (style == AppearancePrefs.MENU_FADE) {
        fadeOut(tween(140))
    } else {
        scaleOut(MotionTokens.springFast, targetScale = 0.9f) + fadeOut(tween(120))
    }
    val realEnter = if (motion.reduced) fadeIn(MotionTokens.instant) else enter
    val realExit = if (motion.reduced) fadeOut(MotionTokens.instant) else exit

    LaunchedEffect(state.isIdle, state.currentState) {
        if (state.isIdle && !state.currentState) {
            pending?.invoke()
            pending = null
            onClosed()
        }
    }

    Popup(onDismissRequest = { state.targetState = false }, properties = PopupProperties(focusable = true)) {
        AnimatedVisibility(visibleState = state, enter = realEnter, exit = realExit) {
            Column(
                modifier = Modifier
                    .shadow(14.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.surface)
                    .padding(vertical = 4.dp),
            ) {
                MenuItem("复制", { pending = onCopy; state.targetState = false })
                MenuItem("引用回复", { pending = onQuote; state.targetState = false })
                MenuItem("重新朗读", { pending = onSpeak; state.targetState = false })
                MenuItem("重新生成", { pending = onRegenerate; state.targetState = false })
                MenuItem("从这里重来", { pending = onRollback; state.targetState = false })
                MenuItem("删除", { pending = onDelete; state.targetState = false }, danger = true)
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit, danger: Boolean = false) {
    val scheme = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .width(132.dp)
            .bouncyClickable(scaleDown = 0.97f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (danger) scheme.error else scheme.onSurface,
        )
    }
}

@Composable
private fun TypingRow(friend: Friend, photo: File?) {
    val scheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        FriendAvatar(file = photo, name = friend.name, size = 28.dp)
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.onSurface.copy(alpha = 0.07f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            TypingDots()
        }
    }
}

@Composable
private fun TypingDots() {
    val scheme = MiuixTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "typing")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            val wave = ((phase * 3f - index + 3f) % 3f)
            val alpha = if (wave < 1f) 0.35f + 0.55f * (1f - abs(wave - 0.5f) * 2f) else 0.35f
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(scheme.onSurfaceVariantSummary.copy(alpha = alpha)),
            )
        }
    }
}

@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    sending: Boolean,
    onSend: () -> Unit,
    onSendVoice: (File, Long) -> Unit,
    onSendImage: (Uri) -> Unit,
    notify: (String) -> Unit,
    photoBg: Boolean = false,
) {
    val context = LocalContext.current
    val scheme = MiuixTheme.colorScheme
    val accent = LocalAccent.current ?: scheme.primary
    val haptics = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val motion = LocalMotionPrefs.current
    val scope = rememberCoroutineScope()

    val recorder = remember { Recorder(context) }
    var recording by remember { mutableStateOf(false) }
    val voiceFile = remember { File(context.cacheDir, "chat_voice.wav") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onSendImage(uri)
    }

    var emojiOpen by remember { mutableStateOf(false) }
    val emojis = listOf(
        "😀", "😂", "🥲", "😅", "🙂", "😴", "🤔", "😭", "😡", "🥰",
        "😎", "🤗", "😮", "😑", "🙃", "😇", "🥺", "😳", "🤯", "😋",
        "👍", "👏", "🙏", "💪", "👋", "🤝", "❤️", "💔", "✨", "🌙",
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) notify("按住麦克风开始录音") else notify("需要麦克风权限")
    }

    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(if (photoBg) scheme.surface.copy(alpha = 0.92f) else scheme.onSurface.copy(alpha = 0.04f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // 按住说话
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (recording) scheme.error else scheme.onSurface.copy(alpha = 0.06f))
                .pointerInput(hasPermission) {
                    detectTapGestures(
                        onPress = {
                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                val started = runCatching { recorder.start(scope, voiceFile) }.getOrDefault(false)
                                if (!started) {
                                    notify("无法开始录音")
                                } else {
                                    if (hapticsEnabled) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    recording = true
                                    val released = tryAwaitRelease()
                                    val duration = runCatching { recorder.stop() }.getOrDefault(0L)
                                    recording = false
                                    if (released && duration >= 500) {
                                        onSendVoice(voiceFile, duration)
                                    } else if (released) {
                                        notify("太短了")
                                    }
                                }
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Mic,
                contentDescription = "按住说话",
                tint = if (recording) scheme.onPrimary else scheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(6.dp))

        // 输入框：自动长高到 5 行，之后内部滚动
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(scheme.onSurface.copy(alpha = 0.06f))
                .animateContentSize(
                    animationSpec = if (motion.reduced) {
                        tween(0)
                    } else {
                        androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntSize>(
                            dampingRatio = 0.9f,
                            stiffness = 400f,
                        )
                    },
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (value.isEmpty()) {
                Text(
                    text = if (recording) "松开发送…" else "发消息…",
                    fontSize = 15.sp,
                    color = if (recording) scheme.error else scheme.onSurfaceVariantSummary,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 15.sp, color = scheme.onSurface),
                maxLines = 5,
                cursorBrush = SolidColor(accent),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Send,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSend = {
                        if (value.isNotBlank() && !sending) onSend()
                    },
                ),
            )
        }
        Spacer(Modifier.width(2.dp))

        // 表情
        Box {
            IconButton(onClick = { emojiOpen = !emojiOpen }) {
                Text(text = "☺", fontSize = 20.sp, color = scheme.onSurface)
            }
            if (emojiOpen) {
                androidx.compose.ui.window.Popup(
                    onDismissRequest = { emojiOpen = false },
                    properties = androidx.compose.ui.window.PopupProperties(focusable = true),
                ) {
                    Column(
                        modifier = Modifier
                            .width(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(scheme.surface)
                            .padding(8.dp),
                    ) {
                        emojis.chunked(6).forEach { row ->
                            Row {
                                row.forEach { e ->
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .bouncyClickable(scaleDown = 0.86f) {
                                                onValueChange(value + e)
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(text = e, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(4.dp))

        // 右侧：空态是"图片"，有文字时变"发送"
        AnimatedContent(
            targetState = value.isNotBlank(),
            transitionSpec = {
                (scaleIn(MotionTokens.springFast, initialScale = 0.6f) + fadeIn(tween(120))) togetherWith
                    (scaleOut(MotionTokens.springFast, targetScale = 0.6f) + fadeOut(tween(90)))
            },
            label = "sendMorph",
        ) { hasText ->
            if (hasText) {
                IconButton(
                    onClick = {
                        if (!sending) {
                            if (hapticsEnabled) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onSend()
                        }
                    },
                ) {
                    Icon(
                        imageVector = MiuixIcons.Send,
                        contentDescription = "发送",
                        tint = if (sending) scheme.onSurfaceVariantSummary else accent,
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        if (hapticsEnabled) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        imagePicker.launch(arrayOf("image/*"))
                    },
                ) {
                    Icon(imageVector = MiuixIcons.Photos, contentDescription = "发送图片", tint = scheme.onSurface)
                }
            }
        }
    }
}
