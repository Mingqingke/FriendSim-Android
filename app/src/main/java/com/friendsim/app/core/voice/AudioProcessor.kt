package com.friendsim.app.core.voice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs

/**
 * 录音后处理（纯本地、无 ffmpeg）：
 * 1) 切掉开头结尾的静音，避免克隆引擎把空白当参考；
 * 2) 轻量噪声门：低于阈值的样本压到 0，去掉底噪；
 * 3) 返回处理后的有效时长（毫秒），交给界面判断是否太短。
 */
object AudioProcessor {

    private const val SAMPLE_RATE = 16_000
    private const val SILENCE_THRESHOLD = 0.02f

    suspend fun trimAndGate(file: File, gate: Boolean = true): Long = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists() || file.length() < 44L + 3200L) return@runCatching 0L

            val bytes = file.readBytes()
            val dataSize = bytes.size - 44
            val samples = ShortArray(dataSize / 2)
            for (i in samples.indices) {
                val lo = bytes[44 + i * 2].toInt() and 0xFF
                val hi = bytes[44 + i * 2 + 1].toInt()
                samples[i] = ((hi shl 8) or lo).toShort()
            }

            fun loud(index: Int): Boolean = abs(samples[index] / 32768f) > SILENCE_THRESHOLD
            var start = 0
            while (start < samples.size && !loud(start)) start++
            var end = samples.size - 1
            while (end > start && !loud(end)) end--
            if (end <= start) return@runCatching 0L

            val pad = (SAMPLE_RATE * 0.12f).toInt()
            start = (start - pad).coerceAtLeast(0)
            end = (end + pad).coerceAtMost(samples.size - 1)
            val kept = end - start + 1

            val out = ShortArray(kept)
            var peak = 0f
            for (i in 0 until kept) {
                val raw = samples[start + i]
                val v = raw / 32768f
                peak = maxOf(peak, abs(v))
                out[i] = if (gate && abs(v) < SILENCE_THRESHOLD * 0.6f) 0 else raw
            }

            // 整体音量太低就提一点，避免克隆出来发虚
            if (peak in 0.01f..0.12f) {
                val gain = (0.35f / peak).coerceAtMost(6f)
                for (i in out.indices) {
                    out[i] = (out[i] * gain).toInt().coerceIn(-32768, 32767).toShort()
                }
            }

            writeWav(file, out)
            kept * 1000L / SAMPLE_RATE
        }.getOrDefault(0L)
    }

    private fun writeWav(file: File, samples: ShortArray) {
        val dataSize = samples.size * 2
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.write(header(dataSize))
            val buffer = ByteArray(8192)
            var index = 0
            while (index < samples.size) {
                var i = 0
                while (i < buffer.size / 2 && index < samples.size) {
                    val s = samples[index].toInt()
                    buffer[i * 2] = (s and 0xFF).toByte()
                    buffer[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                    i++
                    index++
                }
                raf.write(buffer, 0, i * 2)
            }
        }
    }

    private fun header(dataLength: Int): ByteArray {
        val byteRate = SAMPLE_RATE * 2
        val h = ByteArray(44)
        fun ascii(offset: Int, text: String) = text.forEachIndexed { i, c -> h[offset + i] = c.code.toByte() }
        fun int(offset: Int, v: Int) {
            h[offset] = (v and 0xFF).toByte()
            h[offset + 1] = ((v shr 8) and 0xFF).toByte()
            h[offset + 2] = ((v shr 16) and 0xFF).toByte()
            h[offset + 3] = ((v shr 24) and 0xFF).toByte()
        }
        fun short(offset: Int, v: Int) {
            h[offset] = (v and 0xFF).toByte()
            h[offset + 1] = ((v shr 8) and 0xFF).toByte()
        }
        ascii(0, "RIFF")
        int(4, 36 + dataLength)
        ascii(8, "WAVE")
        ascii(12, "fmt ")
        int(16, 16)
        short(20, 1)
        short(22, 1)
        int(24, SAMPLE_RATE)
        int(28, byteRate)
        short(32, 2)
        short(34, 16)
        ascii(36, "data")
        int(40, dataLength)
        return h
    }
}
