package com.friendsim.app.ui.motion

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 全应用唯一的动画参数来源（MotionTokens）。
 * 物理层只允许从这里取 spring：default / fast / slow / bouncy 四档，页面不得自带参数。
 */
object MotionTokens {
    /** 主转场 / 卡片入场 / 气泡入场。 */
    val springDefault = spring<Float>(dampingRatio = 0.92f, stiffness = 240f)
    /** 按钮、标签等即时反馈。 */
    val springFast = spring<Float>(dampingRatio = 0.9f, stiffness = 460f)
    /** 大块内容缓慢落位（引导页、启动页）。 */
    val springSlow = spring<Float>(dampingRatio = 0.9f, stiffness = 130f)
    /** 果冻回弹（bouncyClickable 专用）。 */
    val springBouncy = spring<Float>(dampingRatio = 0.62f, stiffness = 360f)

    /** 长按菜单：果冻弹出（M3 Expressive 参数）。 */
    val springMenu = spring<Float>(dampingRatio = 0.7f, stiffness = 400f)

    val springOffset: SpringSpec<IntOffset> = spring(dampingRatio = 0.92f, stiffness = 240f)

    val fade: FiniteAnimationSpec<Float> = tween(durationMillis = 300)
    val quickFade: FiniteAnimationSpec<Float> = tween(durationMillis = 200)

    /** 系统关闭动画时的瞬时规格。 */
    val instant: FiniteAnimationSpec<Float> = tween(durationMillis = 0)

    /** 页面共享元素边界动画规格。 */
    fun bounds() = spring<androidx.compose.ui.geometry.Rect>(dampingRatio = 0.92f, stiffness = 220f)
}

/** 系统「减少动画」开关。 */
@Stable
data class MotionPrefs(val reduced: Boolean)

val LocalMotionPrefs = compositionLocalOf { MotionPrefs(reduced = false) }

@Composable
fun rememberMotionPrefs(): MotionPrefs {
    val context = LocalContext.current
    val scale = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }
    return remember(scale) { MotionPrefs(reduced = scale == 0f) }
}

/** 取当前生效的 spring（关闭动画时退化为瞬时）。 */
fun MotionPrefs.spec(spec: FiniteAnimationSpec<Float>): FiniteAnimationSpec<Float> =
    if (reduced) MotionTokens.instant else spec

/**
 * 统一按压反馈：按下缩到 [scaleDown]，释放果冻回弹。
 * 全应用只用这一套，不与涟漪混用。
 */
@Composable
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.92f,
    onClick: () -> Unit,
): Modifier {
    val prefs = LocalMotionPrefs.current
    val hapticsEnabled = com.friendsim.app.ui.theme.LocalHapticsEnabled.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !prefs.reduced) scaleDown else 1f,
        animationSpec = if (prefs.reduced) MotionTokens.instant else MotionTokens.springBouncy,
        label = "bouncy",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = interaction,
            indication = null,
            onClick = {
                if (hapticsEnabled) {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                }
                onClick()
            },
        )
}

/**
 * iOS 风格惯性滚动：指数衰减 + 低摩擦，手势松手后仍继续滑行，
 * 滑到边界由 overscroll 处理，不重写列表逻辑，只换 flingBehavior 一个参数。
 */
class IosFlingBehavior(private val spec: DecayAnimationSpec<Float>) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (abs(initialVelocity) < 24f) return initialVelocity
        var lastValue = 0f
        var leftover = initialVelocity
        AnimationState(initialValue = 0f, initialVelocity = initialVelocity).animateDecay(spec) {
            val delta = value - lastValue
            val consumed = scrollBy(delta)
            lastValue = value
            leftover = velocity
            if (abs(delta - consumed) > 0.5f) cancelAnimation()
        }
        return leftover
    }
}

@Composable
fun rememberIosFling(): FlingBehavior {
    val density = LocalDensity.current
    return remember(density) {
        IosFlingBehavior(exponentialDecay(frictionMultiplier = 0.85f, absVelocityThreshold = 5f))
    }
}

/**
 * iOS 式边界回弹：拉伸到顶/底时橡皮筋阻尼。
 * 直接用 Compose 官方 overscroll 能力，不额外引库。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Modifier.rubberBand(): Modifier {
    val effect = androidx.compose.foundation.rememberOverscrollEffect()
    return this.overscroll(effect)
}

/** 一次性入场（气泡 / 卡片）：scaleIn(0.9) + fadeIn，统一 spring。 */
@Composable
fun Entrance(
    modifier: Modifier = Modifier,
    initialScale: Float = 0.9f,
    content: @Composable () -> Unit,
) {
    val prefs = LocalMotionPrefs.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (prefs.reduced) {
            fadeIn(MotionTokens.instant)
        } else {
            scaleIn(animationSpec = MotionTokens.springDefault, initialScale = initialScale) +
                fadeIn(MotionTokens.springDefault)
        },
        exit = if (prefs.reduced) fadeOut(MotionTokens.instant)
        else scaleOut(MotionTokens.springFast, targetScale = 0.98f) + fadeOut(MotionTokens.quickFade),
    ) { content() }
}

/** 一次性粒子消散（仅用于删除档案确认反馈，不常驻）。 */
@Composable
fun DissolveEffect(active: Boolean, color: Color, onFinished: () -> Unit) {
    if (!active) return
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 620))
        onFinished()
    }
    val p = progress.value
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = sqrt(size.width * size.width + size.height * size.height) / 2f
        repeat(56) { i ->
            val angle = (i * 137.508f) * (Math.PI / 180f).toFloat()
            val phase = (i % 7) / 7f
            val dist = maxR * easeOut(p, phase) * (0.25f + phase * 0.55f)
            val r = (2.2f + (i % 5)) * (1f - p * 0.85f) * density
            drawCircle(
                color = color.copy(alpha = (1f - p).coerceIn(0f, 1f) * 0.55f),
                radius = r,
                center = Offset(cx + cos(angle) * dist, cy + sin(angle) * dist * 0.8f),
            )
        }
        drawCircle(
            color = color.copy(alpha = (1f - p) * 0.10f),
            radius = 48.dp.toPx() * (1f + p * 4f),
            center = Offset(cx, cy),
        )
    }
}

private fun easeOut(t: Float, offset: Float): Float {
    val v = ((t - offset * 0.3f) / (1f - offset * 0.3f)).coerceIn(0f, 1f)
    return 1f - (1f - v) * (1f - v)
}

/** 展开式容器（用于聊天输入区上方的离线条等小提示，一次性）。 */
@Composable
fun SoftVisibility(visible: Boolean, content: @Composable () -> Unit) {
    val prefs = LocalMotionPrefs.current
    AnimatedVisibility(
        visible = visible,
        enter = if (prefs.reduced) fadeIn(MotionTokens.instant)
        else fadeIn(MotionTokens.fade) + expandIn(
            animationSpec = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntSize>(
                dampingRatio = 0.85f,
                stiffness = 350f,
            ),
        ),
        exit = fadeOut(MotionTokens.quickFade),
    ) { content() }
}
