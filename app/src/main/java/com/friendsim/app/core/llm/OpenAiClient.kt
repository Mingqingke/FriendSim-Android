package com.friendsim.app.core.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OpenAI 兼容 /chat/completions 客户端（SSE 流式）。
 * Level 1（用户自备 Key）与 Level 2（免 Key 公共端点）共用同一实现 —— 差别只在
 * 是否带 Authorization，以及 baseUrl 指向哪里。
 */
class OpenAiClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .callTimeout(80, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .apply { com.friendsim.app.core.net.debugInterceptor()?.let(::addInterceptor) }
        .build()

    /**
     * 逐段吐出回复文本。
     * @throws IOException 连接失败 / 非 2xx / 全程无内容
     */
    fun stream(
        baseUrl: String,
        apiKey: String?,
        model: String,
        systemPrompt: String,
        history: List<Pair<String, String>>,
        userText: String,
        maxTokens: Int = 300,
        imageDataUrl: String? = null,
    ): Flow<String> = flow {
        val payload = buildJsonObject {
            put("model", model.ifBlank { "deepseek-v4-flash-1017" })
            put("stream", true)
            put("temperature", 0.85)
            put("max_tokens", maxTokens)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                }
                history.takeLast(16).forEach { (role, text) ->
                    addJsonObject {
                        put("role", role)
                        put("content", text)
                    }
                }
                addJsonObject {
                    put("role", "user")
                    if (imageDataUrl == null) {
                        put("content", userText)
                    } else {
                        put("content", buildJsonArray {
                            addJsonObject {
                                put("type", "text")
                                put("text", userText)
                            }
                            addJsonObject {
                                put("type", "image_url")
                                put("image_url", buildJsonObject { put("url", imageDataUrl) })
                            }
                        })
                    }
                }
            }
        }.toString()

        val url = chatUrl(baseUrl)
        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody(JSON))
            .header("Accept", "text/event-stream")
            .apply { if (!apiKey.isNullOrBlank()) header("Authorization", "Bearer $apiKey") }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val detail = runCatching { response.body.string().take(240) }.getOrDefault("")
                throw IOException("HTTP ${response.code} $detail".trim())
            }
            val contentType = response.header("Content-Type").orEmpty()
            val body = response.body

            if (!contentType.contains("event-stream")) {
                // 少数端点忽略 stream 参数，直接返回整段 JSON
                val text = body.string()
                val content = extractContent(text)
                if (content.isBlank()) throw IOException("端点返回空内容")
                emit(content)
                return@use
            }

            val reader = body.byteStream().bufferedReader()
            var produced = false
            while (true) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data.isEmpty()) continue
                if (data == "[DONE]") break
                val delta = runCatching { parseDelta(data) }.getOrDefault("")
                if (delta.isNotEmpty()) {
                    produced = true
                    emit(delta)
                }
            }
            if (!produced) throw IOException("端点无有效内容")
        }
    }.flowOn(Dispatchers.IO)

    /** 连通性验证：max_tokens=1，10 秒超时，只返回一句人话。 */
    suspend fun ping(baseUrl: String, apiKey: String, model: String): Result<String> =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            runCatching {
                val payload = buildJsonObject {
                    put("model", model.ifBlank { "deepseek-v4-flash-1017" })
                    put("max_tokens", 1)
                    putJsonArray("messages") {
                        addJsonObject {
                            put("role", "user")
                            put("content", "hi")
                        }
                    }
                }.toString()
                val request = Request.Builder()
                    .url(chatUrl(baseUrl))
                    .post(payload.toRequestBody(JSON))
                    .apply { if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey") }
                    .build()
                val shortClient = client.newBuilder().callTimeout(10, TimeUnit.SECONDS).build()
                shortClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        "连接正常"
                    } else {
                        val detail = runCatching { response.body.string().take(120) }.getOrDefault("")
                        throw IOException("HTTP ${response.code} $detail".trim())
                    }
                }
            }
        }

    private fun extractContent(raw: String): String = runCatching {
        json.parseToJsonElement(raw).jsonObject["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull.orEmpty()
    }.getOrDefault("")

    private fun parseDelta(data: String): String {
        val obj = json.parseToJsonElement(data).jsonObject
        val choice = obj["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return ""
        val delta = choice["delta"]?.jsonObject
        val content = delta?.get("content")?.jsonPrimitive?.contentOrNull
        if (!content.isNullOrEmpty()) return content
        return choice["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        /**
         * 把用户填的 base_url 规整成 chat/completions 端点：
         * - 已含 /chat/completions → 原样
         * - 以 /openai 结尾（Pollinations 风格）→ 原样
         * - 其余（…/v1）→ 追加 /chat/completions
         */
        fun chatUrl(base: String): String {
            val b = base.trim().trimEnd('/')
            if (b.isEmpty()) return ""
            return when {
                b.contains("/chat/completions") -> b
                b.endsWith("/openai") -> b
                else -> "$b/chat/completions"
            }
        }
    }
}
