package com.friendsim.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * iMessage 风格气泡（v2.0 重写）。
 *
 * 几何约定：
 * - 尾巴侧预留 [TAIL_W] 的缺口，正文区不被裁切；
 * - 尾巴从侧边 12dp 高度处切出，用三段三次贝塞尔收成水滴尖，
 *   起始切点与侧边相切，因此不会出现折角或接缝；
 * - 连续消息只有最后一条带尾巴，其余用收窄的内角（8dp）表现「同一串」；
 * - 路径全部落在 bounds 内，不会被 clip 截断。
 */
class BubbleShape(
    private val mine: Boolean,
    private val iMessage: Boolean,
    private val radius: Dp = 18.dp,
    private val showTail: Boolean = true,
) : Shape {

    companion object {
        const val TAIL_W = 9f   // dp
        const val TAIL_H = 12f  // dp
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (!iMessage) {
            return RoundedCornerShape(radius).createOutline(size, layoutDirection, density)
        }

        val r = with(density) { radius.toPx() }
        val inner = with(density) { 8.dp.toPx() }
        val tw = with(density) { TAIL_W.dp.toPx() }
        val th = with(density) { TAIL_H.dp.toPx() }

        val w = size.width
        val h = size.height
        val space = if (showTail) tw else 0f

        // 正文区（尾巴占的那一侧收进来）
        val left = if (mine) 0f else space
        val right = if (mine) w - space else w

        // 圆角：外侧两角大，内侧两角小（同一串消息的收窄感）
        val rTL = if (mine) inner else r
        val rTR = if (mine) r else inner
        val rBL = if (mine) inner else r
        val rBR = if (mine) r else inner

        val maxR = min(w, h) / 2f
        val rt = rTL.coerceAtMost(maxR)
        val rtr = rTR.coerceAtMost(maxR)
        val rb = rBL.coerceAtMost(maxR)
        val rbr = rBR.coerceAtMost(maxR)

        val path = Path()

        if (mine) {
            // 顶边
            path.moveTo(left + rt, 0f)
            path.lineTo(right - rtr, 0f)
            path.cubicTo(right - rtr * 0.45f, 0f, right, rtr * 0.45f, right, rtr)
            // 右边向下
            val tailTop = if (showTail) (h - th).coerceAtLeast(rtr + th * 0.4f) else (h - rbr)
            path.lineTo(right, tailTop)
            if (showTail) {
                // 水滴尾巴：与侧边相切 → 向外 → 收尖 → 回到下边
                path.cubicTo(
                    right + tw * 0.35f, tailTop + th * 0.35f,
                    right + tw * 0.95f, h - th * 0.42f,
                    right + tw, h,
                )
                path.cubicTo(
                    right + tw * 0.28f, h - th * 0.06f,
                    right - tw * 1.1f, h,
                    right - tw * 2.0f, h,
                )
            } else {
                path.cubicTo(right, h - rbr * 0.45f, right - rbr * 0.45f, h, right - rbr, h)
            }
            // 下边向左
            path.lineTo(left + rb, h)
            path.cubicTo(left + rb * 0.45f, h, left, h - rb * 0.45f, left, h - rb)
            // 左边向上
            path.lineTo(left, rt)
            path.cubicTo(left, rt * 0.45f, left + rt * 0.45f, 0f, left + rt, 0f)
        } else {
            path.moveTo(left + rt, 0f)
            path.lineTo(right - rtr, 0f)
            path.cubicTo(right - rtr * 0.45f, 0f, right, rtr * 0.45f, right, rtr)
            path.lineTo(right, h - rbr)
            path.cubicTo(right, h - rbr * 0.45f, right - rbr * 0.45f, h, right - rbr, h)
            // 下边向左，到尾巴起点
            val tailTop = if (showTail) (h - th).coerceAtLeast(rt + th * 0.4f) else (h - rb)
            path.lineTo(left + (if (showTail) tw * 2.0f else rb), h)
            if (showTail) {
                path.cubicTo(
                    left + tw * 1.1f, h,
                    left - tw * 0.28f, h - th * 0.06f,
                    left - tw, h,
                )
                path.cubicTo(
                    left - tw * 0.95f, h - th * 0.42f,
                    left - tw * 0.35f, tailTop + th * 0.35f,
                    left, tailTop,
                )
            } else {
                path.cubicTo(left + rb * 0.45f, h, left, h - rb * 0.45f, left, h - rb)
            }
            path.lineTo(left, rt)
            path.cubicTo(left, rt * 0.45f, left + rt * 0.45f, 0f, left + rt, 0f)
        }

        path.close()
        return Outline.Generic(path)
    }
}
