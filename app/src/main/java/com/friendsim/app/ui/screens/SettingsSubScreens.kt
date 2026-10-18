package com.friendsim.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friendsim.app.AppGraph
import com.friendsim.app.core.data.SettingsStore
import com.friendsim.app.ui.components.CardBody
import com.friendsim.app.ui.components.ConfirmDialog
import com.friendsim.app.ui.components.DangerConfirmDialog
import com.friendsim.app.ui.motion.DissolveEffect
import com.friendsim.app.ui.components.FsInput
import com.friendsim.app.ui.components.FsPage
import com.friendsim.app.ui.components.FsSpacing
import com.friendsim.app.ui.components.section
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val ttsLabels = listOf("克隆音色", "系统语音", "关闭")
private val ttsValues = listOf(
    SettingsStore.TTS_BRIDGE,
    SettingsStore.TTS_SYSTEM,
    SettingsStore.TTS_OFF,
)

/** 二级页：对话设置。 */
@Composable
fun ChatSettingsScreen(
    onBack: () -> Unit,
    onOpenAdvanced: () -> Unit,
    notify: (String) -> Unit = {},
) {
    val settings = AppGraph.settings
    val scope = rememberCoroutineScope()
    val apiKey by settings.apiKey.collectAsState(initial = "")
    val allowPublic by settings.allowPublic.collectAsState(initial = true)
    val offlineOnly by settings.offlineOnly.collectAsState(initial = false)
    var firing by remember { mutableStateOf(false) }
    var weatherOn by remember { mutableStateOf(true) }
    var proactiveOn by remember { mutableStateOf(true) }
    var quietHours by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val cfg = settings.snapshot()
        weatherOn = cfg.weatherOn
        proactiveOn = cfg.proactive
        quietHours = cfg.quietHours
    }

    FsPage(title = "对话", onBack = onBack) {
        section(title = "引擎", key = "engine") {
            ArrowPreference(
                title = "高级设置",
                summary = if (apiKey.isNotBlank()) "已配置密钥" else null,
                onClick = onOpenAdvanced,
            )
            HorizontalDivider()
            SwitchPreference(
                checked = allowPublic,
                onCheckedChange = { scope.launch { settings.setAllowPublic(it) } },
                title = "公共端点",
                summary = null,
            )
            HorizontalDivider()
            SwitchPreference(
                checked = offlineOnly,
                onCheckedChange = { scope.launch { settings.setOfflineOnly(it) } },
                title = "仅离线",
                summary = null,
            )
            HorizontalDivider()
            Footnote("公共端点由第三方提供，请勿输入隐私信息。")
        }

        section(title = "陪伴", key = "company") {
            SwitchPreference(
                checked = weatherOn,
                onCheckedChange = {
                    weatherOn = it
                    scope.launch { settings.setWeatherOn(it) }
                },
                title = "情境感知",
                summary = null,
            )
            HorizontalDivider()
            SwitchPreference(
                checked = proactiveOn,
                onCheckedChange = {
                    proactiveOn = it
                    scope.launch { settings.setProactive(it) }
                },
                title = "主动消息",
                summary = null,
            )
            HorizontalDivider()
            SwitchPreference(
                checked = quietHours,
                onCheckedChange = {
                    quietHours = it
                    scope.launch { settings.setQuietHours(it) }
                },
                title = "深夜免打扰",
                summary = null,
            )
            HorizontalDivider()
            ArrowPreference(
                title = if (firing) "正在发送…" else "现在试一条",
                summary = "忽略时间间隔，立刻让一个朋友找你",
                onClick = {
                    if (!firing) {
                        firing = true
                        scope.launch {
                            val sent = runCatching {
                                com.friendsim.app.core.notify.ProactiveEngine.fire(
                                    AppGraph.app,
                                    ignoreGap = true,
                                    ignoreQuiet = true,
                                )
                            }.getOrNull()
                            firing = false
                            notify(if (sent != null) "${sent.name} 发来一条" else "没有可以发的朋友")
                        }
                    }
                },
            )
            HorizontalDivider()
            Footnote("主动消息只在隔了半天以上、且不在 23:00–08:00 时出现。")
        }
    }
}

/** 二级页：语音设置。 */
@Composable
fun VoiceSettingsScreen(
    onBack: () -> Unit,
    notify: (String) -> Unit,
) {
    val settings = AppGraph.settings
    val scope = rememberCoroutineScope()
    val autoSpeak by settings.autoSpeak.collectAsState(initial = false)
    val voiceReply by settings.voiceReply.collectAsState(initial = false)
    var bridgeUrl by remember { mutableStateOf(SettingsStore.DEFAULT_BRIDGE) }
    var ttsMode by remember { mutableStateOf(SettingsStore.TTS_BRIDGE) }

    LaunchedEffect(Unit) {
        val cfg = settings.snapshot()
        bridgeUrl = cfg.bridgeUrl
        ttsMode = cfg.ttsMode
    }

    FsPage(title = "语音", onBack = onBack) {
        section(title = "朗读", key = "tts") {
            OverlayDropdownPreference(
                items = ttsLabels,
                selectedIndex = ttsValues.indexOf(ttsMode).coerceAtLeast(0),
                title = "朗读方式",
                summary = null,
                onSelectedIndexChange = { index ->
                    val value = ttsValues.getOrNull(index) ?: SettingsStore.TTS_BRIDGE
                    ttsMode = value
                    scope.launch { settings.setTtsMode(value) }
                },
            )
            HorizontalDivider()
            SwitchPreference(
                checked = autoSpeak,
                onCheckedChange = { enabled ->
                    scope.launch { settings.setAutoSpeak(enabled) }
                    if (!enabled) AppGraph.voice.stop()
                },
                title = "自动朗读",
                summary = null,
            )
            HorizontalDivider()
            SwitchPreference(
                checked = voiceReply,
                onCheckedChange = { scope.launch { settings.setVoiceReply(it) } },
                title = "用语音条回复",
                summary = if (voiceReply) "回复会变成可播放的语音条" else null,
            )
            HorizontalDivider()
            var probe by remember { mutableStateOf<String?>(null) }
            var probing by remember { mutableStateOf(false) }
            ArrowPreference(
                title = if (probing) "正在检测…" else "克隆引擎自检",
                summary = probe ?: "点一下确认桥接是否在线",
                onClick = {
                    if (!probing) {
                        probing = true
                        probe = null
                        scope.launch {
                            probe = AppGraph.voice.probeBridge(bridgeUrl)
                            probing = false
                            notify(probe ?: "")
                        }
                    }
                },
            )
            HorizontalDivider()
            Footnote("连不上克隆引擎时自动改用系统语音。")
        }

        section(title = "克隆引擎", key = "bridge") {
            CardBody {
                FsInput(
                    value = bridgeUrl,
                    onValueChange = {
                        bridgeUrl = it
                        scope.launch { settings.setBridgeUrl(it) }
                    },
                    label = "地址",
                    placeholder = SettingsStore.DEFAULT_BRIDGE,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "电脑上运行桥接脚本后填局域网地址。",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 二级页：隐私与数据。 */
@Composable
fun PrivacyDataScreen(
    onBack: () -> Unit,
    onWiped: () -> Unit,
    notify: (String) -> Unit,
) {
    val settings = AppGraph.settings
    val scope = rememberCoroutineScope()
    val privacyAck by settings.privacyAck.collectAsState(initial = false)
    var confirmWipe by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }

    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        restoring = true
        scope.launch {
            val count = com.friendsim.app.core.media.BackupManager.restore(AppGraph.app, uri)
            restoring = false
            notify(if (count > 0) "已恢复 $count 个档案" else "备份文件读不出来")
        }
    }

    var dissolving by remember { mutableStateOf(false) }

    if (confirmWipe) {
        DangerConfirmDialog(
            title = "清除全部数据",
            message = "此操作不可逆 将移除所有档案与记忆",
            confirmText = "清除",
            holdMillis = 2000,
            onDismiss = { confirmWipe = false },
            onCanceledHold = { notify("已取消") },
            onConfirmed = {
                confirmWipe = false
                dissolving = true
            },
        )
    }

    if (dissolving) {
        DissolveEffect(active = true, color = MiuixTheme.colorScheme.primary) {
            scope.launch {
                AppGraph.friends.deleteAll()
                AppGraph.memory.deleteAll()
                dissolving = false
                notify("已清除")
                onWiped()
            }
        }
    }

    FsPage(title = "隐私与数据", onBack = onBack) {
        section(title = "存储", key = "storage") {
            Footnote("照片、录音与聊天记录只在本机，聊天记录加密保存。")
            HorizontalDivider()
            SwitchPreference(
                checked = privacyAck,
                onCheckedChange = { scope.launch { settings.setPrivacyAck(it) } },
                title = "不再提示",
                summary = null,
            )
        }

        section(title = "数据", key = "data") {
            ArrowPreference(
                title = "备份到文件",
                summary = "档案 · 照片 · 录音 · 聊天",
                onClick = {
                    scope.launch {
                        val path = com.friendsim.app.core.media.BackupManager.export(AppGraph.app)
                        notify(if (path != null) "已保存到下载目录" else "备份失败")
                    }
                },
            )
            HorizontalDivider()
            ArrowPreference(
                title = if (restoring) "正在恢复…" else "从备份恢复",
                summary = "选择 zip 备份文件",
                onClick = { restorePicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
            )
            HorizontalDivider()
            ArrowPreference(
                title = "导出全部数据",
                summary = "JSON 备份到下载目录",
                onClick = {
                    scope.launch {
                        val path = com.friendsim.app.core.media.DataExporter.export(AppGraph.app)
                        notify(if (path != null) "已导出到下载目录" else "导出失败")
                    }
                },
            )
            HorizontalDivider()
            ArrowPreference(
                title = "清除全部数据",
                summary = null,
                onClick = { confirmWipe = true },
            )
        }
    }
}

/** 脚注：13sp、灰色、只在必要时出现。 */
@Composable
private fun Footnote(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FsSpacing.CardPadding, vertical = 12.dp),
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}
