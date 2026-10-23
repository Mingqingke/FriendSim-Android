package com.friendsim.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friendsim.app.AppGraph
import com.friendsim.app.core.data.MemoryItem
import com.friendsim.app.core.update.UpdateChecker
import com.friendsim.app.ui.components.FsPage
import com.friendsim.app.ui.components.section
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 记忆条目去掉句末标点和前缀符号，读起来更干净。 */
private fun String.cleanMemoryText(): String =
    trim()
        .removePrefix("·")
        .removePrefix("-")
        .trim()
        .trimEnd('。', '，', ',', '.', '；', ';', '！', '!')
        .trim()

/** 记忆管理中心：按角色查看/删除长期记忆，删掉的不会再进上下文。 */
@Composable
fun MemoryCenterScreen(onBack: () -> Unit, notify: (String) -> Unit) {
    val scheme = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val friendList = AppGraph.friends.friends.collectAsState().value
    var selected by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf(listOf<MemoryItem>()) }
    var revision by remember { mutableIntStateOf(0) }

    val currentId = selected ?: friendList.firstOrNull()?.id

    LaunchedEffect(currentId, revision) {
        items = withContext(Dispatchers.IO) {
            currentId?.let { runCatching { AppGraph.memory.list(it) }.getOrDefault(emptyList()) }
                ?: emptyList()
        }
    }

    FsPage(title = "记忆", bottomPadding = 12.dp, onBack = onBack) {
        item(key = "chips") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                friendList.forEach { f ->
                    val on = f.id == currentId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (on) scheme.primary else scheme.surfaceContainerHigh)
                            .clickable { selected = f.id }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = f.name.ifBlank { "朋友" },
                            fontSize = 13.sp,
                            color = if (on) scheme.onPrimary else scheme.onSurface,
                        )
                    }
                }
            }
        }

        section(title = "长期记忆", key = "mem") {
            if (items.isEmpty()) {
                Text(
                    text = "还没有记忆。多聊几句，它会记住你说的细节",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariantSummary,
                )
            }
            items.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (item.kind == MemoryItem.KIND_EPISODE) scheme.primary else scheme.onSurfaceVariantSummary),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = item.text.cleanMemoryText(),
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "删除",
                        modifier = Modifier.clickable {
                            scope.launch {
                                withContext(Dispatchers.IO) { currentId?.let { AppGraph.memory.remove(it, item.id) } }
                                revision++
                                notify("已删除")
                            }
                        },
                        fontSize = 13.sp,
                        color = scheme.primary,
                    )
                }
            }
        }

        if (items.isNotEmpty()) {
            item(key = "clear") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "清空这个角色的记忆",
                        modifier = Modifier.clickable {
                            scope.launch {
                                withContext(Dispatchers.IO) { currentId?.let { AppGraph.memory.clear(it) } }
                                revision++
                                notify("已清空")
                            }
                        },
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private data class Check(val label: String, val value: String, val ok: Boolean)

/** 运行诊断：四项真检查，检查不到就如实说检查不到。 */
@Composable
fun DiagnosticsScreen(onBack: () -> Unit, notify: (String) -> Unit) {
    val scheme = MiuixTheme.colorScheme
    val appContext = LocalContext.current.applicationContext
    var checks by remember { mutableStateOf(listOf<Check>()) }
    var running by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val result = mutableListOf<Check>()
        val net = withContext(Dispatchers.IO) {
            runCatching {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                client.newCall(okhttp3.Request.Builder().url("https://api.github.com/").build())
                    .execute().use { it.isSuccessful }
            }.getOrDefault(false)
        }
        result += Check("网络", if (net) "可以连上" else "连不上，检查网络或代理", net)

        val cfg = AppGraph.settings.snapshot()
        result += Check(
            "AI 接口",
            when {
                cfg.apiKey.isNotBlank() -> "已填写 Key"
                cfg.offlineOnly -> "仅离线模式"
                cfg.allowPublic -> "使用公共端点"
                else -> "未配置，走离线规则"
            },
            cfg.apiKey.isNotBlank() || cfg.allowPublic || cfg.offlineOnly,
        )

        val upd = withContext(Dispatchers.IO) { runCatching { UpdateChecker.check() }.getOrNull() }
        result += Check(
            "更新服务",
            upd?.let { "有新版本 ${it.version}" } ?: "已是最新或暂时连不上",
            true,
        )

        val freeMb = withContext(Dispatchers.IO) {
            runCatching {
                android.os.StatFs(appContext.filesDir.path).availableBytes / (1024 * 1024)
            }.getOrDefault(0L)
        }
        result += Check("存储", "${freeMb} MB 可用", freeMb > 50)

        checks = result
        running = false
    }

    FsPage(title = "诊断", bottomPadding = 12.dp, onBack = onBack) {
        section(title = "运行状态", key = "diag") {
            checks.forEachIndexed { index, c ->
                if (index > 0) HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (c.ok) scheme.primary else Color(0xFFE5484D)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(text = c.label, fontSize = 14.sp, modifier = Modifier.width(76.dp))
                    Text(
                        text = c.value,
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariantSummary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (running) {
            item(key = "running") {
                Text(
                    text = "检查中…",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariantSummary,
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
