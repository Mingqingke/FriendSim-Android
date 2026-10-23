package com.friendsim.app.core.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.sqrt

/**
 * 录音：16 kHz / 单声道 / 16 bit PCM，边录边算归一化振幅（0..1），
 * 供界面直接画波形；结束后补 WAV 头落盘。
 */
class Recorder(private val context: Context) {

    val amplitudes = MutableStateFlow<List<Float>>(emptyList())
    val level = MutableStateFlow(0f)
    val recording = MutableStateFlow(false)
    val elapsedMs = MutableStateFlow(0L)

    private var record: AudioRecord? = null
    private var job: Job? = null
    private var pcm: File? = null
    private var target: File? = null
    private var voicedChunks = 0
    private var totalChunks = 0

    @Volatile private var stopping = false

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope, out: File): Boolean {
        if (recording.value) return true
        if (!hasPermission()) return false

        val sampleRate = 16_000
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return false

        val recorder = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 2,
            )
        }.getOrNull() ?: return false
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return false
        }

        // 录音前抢焦点并切到蓝牙麦克风（如果有）
        AudioFocusHelper.acquire(context, forRecording = true)
        AudioFocusHelper.routeToBluetooth(context)

        val pcmFile = File(context.cacheDir, "rec_${System.currentTimeMillis()}.pcm")
        record = recorder
        pcm = pcmFile
        target = out
        voicedChunks = 0
        totalChunks = 0
        amplitudes.value = emptyList()
        level.value = 0f
        elapsedMs.value = 0L
        stopping = false
        recording.value = true

        job = scope.launch(Dispatchers.IO) {
            val buffer = ShortArray(1024)
            val stream = runCatching { pcmFile.outputStream() }.getOrNull()
            val startedAt = System.currentTimeMillis()
            try {
                recorder.startRecording()
                while (isActive && !stopping) {
                    val read = runCatching { recorder.read(buffer, 0, buffer.size) }.getOrDefault(0)
                    if (read <= 0) {
                        kotlinx.coroutines.delay(10)
                        continue
                    }
                    var sum = 0.0
                    for (i in 0 until read) {
                        val v = buffer[i] / 32768f
                        sum += (v * v).toDouble()
                    }
                    val rms = sqrt(sum / read).toFloat().coerceIn(0f, 1f)
                    val amp = (rms * 3.4f).coerceIn(0.05f, 1f)
                    level.value = amp
                    amplitudes.value = (amplitudes.value + amp).takeLast(48)
                    totalChunks++
                    if (rms > 0.02f) voicedChunks++

                    val bytes = ByteArray(read * 2)
                    for (i in 0 until read) {
                        val s = buffer[i].toInt()
                        bytes[i * 2] = (s and 0xFF).toByte()
                        bytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                    }
                    stream?.write(bytes)
                    elapsedMs.value = System.currentTimeMillis() - startedAt
                }
            } catch (t: Throwable) {
                // 录音中断（被其他应用抢占 / 权限被撤销）时静默收尾，界面由 stop() 复位
            } finally {
                runCatching { stream?.close() }
                recording.value = false
            }
        }
        return true
    }

    /** 停止并封成 WAV；返回录音时长（毫秒）。 */
    suspend fun stop(): Long = withContext(Dispatchers.IO) {
        // 先让采集停止，等录音循环把最后一块写完，再封 WAV（否则尾巴必丢）
        stopping = true
        runCatching { record?.stop() }
        kotlinx.coroutines.withTimeoutOrNull(1500L) { job?.join() }
        job = null
        recording.value = false
        runCatching { record?.release() }
        record = null
        AudioFocusHelper.restoreRoute(context)
        AudioFocusHelper.abandon(context)

        val pcmFile = pcm
        val out = target
        pcm = null
        target = null

        if (pcmFile == null || out == null || !pcmFile.exists()) return@withContext 0L
        val dataBytes = pcmFile.length()
        // 16kHz/16bit/单声道：每毫秒 32 字节
        val duration = dataBytes * 1000L / (16_000L * 2L)
        runCatching {
            RandomAccessFile(out, "rw").use { raf ->
                raf.setLength(0)
                raf.write(wavHeader(dataBytes.toInt()))
                raf.write(pcmFile.readBytes())
            }
        }
        pcmFile.delete()

        // 时长照实返回；是否「几乎全是静音」交给上层提示，不再伪装成录音失败
        if (dataBytes < 3L * 16000L * 2L / 10L) 0L else duration
    }

    fun cancel() {
        stopping = true
        job?.cancel()
        job = null
        recording.value = false
        runCatching { record?.stop() }
        runCatching { record?.release() }
        record = null
        AudioFocusHelper.restoreRoute(context)
        AudioFocusHelper.abandon(context)
        pcm?.delete()
        pcm = null
    }

    private fun wavHeader(dataLength: Int): ByteArray {
        val sampleRate = 16_000
        val channels = 1
        val bits = 16
        val byteRate = sampleRate * channels * bits / 8
        val header = ByteArray(44)
        fun putAscii(offset: Int, text: String) {
            text.forEachIndexed { i, c -> header[offset + i] = c.code.toByte() }
        }
        fun putInt(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
            header[offset + 2] = ((value shr 16) and 0xFF).toByte()
            header[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }
        fun putShort(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }
        putAscii(0, "RIFF")
        putInt(4, 36 + dataLength)
        putAscii(8, "WAVE")
        putAscii(12, "fmt ")
        putInt(16, 16)
        putShort(20, 1)
        putShort(22, channels)
        putInt(24, sampleRate)
        putInt(28, byteRate)
        putShort(32, channels * bits / 8)
        putShort(34, bits)
        putAscii(36, "data")
        putInt(40, dataLength)
        return header
    }
}
