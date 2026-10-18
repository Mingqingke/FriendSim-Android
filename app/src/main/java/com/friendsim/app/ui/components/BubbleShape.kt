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

/**
 * iMessage 级气泡：
 * - 连续圆角（大 / 中 / 小三档），靠尾巴一侧的下角收窄
 * - 尾巴是向外舒展的水滴弧（两次二阶贝塞尔），宽 12dp、高 14dp
 * - 一串消息里只有最后一条带尾巴，中间用收窄角模拟气泡串
 * - 不使用阴影，扁平纯净
 */
class BubbleShape(
    private val mine: Boolean,
    private val iMessage: Boolean,
    private val radius: Dp = 18.dp,
    private val showTail: Boolean = true,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (!iMessage) {
            return RoundedCornerShape(radius).createOutline(size, layoutDirection, density)
        }

        val r = with(density) { radius.toPx() }.coerceAtLeast(2f)
        val small = with(density) { (radius.value * 0.33f).dp.toPx() }.coerceAtLeast(3f)
        val tw = with(density) { 12.dp.toPx() }
        val th = with(density) { 14.dp.toPx() }

        // 尾巴占用的水平空间
        val space = if (showTail) tw else 0f
        val left = if (mine) 0f else space
        val right = if (mine) size.width - space else size.width
        val h = size.height

        // 靠发送者一侧的角收窄，另一侧保持大圆角
        val topLeft = if (mine) r else small
        val topRight = if (mine) small else r
        val bottomLeft = if (mine) r else if (showTail) small else small
        val bottomRight = if (mine) if (showTail) small else small else r

        val path = Path()

        if (mine) {
            path.moveTo(left + topLeft, 0f)
            path.lineTo(right - topRight, 0f)
            path.quadraticBezierTo(right, 0f, right, topRight)
            path.lineTo(right, h - th)
            if (showTail) {
                path.quadraticBezierTo(
                    right + tw * 0.55f, h - th * 0.30f,
                    right + tw * 0.45f, h - th * 0.02f,
                )
                path.quadraticBezierTo(
                    right + tw * 0.05f, h,
                    right - tw * 0.85f, h,
                )
            } else {
                path.quadraticBezierTo(right, h, right - bottomRight, h)
            }
            path.lineTo(left + bottomLeft, h)
            path.quadraticBezierTo(left, h, left, h - bottomLeft)
            path.lineTo(left, topLeft)
            path.quadraticBezierTo(left, 0f, left + topLeft, 0f)
            path.close()
        } else {
            path.moveTo(left + topLeft, 0f)
            path.lineTo(right - topRight, 0f)
            path.quadraticBezierTo(right, 0f, right, topRight)
            path.lineTo(right, h - bottomRight)
            path.quadraticBezierTo(right, h, right - bottomRight, h)
            path.lineTo(left + tw * 0.85f, h)
            if (showTail) {
                path.quadraticBezierTo(
                    left + tw * 0.05f, h,
                    left - tw * 0.45f, h - th * 0.02f,
                )
                path.quadraticBezierTo(
                    left - tw * 0.55f, h - th * 0.30f,
                    left, h - th,
                )
            } else {
                path.lineTo(left + bottomLeft, h)
                path.quadraticBezierTo(left, h, left, h - bottomLeft)
            }
            path.lineTo(left, topLeft)
            path.quadraticBezierTo(left, 0f, left + topLeft, 0f)
            path.close()
        }

        return Outline.Generic(path)
    }
}
