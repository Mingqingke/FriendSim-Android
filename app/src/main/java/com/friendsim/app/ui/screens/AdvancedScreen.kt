package com.friendsim.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.friendsim.app.core.llm.EndpointPool
import com.friendsim.app.core.llm.OpenAiClient
import com.friendsim.app.ui.components.CardBody
import com.friendsim.app.ui.components.FsInput
import com.friendsim.app.ui.components.FsPage
import com.friendsim.app.ui.components.FsSpacing
import com.friendsim.app.ui.components.MetaRow
import com.friendsim.app.ui.components.section
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

/** 三级页：高级设置（Key / Base URL / 模型 / 连通性验证 + 端点状态）。 */
@Composable
fun AdvancedScreen(onBack: () -> Unit, notify: (String) -> Unit) {
    val settings = AppGraph.settings
    val scope = rememberCoroutineScope()

    var key by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf(SettingsStore.DEFAULT_BASE_URL) }
    var model by remember { mutableStateOf(SettingsStore.DEFAULT_MODEL) }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }

    // 填完 Key 静默自检（1 token），失败直接提示，不用等聊天才报错
    LaunchedEffect(key) {
        if (key.isBlank()) return@LaunchedEffect
        delay(1200)
        if (testing) return@LaunchedEffect
        checking = true
        val outcome = OpenAiClient().ping(baseUrl.trim(), key.trim(), model.trim())
        checking = false
        result = outcome.fold(
            onSuccess = { it },
            onFailure = { "Key 好像不对哦" },
        )
        AppGraph.settings.setKeyValid(outcome.isSuccess)
    }

    LaunchedEffect(Unit) {
        val cfg = settings.snapshot()
        key = cfg.apiKey
        baseUrl = cfg.baseUrl
        model = cfg.model
    }

    FsPage(
        title = "高级设置",
        
        onBack = onBack,
    ) {
        section(title = "API 密钥", key = "key") {
            CardBody {
                FsInput(
                    value = key,
                    onValueChange = {
                        key = it
                        scope.launch { settings.setApiKey(it) }
                    },
                    label = "API 密钥",
                    placeholder = "sk-…",
                )
                Spacer(Modifier.height(FsSpacing.Item))
                FsInput(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        scope.launch { settings.setBaseUrl(it) }
                    },
                    label = "接口地址",
                    placeholder = SettingsStore.DEFAULT_BASE_URL,
                )
                Spacer(Modifier.height(FsSpacing.Item))
                FsInput(
                    value = model,
                    onValueChange = {
                        model = it
                        scope.launch { settings.setModel(it) }
                    },
                    label = "模型",
                    placeholder = SettingsStore.DEFAULT_MODEL,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "deepseek-flash" to "Flash · 能看图",
                        "deepseek-v4-pro" to "V4 Pro · 纯文本",
                    ).forEach { (id, label) ->
                        val on = model == id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (on) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHigh)
                                .clickable {
                                    model = id
                                    scope.launch { settings.setModel(id) }
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.5.sp,
                                color = if (on) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(FsSpacing.Item))
                Button(
                    onClick = {
                        if (testing) return@Button
                        testing = true
                        result = ""
                        scope.launch {
                            val outcome = OpenAiClient().ping(baseUrl.trim(), key.trim(), model.trim())
                            testing = false
                            result = outcome.fold(
                                onSuccess = { it },
                                onFailure = { "失败：${it.message?.take(50) ?: "未知"}" },
                            )
                            if (result.startsWith("连接正常")) notify("Key 可用")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = if (testing) "验证中…" else "测试连接")
                }
                if (checking) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "正在检查密钥…",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                if (result.isNotEmpty() && !checking) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = result,
                        fontSize = 12.sp,
                        color = if (result.startsWith("连接正常")) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantSummary
                        },
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "密钥仅保存在本机。",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        if (com.friendsim.app.BuildConfig.DEBUG) {
            section(title = "调试", key = "debug") {
                ArrowPreference(
                    title = "网络日志",
                    summary = "查看最近请求",
                    onClick = {
                        val ok = com.friendsim.app.core.net.openDebugConsole(AppGraph.app)
                        if (!ok) notify("打不开日志页")
                    },
                )
            }
        }

        section(title = "端点", key = "endpoints") {
            EndpointPool.public.forEachIndexed { index, candidate ->
                if (index > 0) HorizontalDivider()
                MetaRow(
                    label = candidate.label,
                    value = if (EndpointPool.isAvailable(candidate.id)) "可用" else "冷却中",
                )
            }
        }

    }
}
