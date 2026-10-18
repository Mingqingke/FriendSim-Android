package com.friendsim.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.friendsim.app.ui.motion.bouncyClickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val dangerRed = Color(0xFFE5484D)

/**
 * 破坏性操作的居中确认弹窗：
 * 左右平分的大按钮（各 52dp 高），右侧「清除」必须长按 2 秒，
 * 期间有进度填充 + 连续触感，松手即取消。
 */
@Composable
fun DangerConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "清除",
    holdMillis: Int = 2000,
    onDismiss: () -> Unit,
    onCanceledHold: () -> Unit = {},
    onConfirmed: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val haptics = LocalHapticFeedback.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(scheme.surface)
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    color = scheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(scheme.onSurface.copy(alpha = 0.06f))
                            .bouncyClickable(scaleDown = 0.96f) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "取消", fontSize = 15.sp, color = scheme.onSurface)
                    }

                    HoldToConfirm(
                        modifier = Modifier.weight(1f),
                        text = confirmText,
                        holdMillis = holdMillis,
                        onConfirmed = onConfirmed,
                        onCanceled = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCanceledHold()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HoldToConfirm(
    modifier: Modifier,
    text: String,
    holdMillis: Int,
    onConfirmed: () -> Unit,
    onCanceled: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var holding by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(dangerRed.copy(alpha = 0.12f))
            .pointerInput(holdMillis) {
                detectTapGestures(
                    onPress = {
                        if (done) return@detectTapGestures
                        holding = true
                        progress.snapTo(0f)
                        val holdJob = scope.launch {
                            progress.animateTo(1f, tween(holdMillis, easing = LinearEasing))
                            done = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfirmed()
                        }
                        val tickJob = scope.launch {
                            while (progress.value < 1f && holding) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                delay(320)
                            }
                        }
                        tryAwaitRelease()
                        holding = false
                        holdJob.cancel()
                        tickJob.cancel()
                        if (!done) {
                            progress.snapTo(0f)
                            onCanceled()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // 进度填充
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value.coerceIn(0f, 1f))
                .background(dangerRed.copy(alpha = 0.28f)),
        )
        Text(
            text = if (holding) "继续按住" else text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = dangerRed,
        )
    }
}
