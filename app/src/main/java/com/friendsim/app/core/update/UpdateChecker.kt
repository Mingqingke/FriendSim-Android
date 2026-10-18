package com.friendsim.app.core.update

import android.content.Context
import com.friendsim.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 应用内更新：查 GitHub 最新 release，下载 APK 并拉起系统安装器。
 * 不依赖任何商店，纯 HTTP。
 */
object UpdateChecker {

    private const val REPO = "Mingqingke/FriendSim-Android"

    data class Release(
        val version: String,
        val notes: String,
        val apkUrl: String,
        val pageUrl: String,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun currentVersion(): String = BuildConfig.VERSION_NAME

    suspend fun check(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$REPO/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "FriendSim/${BuildConfig.VERSION_NAME}")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val body = response.body.string()
                val obj = json.parseToJsonElement(body).jsonObject
                val tag = obj["tag_name"]?.jsonPrimitive?.contentOrNull.orEmpty().removePrefix("v")
                val page = obj["html_url"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val notes = obj["body"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val apk = obj["assets"]?.jsonArray
                    ?.mapNotNull { it.jsonObject }
                    ?.firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk") == true }
                    ?.get("browser_download_url")?.jsonPrimitive?.contentOrNull
                    .orEmpty()
                if (tag.isBlank() || apk.isBlank()) return@runCatching null
                if (compareVersions(tag, BuildConfig.VERSION_NAME) <= 0) return@runCatching null
                Release(tag, notes, apk, page)
            }
        }.getOrNull()
    }

    /** 1.2.3 与 1.10.0 比较，返回正数表示 a 更新。 */
    fun compareVersions(a: String, b: String): Int {
        val pa = a.split('-', '+').first().split('.').mapNotNull { it.toIntOrNull() }
        val pb = b.split('-', '+').first().split('.').mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val va = pa.getOrElse(i) { 0 }
            val vb = pb.getOrElse(i) { 0 }
            if (va != vb) return va - vb
        }
        return 0
    }

    /** 下载到 cache，progress 回调 0..1。 */
    suspend fun download(
        context: Context,
        release: Release,
        onProgress: (Float) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(release.apkUrl)
                .header("User-Agent", "FriendSim/${BuildConfig.VERSION_NAME}")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val body = response.body
                val total = body.contentLength().coerceAtLeast(1L)
                val target = File(context.cacheDir, "friendsim-${release.version}.apk")
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        var written = 0L
                        while (input.read(buffer).also { read = it } > 0) {
                            output.write(buffer, 0, read)
                            written += read
                            runCatching { onProgress((written.toFloat() / total).coerceIn(0f, 1f)) }
                        }
                    }
                }
                if (target.length() < 1024 * 512) return@runCatching null
                target
            }
        }.getOrNull()
    }
}
