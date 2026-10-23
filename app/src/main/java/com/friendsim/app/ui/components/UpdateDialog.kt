package com.friendsim.app.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.friendsim.app.core.update.UpdateChecker
import com.friendsim.app.ui.motion.bouncyClickable
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 应用内更新：显示更新内容 → 下载（进度条）→ 拉起系统安装。 */
@Composable
fun UpdateDialog(
    release: UpdateChecker.Release,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MiuixTheme.colorScheme

    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var file by remember { mutableStateOf<java.io.File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var rateText by remember { mutableStateOf("") }
    var paused by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.surface)
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Text(text = "发现新版本 v${release.version}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "当前 v${UpdateChecker.currentVersion()}",
                fontSize = 12.sp,
                color = scheme.onSurfaceVariantSummary,
            )
            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = plainNotes(release.notes).ifBlank { "修复了一些已知问题" },
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = scheme.onSurface.copy(alpha = 0.85f),
                )
            }

            Spacer(Modifier.height(18.dp))

            if (downloading || progress > 0f) {
                WaveProgress(progress = progress, color = scheme.primary)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when {
                            file != null -> "下载完成"
                            paused -> "已暂停 · 继续会从断点接着下"
                            else -> "正在下载 ${(progress * 100).toInt()}%$rateText"
                        },
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        color = scheme.onSurfaceVariantSummary,
                    )
                    if (downloading && file == null) {
                        Text(
                            text = "暂停",
                            modifier = Modifier.bouncyClickable(scaleDown = 0.94f) {
                                job?.cancel()
                                job = null
                                downloading = false
                                paused = true
                            },
                            fontSize = 12.sp,
                            color = scheme.primary,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            error?.let {
                Text(text = it, fontSize = 12.sp, color = scheme.error)
                Spacer(Modifier.height(10.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.onSurface.copy(alpha = 0.06f))
                        .bouncyClickable(enabled = !downloading, scaleDown = 0.96f) { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "稍后", fontSize = 15.sp, color = scheme.onSurface)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (downloading) scheme.primary.copy(alpha = 0.5f) else scheme.primary,
                        )
                        .bouncyClickable(enabled = !downloading, scaleDown = 0.96f) {
                            val ready = file
                            if (ready != null) {
                                runCatching { installApk(context, ready) }
                                onDismiss()
                            } else {
                                downloading = true
                                paused = false
                                error = null
                                job = scope.launch {
                                    val result = UpdateChecker.download(
                                        context = context,
                                        release = release,
                                        onProgress = { p -> progress = p },
                                        onRate = { bps, eta ->
                                            val kb = bps / 1024
                                            rateText = if (kb > 0) {
                                                val etaPart = if (eta > 0) " · 剩 ${eta / 1000}s" else ""
                                                " · $kb KB/s$etaPart"
                                            } else ""
                                        },
                                    )
                                    downloading = false
                                    file = result
                                    if (result == null) {
                                        error = "下载失败，稍后再试"
                                        progress = 0f
                                        rateText = ""
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            downloading -> "下载中"
                            file != null -> "安装"
                            paused -> "继续下载"
                            else -> "立即更新"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onPrimary,
                    )
                }
            }
        }
    }
}

/** 波浪进度：正弦水面随下载推进。 */
@Composable
private fun WaveProgress(progress: Float, color: Color) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val p = progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.08f)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            val waterY = h * (1f - p)
            val amp = 7f * density

            // 后层水
            drawPath(
                buildWave(w, h, waterY, amp * 0.8f, phase * -0.7f + 1.2f, 2.2f),
                color.copy(alpha = 0.22f),
            )
            // 前层水
            drawPath(
                buildWave(w, h, waterY, amp, phase, 3.2f),
                color.copy(alpha = 0.45f),
            )
            // 波峰高光线
            drawPath(
                buildCrest(w, waterY, amp, phase, 3.2f),
                color.copy(alpha = 0.9f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f * density),
            )
        }
        Text(
            text = "${(p * 100).toInt()}%",
            modifier = Modifier.align(Alignment.Center),
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
    }
}

private fun buildWave(w: Float, h: Float, waterY: Float, amp: Float, phase: Float, cycles: Float): Path =
    Path().apply {
        moveTo(0f, waterY)
        var x = 0f
        while (x <= w) {
            val y = waterY + kotlin.math.sin(x / w * cycles * Math.PI.toFloat() + phase) * amp
            lineTo(x, y)
            x += 4f
        }
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }

private fun buildCrest(w: Float, waterY: Float, amp: Float, phase: Float, cycles: Float): Path =
    Path().apply {
        moveTo(0f, waterY + kotlin.math.sin(phase) * amp)
        var x = 0f
        while (x <= w) {
            val y = waterY + kotlin.math.sin(x / w * cycles * Math.PI.toFloat() + phase) * amp
            lineTo(x, y)
            x += 4f
        }
    }

/** 把 release 正文里的 markdown 符号去掉，只留干净的短句。 */
private fun plainNotes(raw: String): String {
    if (raw.isBlank()) return ""
    return raw.lineSequence()
        .map { line ->
            line.trim()
                .removePrefix("###")
                .removePrefix("##")
                .removePrefix("#")
                .removePrefix("- [ ]")
                .removePrefix("- ")
                .replace("**", "")
                .replace("`", "")
                .trim()
        }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
}

private fun installApk(context: Context, file: java.io.File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
