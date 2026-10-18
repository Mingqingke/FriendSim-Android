package com.friendsim.app.core.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.friendsim.app.core.model.ChatMessage
import com.friendsim.app.core.model.Friend
import com.friendsim.app.core.model.Roles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 聊天长图导出：手动排版渲染成一张 iMessage 风格的 PNG，保存到相册。
 * 不依赖截图，长对话也不会被 View 高度限制。
 */
object ChatExporter {

    private const val W = 1080
    private const val PAD = 48f
    private const val BUBBLE_PAD_H = 28f
    private const val BUBBLE_PAD_V = 20f
    private const val BUBBLE_RADIUS = 30f
    private const val LINE_GAP = 10f

    suspend fun export(
        context: Context,
        friend: Friend,
        messages: List<ChatMessage>,
        mineColorHex: String,
        friendColorHex: String,
        dark: Boolean,
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val visible = messages.filter { it.role != Roles.NOTICE }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (dark) Color.WHITE else Color.BLACK
                textSize = 46f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (dark) 0xFF8E8E93.toInt() else 0xFF8E8E93.toInt()
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 40f
                color = Color.BLACK
            }
            val sysPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 28f
                color = 0xFF8E8E93.toInt()
                textAlign = Paint.Align.CENTER
            }

            val maxBubbleW = W - PAD * 2 - 140f
            val mine = parse(mineColorHex, 0xFF007AFF.toInt())
            val theirs = parse(friendColorHex, if (dark) 0xFF2C2C2E.toInt() else 0xFFE5E5EA.toInt())
            val bg = if (dark) 0xFF000000.toInt() else 0xFFF7F7F9.toInt()

            // 先算高度
            data class Block(val msg: ChatMessage, val lines: List<String>, val h: Float, val w: Float)
            val blocks = mutableListOf<Block>()
            visible.forEach { msg ->
                when (msg.role) {
                    Roles.SYSTEM -> {
                        blocks += Block(msg, listOf(msg.text), 56f, W.toFloat())
                    }

                    else -> {
                        val text = if (msg.kind == ChatMessage.KIND_VOICE) "🎤 ${(msg.durationMs / 1000).coerceAtLeast(1)}″" else msg.text
                        val lines = wrap(text, bodyPaint, maxBubbleW - BUBBLE_PAD_H * 2)
                        val lineHeight = bodyPaint.textSize * 1.35f
                        val h = lines.size * lineHeight + BUBBLE_PAD_V * 2
                        val w = lines.maxOfOrNull { bodyPaint.measureText(it) }?.plus(BUBBLE_PAD_H * 2) ?: 120f
                        blocks += Block(msg, lines, h, w.coerceAtMost(maxBubbleW))
                    }
                }
            }

            val headerH = 210f
            val contentH = blocks.sumOf { (it.h + LINE_GAP + 14f).toDouble() }.toFloat()
            val height = (headerH + contentH + PAD * 2).toInt().coerceAtMost(12000)

            val bitmap = Bitmap.createBitmap(W, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(bg)

            // 头部
            canvas.drawText(friend.name.ifBlank { "朋友" }, W / 2f, 110f, titlePaint)
            canvas.drawText("聊天记录", W / 2f, 160f, subPaint)

            var y = headerH
            blocks.forEach { block ->
                if (block.msg.role == Roles.SYSTEM) {
                    canvas.drawText(block.msg.text, W / 2f, y + 34f, sysPaint)
                    y += block.h + LINE_GAP
                    return@forEach
                }
                val isMine = block.msg.role == Roles.USER
                val bubbleW = (block.w + 8f).coerceAtMost(maxBubbleW + 8f)
                val left = if (isMine) W - PAD - bubbleW else PAD
                val rect = RectF(left, y, left + bubbleW, y + block.h)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isMine) mine else theirs
                }
                val path = Path().apply {
                    addRoundRect(rect, BUBBLE_RADIUS, BUBBLE_RADIUS, Path.Direction.CW)
                }
                canvas.drawPath(path, paint)

                bodyPaint.color = if (isMine) Color.WHITE else if (dark) Color.WHITE else Color.BLACK
                var ty = y + BUBBLE_PAD_V + bodyPaint.textSize
                block.lines.forEach { line ->
                    val tx = left + BUBBLE_PAD_H
                    canvas.drawText(line, tx, ty, bodyPaint)
                    ty += bodyPaint.textSize * 1.35f
                }
                y += block.h + LINE_GAP
            }

            // 落款
            val stamp = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.CHINA).format(Date())
            canvas.drawText(stamp, W / 2f, y + 46f, subPaint)

            save(context, bitmap)
        }.getOrNull()
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val out = mutableListOf<String>()
        var current = StringBuilder()
        text.split('\n').forEach { paragraph ->
            paragraph.forEach { ch ->
                current.append(ch)
                if (paint.measureText(current.toString()) > maxWidth) {
                    current.deleteCharAt(current.length - 1)
                    if (current.isNotEmpty()) out += current.toString()
                    current = StringBuilder().append(ch)
                }
            }
            if (current.isNotEmpty()) {
                out += current.toString()
                current = StringBuilder()
            }
        }
        if (out.isEmpty()) out += ""
        return out
    }

    private fun parse(hex: String, fallback: Int): Int =
        runCatching { Color.parseColor(hex) }.getOrDefault(fallback)

    private fun save(context: Context, bitmap: Bitmap): String? {
        val name = "friendsim_${System.currentTimeMillis()}.png"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FriendSim")
            }
            val resolver = context.contentResolver
            val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) return null
            val stream: OutputStream? = resolver.openOutputStream(uri)
            stream?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            uri.toString()
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FriendSim")
            dir.mkdirs()
            val file = File(dir, name)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file.absolutePath
        }
    }
}
