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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friendsim.app.AppGraph
import com.friendsim.app.core.data.MemoryItem
import com.friendsim.app.ui.components.FsPage
import com.friendsim.app.ui.components.section
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class Draft(
    val style: String = "",
    val catchphrase: String = "",
    val likes: String = "",
    val dislikes: String = "",
    val relationship: String = "",
    val memories: List<String> = emptyList(),
)

/** 粘贴一段聊天记录，交给模型提炼人物档案，再写进角色和记忆。 */
@Composable
fun ImportChatScreen(onBack: () -> Unit, notify: (String) -> Unit) {
    val scheme = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val friends = AppGraph.friends.friends.collectAsState().value
    var target by remember { mutableStateOf<String?>(null) }
    var raw by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf<Draft?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentId = target ?: friends.firstOrNull()?.id

    FsPage(title = "导入聊天记录", bottomPadding = 12.dp, onBack = onBack) {
        item(key = "hint") {
            Text(
                text = "把和 TA 的聊天记录粘贴进来，会提炼说话风格、喜好、雷区和共同经历，写进选中的角色。分析需要联网。",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                fontSize = 12.5.sp,
                color = scheme.onSurfaceVariantSummary,
            )
        }
        item(key = "input") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surfaceContainerHigh)
                    .padding(14.dp),
            ) {
                BasicTextField(
                    value = raw,
                    onValueChange = { raw = it.take(20000) },
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    textStyle = TextStyle(fontSize = 13.sp, color = scheme.onSurface),
                )
                if (raw.isBlank()) {
                    Text(
                        text = "TA: 今天加班到十点\n我: 又加班\nTA: 习惯了 反正回去也没人等",
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariantSummary.copy(alpha = 0.6f),
                    )
                }
            }
        }
        item(key = "target") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                friends.forEach { f ->
                    val on = f.id == currentId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (on) scheme.primary else scheme.surfaceContainerHigh)
                            .clickable { target = f.id }
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
        item(key = "run") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (busy || raw.isBlank()) scheme.primary.copy(alpha = 0.5f) else scheme.primary)
                    .clickable(enabled = !busy && raw.isNotBlank() && currentId != null) {
                        busy = true
                        error = null
                        draft = null
                        scope.launch {
                            val text = withContext(Dispatchers.IO) {
                                runCatching { AppGraph.engine.analyzePersona(raw, AppGraph.settings.snapshot()) }.getOrNull()
                            }
                            busy = false
                            if (text == null) {
                                error = "分析失败：接口不可用或没返回值"
                                return@launch
                            }
                            val parsed = parseDraft(text)
                            if (parsed == null) {
                                error = "返回格式看不懂，换个模型或缩短记录再试"
                            } else {
                                draft = parsed
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (busy) "分析中…" else "分析",
                    fontSize = 15.sp,
                    color = scheme.onPrimary,
                )
            }
        }
        error?.let { msg ->
            item(key = "err") {
                Text(
                    text = msg,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 12.5.sp,
                    color = scheme.error,
                )
            }
        }
        draft?.let { d ->
            section(title = "提炼结果", key = "draft") {
                listOfNotNull(
                    d.relationship.takeIf { it.isNotBlank() }?.let { "关系：$it" },
                    d.style.takeIf { it.isNotBlank() }?.let { "说话风格：$it" },
                    d.catchphrase.takeIf { it.isNotBlank() }?.let { "口头禅：$it" },
                    d.likes.takeIf { it.isNotBlank() }?.let { "喜欢：$it" },
                    d.dislikes.takeIf { it.isNotBlank() }?.let { "雷区：$it" },
                ).forEach { line ->
                    Text(
                        text = line,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                        fontSize = 13.5.sp,
                    )
                }
                d.memories.forEach { m ->
                    Text(
                        text = "· $m",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariantSummary,
                    )
                }
            }
            item(key = "apply") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.primary)
                        .clickable {
                            val id = currentId ?: return@clickable
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    runCatching {
                                        val f = AppGraph.friends.find(id)
                                        if (f != null) {
                                            if (d.relationship.isNotBlank()) f.relationship = d.relationship
                                            if (d.style.isNotBlank()) f.persona = d.style
                                            if (d.catchphrase.isNotBlank()) f.catchphrase = d.catchphrase
                                            if (d.likes.isNotBlank()) f.likes = d.likes
                                            if (d.dislikes.isNotBlank()) f.dislikes = d.dislikes
                                            AppGraph.friends.upsert(f)
                                        }
                                        d.memories.forEach { m ->
                                            runCatching { AppGraph.memory.add(id, m, MemoryItem.KIND_EPISODE) }
                                        }
                                    }
                                }
                                draft = null
                                raw = ""
                                notify("已写入档案")
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "写进这个角色", fontSize = 15.sp, color = scheme.onPrimary)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** 模型可能带代码块或前后废话，先剥掉再解析；解析失败返回 null。 */
private fun parseDraft(raw: String): Draft? = runCatching {
    val start = raw.indexOf('{')
    val end = raw.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    val obj = Json.parseToJsonElement(raw.substring(start, end + 1)).jsonObject
    fun str(k: String) = obj[k]?.jsonPrimitive?.content?.trim().orEmpty()
    val mem = obj["memories"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content?.trim() } ?: emptyList()
    val d = Draft(str("style"), str("catchphrase"), str("likes"), str("dislikes"), str("relationship"), mem.filter { it.isNotBlank() }.take(8))
    if (d.style.isBlank() && d.memories.isEmpty()) null else d
}.getOrNull()
