package com.friendsim.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friendsim.app.AppGraph
import com.friendsim.app.ui.motion.LocalMotionPrefs
import com.friendsim.app.ui.motion.MotionTokens
import com.friendsim.app.ui.motion.bouncyClickable
import com.friendsim.app.ui.theme.LocalAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class Page(
    val title: String,
    val subtitle: String,
    val action: String,
)

private val pages = listOf(
    Page("遇见", "把那个特别的人 留在身边", "继续"),
    Page("听见", "用一段熟悉的声音 唤醒专属的陪伴", "继续"),
    Page("看见", "一张照片 定格那些未曾褪色的瞬间", "继续"),
    Page("会想起你", "隔了几天 它也会先问一句", "继续"),
    Page("好久不见", "故事 从这里继续", "开启对话"),
)

private val notes = listOf(
    "不用注册 不用填 Key",
    "声音可以之后在档案里补上",
    "照片只留在本机",
    "深夜不会打扰你",
    "从新建一个朋友开始",
)

/**
 * 引导：4 页纯介绍，不填任何东西。
 * 核心视觉用 SharedTransitionLayout 在页间连续形变（圆 → 声波球 → 相框 → 圆点），
 * 文案无标点、用空格断句。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val settings = AppGraph.settings
    val scope = rememberCoroutineScope()
    val motion = LocalMotionPrefs.current
    val haptics = LocalHapticFeedback.current
    val scheme = MiuixTheme.colorScheme

    var step by rememberSaveable { mutableIntStateOf(0) }
    var switching by remember { mutableStateOf(false) }
    val reveal = remember { Animatable(0f) }

    fun go(next: Int) {
        if (switching) return
        switching = true
        if (!motion.reduced) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch {
            step = next
            delay(if (motion.reduced) 120 else 460)
            switching = false
        }
    }

    fun complete() {
        if (switching) return
        switching = true
        scope.launch {
            if (motion.reduced) {
                reveal.snapTo(1f)
            } else {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                reveal.animateTo(1f, tween(480))
            }
            runCatching { settings.setOnboarded(true) }
            onFinish()
        }
    }

    // 第 4 页时后台预热主界面数据，点下去是瞬间出现
    LaunchedEffect(step) {
        if (step == 3) {
            runCatching { AppGraph.friends }
            runCatching { AppGraph.memory }
            runCatching { settings.snapshot() }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(scheme.surface)) {
        BreathingGlow()

        SharedTransitionLayout {
            AnimatedContent(
                targetState = step,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    (fadeIn(tween(if (motion.reduced) 0 else 320)) +
                        slideInVertically(tween(if (motion.reduced) 0 else 360)) { it / 14 }) togetherWith
                        fadeOut(tween(if (motion.reduced) 0 else 220))
                },
                label = "onboarding",
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 32.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StepDots(step = page, motionReduced = motion.reduced)
                    }

                    Spacer(Modifier.weight(1f))

                    with(this@SharedTransitionLayout) {
                        MorphVisual(
                            page = page,
                            accent = LocalAccent.current ?: scheme.primary,
                            modifier = Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "onboard-shape"),
                                animatedVisibilityScope = this@AnimatedContent,
                                boundsTransform = { _, _ -> MotionTokens.bounds() },
                            ),
                        )
                    }

                    Spacer(Modifier.height(40.dp))

                    Text(
                        text = pages[page].title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = pages[page].subtitle,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        color = scheme.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = notes[page],
                        fontSize = 12.sp,
                        color = scheme.onSurfaceVariantSummary.copy(alpha = 0.7f),
                    )

                    Spacer(Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(if (switching) scheme.primary.copy(alpha = 0.4f) else scheme.primary)
                            .bouncyClickable(enabled = !switching) {
                                if (page < pages.lastIndex) go(page + 1) else complete()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = pages[page].action, color = scheme.onPrimary, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    AnimatedVisibility(visible = page == pages.lastIndex, enter = fadeIn(tween(240)), exit = fadeOut(tween(120))) {
                        Text(
                            text = "照片 声音 都可以之后在设置里添加",
                            fontSize = 11.sp,
                            color = scheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }

        // 收尾：主题色从按钮位置铺满全屏
        val progress = reveal.value
        if (progress > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxR = kotlin.math.hypot(size.width.toDouble(), size.height.toDouble()).toFloat()
                drawCircle(
                    color = scheme.primary,
                    radius = maxR * progress,
                    center = Offset(size.width / 2f, size.height * 0.82f),
                )
            }
        }
    }
}

/** 背景呼吸光晕：两块极大的半透明色块，极慢地换色与漂移。 */
@Composable
private fun BreathingGlow() {
    val scheme = MiuixTheme.colorScheme
    val accent = LocalAccent.current ?: scheme.primary
    val transition = rememberInfiniteTransition(label = "glow")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "phase",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.10f + 0.06f * phase), Color.Transparent),
                    center = Offset(size.width * (0.25f + 0.1f * phase), size.height * 0.28f),
                    radius = size.minDimension * 0.75f,
                ),
                radius = size.minDimension * 0.75f,
                center = Offset(size.width * (0.25f + 0.1f * phase), size.height * 0.28f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.07f + 0.05f * (1f - phase)), Color.Transparent),
                    center = Offset(size.width * (0.75f - 0.08f * phase), size.height * 0.72f),
                    radius = size.minDimension * 0.65f,
                ),
                radius = size.minDimension * 0.65f,
                center = Offset(size.width * (0.75f - 0.08f * phase), size.height * 0.72f),
            )
        }
    }
}

/** 一个连续形变的核心视觉：圆 → 声波球 → 相框 → 圆点。 */
@Composable
private fun MorphVisual(page: Int, accent: Color, modifier: Modifier = Modifier) {
    val scheme = MiuixTheme.colorScheme
    val radiusPercent by animateFloatAsState(
        targetValue = when (page) {
            2 -> 24f
            else -> 50f
        },
        animationSpec = MotionTokens.springDefault,
        label = "shapeRadius",
    )
    val scale by animateFloatAsState(
        targetValue = when (page) {
            0 -> 1f
            1 -> 0.94f
            2 -> 1f
            else -> 0.62f
        },
        animationSpec = MotionTokens.springSlow,
        label = "shapeScale",
    )

    Box(
        modifier = modifier
            .size(152.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(radiusPercent))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.90f),
                        accent.copy(alpha = 0.55f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(160)) },
            label = "shapeInner",
        ) { current ->
            when (current) {
                0 -> Text(
                    text = "友",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )

                1 -> SoundBars()
                2 -> FrameGlyph()
                else -> Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.9f)),
                )
            }
        }
    }
}

@Composable
private fun StepDots(step: Int, motionReduced: Boolean) {
    val scheme = MiuixTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(pages.size) { index ->
            val width by animateFloatAsState(
                targetValue = if (index == step) 20f else 6f,
                animationSpec = if (motionReduced) MotionTokens.instant else MotionTokens.springFast,
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .width(width.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (index == step) scheme.primary else scheme.onSurface.copy(alpha = 0.15f)),
            )
        }
    }
}

@Composable
private fun SoundBars() {
    val transition = rememberInfiniteTransition(label = "bars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "barsPhase",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { index ->
            val base = 14f + ((index * 5) % 4) * 9f
            val wave = base + 16f * kotlin.math.abs(
                kotlin.math.sin((phase * 2f * Math.PI).toFloat() + index * 0.7f),
            )
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(wave.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.9f)),
            )
        }
    }
}

@Composable
private fun FrameGlyph() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                style = Stroke(width = 3f),
                topLeft = Offset(8f, 8f),
                size = Size(size.width - 16f, size.height - 16f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 5f,
                center = Offset(size.width * 0.32f, size.height * 0.34f),
            )
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * 0.18f, size.height * 0.80f)
                lineTo(size.width * 0.44f, size.height * 0.52f)
                lineTo(size.width * 0.62f, size.height * 0.70f)
                lineTo(size.width * 0.78f, size.height * 0.58f)
                lineTo(size.width * 0.86f, size.height * 0.80f)
                close()
            }
            drawPath(path, Color.White.copy(alpha = 0.85f))
        }
    }
}
