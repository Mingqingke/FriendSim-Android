package com.friendsim.app.core.llm

import com.friendsim.app.core.model.Friend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 情境感知：时间 / 星期 / 天气 / 纪念日，作为隐藏背景注入 System Prompt。
 * 天气走公开的 wttr.in（无需 Key、按 IP 定位），失败就静默省略，绝不阻塞对话。
 */
object ContextFacts {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(7, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var weatherCache: Pair<Long, String>? = null
    private const val WEATHER_TTL = 60 * 60 * 1000L

    suspend fun build(friend: Friend, weatherEnabled: Boolean): String = buildString {
        val now = Date()
        val cal = Calendar.getInstance()
        val week = SimpleDateFormat("EEEE", Locale.CHINA).format(now)
        val hm = SimpleDateFormat("HH:mm", Locale.CHINA).format(now)
        appendLine("现在：${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日 $week $hm。")

        if (weatherEnabled) {
            weather()?.let { appendLine("天气：$it。") }
        }

        friend.anniversary.takeIf { it.isNotBlank() }?.let { md ->
            val today = SimpleDateFormat("MM-dd", Locale.CHINA).format(now)
            if (today.replace('-', '.') == md.replace('-', '.')) {
                appendLine("今天是你们的纪念日（$md），可以主动提一句。")
            }
        }
        appendLine("深夜（23:00–06:00）时说话轻一点，别显得吵。")
    }

    private suspend fun weather(): String? = withContext(Dispatchers.IO) {
        weatherCache?.let { (at, text) ->
            if (System.currentTimeMillis() - at < WEATHER_TTL) return@withContext text
        }
        runCatching {
            val request = Request.Builder()
                .url("https://wttr.in/?format=%l%3A+%c+%t&lang=zh")
                .header("User-Agent", "curl/8")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val text = response.body.string().trim()
                if (text.isEmpty() || text.length > 80) null else text
            }
        }.getOrNull()?.also { weatherCache = System.currentTimeMillis() to it }
    }
}
