package com.friendsim.app.core.voice

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import com.friendsim.app.core.data.EngineConfig
import com.friendsim.app.core.data.SettingsStore
import com.friendsim.app.core.model.Friend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 语音输出：优先走本地桥接的克隆引擎（GPT-SoVITS 等），失败自动退到系统 TTS。
 *
 * 桥接协议（见 assets/bridge/friendsim_bridge.py）：
 *   POST {bridge}/tts   multipart/form-data
 *     text        要朗读的文本
 *   prompt_text   参考音频对应的原文（可空）
 *   ref_audio     参考音频文件（用户导入的那段）
 *   ← 200 + audio/wav（或 mp3/m4a）字节流
 */
class VoiceEngine(private val context: Context) {

    enum class Mode { CLONE, SYSTEM, NONE }

    private val http = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(70, TimeUnit.SECONDS)
        .build()

    private val pendingSynth = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.CompletableDeferred<Boolean>>()

    private var systemTts: TextToSpeech? = null
    private var systemReady = false
    private val readyFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
    private var player: MediaPlayer? = null

    fun prepareSystemTts() {
        if (systemTts != null) return
        systemTts = TextToSpeech(context.applicationContext) { status ->
            systemReady = status == TextToSpeech.SUCCESS
            runCatching {
                systemTts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        utteranceId?.let { pendingSynth.remove(it)?.complete(true) }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        utteranceId?.let { pendingSynth.remove(it)?.complete(false) }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        utteranceId?.let { pendingSynth.remove(it)?.complete(false) }
                    }
                })
            }
            if (systemReady) {
                runCatching {
                    systemTts?.language = Locale.CHINA
                    systemTts?.setSpeechRate(1.0f)
                }
            }
            readyFlow.value = true
        }
    }

    /** 朗读一句话，返回实际使用的引擎。 */
    suspend fun speak(text: String, friend: Friend, voiceFile: File?, config: EngineConfig): Mode {
        if (text.isBlank()) return Mode.NONE
        if (config.ttsMode == SettingsStore.TTS_OFF) return Mode.NONE

        // 退到后台就不做克隆推理（省电、别在口袋里出声）
        val foreground = com.friendsim.app.AppVisibility.foreground
        if (foreground && config.ttsMode == SettingsStore.TTS_BRIDGE && voiceFile != null) {
            val audio = clone(text, friend, voiceFile, config.bridgeUrl)
            if (audio != null) {
                play(audio)
                return Mode.CLONE
            }
        }
        return speakWithSystem(text)
    }

    /** 调用克隆桥接；拿不到音频就返回 null（由调用方降级）。 */
    private suspend fun clone(text: String, friend: Friend, voiceFile: File, bridgeUrl: String): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                runCatching { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND) }
                val url = bridgeUrl.trim().trimEnd('/') + "/tts"
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("text", text)
                    .addFormDataPart("prompt_text", friend.catchphrase.ifBlank { "" })
                    .addFormDataPart("prompt_lang", "zh")
                    .addFormDataPart("text_lang", "zh")
                    .addFormDataPart(
                        "ref_audio",
                        voiceFile.name,
                        voiceFile.asRequestBody("audio/*".toMediaType()),
                    )
                    .build()
                val request = Request.Builder().url(url).post(body).build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    val bytes = response.body.bytes()
                    if (bytes.size < 512) return@runCatching null
                    val out = File(context.cacheDir, "tts_${System.currentTimeMillis()}.wav")
                    out.writeBytes(bytes)
                    out
                }
            }.getOrNull()
        }

    private suspend fun speakWithSystem(text: String): Mode {
        prepareSystemTts()
        // 首次调用时引擎可能还没初始化完，等一下再决定，避免"未配置朗读引擎"
        if (!readyFlow.value) {
            kotlinx.coroutines.withTimeoutOrNull(2500L) { readyFlow.first { it } }
        }
        val engine = systemTts ?: return Mode.NONE
        if (!systemReady) return Mode.NONE
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fs-${System.currentTimeMillis()}")
        return Mode.SYSTEM
    }

    /**
     * 把一句话合成为音频文件：优先克隆桥接，失败退系统 TTS 的 synthesizeToFile。
     * 用来把朋友的回复变成语音条。
     */
    suspend fun synthesizeToFile(
        text: String,
        friend: Friend,
        voiceFile: File?,
        config: EngineConfig,
    ): File? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext null

        if (config.ttsMode == SettingsStore.TTS_BRIDGE && voiceFile != null) {
            clone(text, friend, voiceFile, config.bridgeUrl)?.let { return@withContext it }
        }

        prepareSystemTts()
        if (!readyFlow.value) {
            kotlinx.coroutines.withTimeoutOrNull(2500L) { readyFlow.first { it } }
        }
        val engine = systemTts ?: return@withContext null
        if (!systemReady) return@withContext null

        val out = File(context.cacheDir, "tts_${System.currentTimeMillis()}.wav")
        val id = "synth-${System.currentTimeMillis()}"
        val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
        pendingSynth[id] = deferred
        val ok = engine.synthesizeToFile(text, android.os.Bundle(), out, id) == TextToSpeech.SUCCESS
        if (!ok) {
            pendingSynth.remove(id)
            return@withContext null
        }
        val done = kotlinx.coroutines.withTimeoutOrNull(10_000L) { deferred.await() } ?: false
        pendingSynth.remove(id)
        if (!done || !out.exists() || out.length() < 1024) {
            out.delete()
            null
        } else {
            out
        }
    }

    /** 试听用：直接系统语音，不走桥接（等初始化完成，最多 2.5 秒）。 */
    suspend fun speakSystem(text: String): Mode = speakWithSystem(text)

    /** 播放录音文件（引导页试听）。 */
    fun playFile(file: File) = play(file)

    private fun play(file: File) {
        withContextMain {
            runCatching {
                player?.release()
                AudioFocusHelper.acquire(context, forRecording = false)
                player = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener { mp ->
                        runCatching { mp.release() }
                        AudioFocusHelper.abandon(context)
                    }
                }
            }
        }
    }

    private fun withContextMain(block: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(block)
    }

    fun stop() {
        runCatching { player?.stop(); player?.release(); player = null }
        AudioFocusHelper.abandon(context)
        runCatching { systemTts?.stop() }
    }

    fun release() {
        stop()
        runCatching { systemTts?.shutdown() }
        systemTts = null
    }
}
