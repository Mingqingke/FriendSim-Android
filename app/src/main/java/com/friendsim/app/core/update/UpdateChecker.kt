package com.friendsim.app.core.update

import android.content.Context
import com.friendsim.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.intOrNull
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
        val apkUrls: List<String>,
        val pageUrl: String,
    )

    /** 国内可达的下载镜像，按顺序尝试。 */
    private fun mirrors(url: String): List<String> = listOf(
        url,
        "https://ghfast.top/$url",
        "https://ghproxy.net/$url",
        "https://gh.llkk.cc/$url",
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun currentVersion(): String = BuildConfig.VERSION_NAME

    suspend fun check(): Release? = withContext(Dispatchers.IO) {
        // 源 1：GitHub API
        githubApi()?.let { return@withContext it }
        // 源 2/3：仓库里的 update.json（jsDelivr / raw）
        jsonMirror()?.let { return@withContext it }
        null
    }

    private fun githubApi(): Release? = runCatching {
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
                // 显示版本号已经改基线（2.0.0），不能再拿 versionName 比大小，
                // 一律用 versionCode 判断是否更新。
                val remoteCode = codeFromNotes(notes)
                if (remoteCode <= BuildConfig.VERSION_CODE) return@runCatching null
                Release(displayFromNotes(notes) ?: tag, cleanNotes(notes), mirrors(apk), page)
            }
        }.getOrNull()

    /** 静态兜底：仓库根目录 update.json，走 jsDelivr 再走 raw。 */
    private fun jsonMirror(): Release? {
        val urls = listOf(
            "https://cdn.jsdelivr.net/gh/$REPO@main/update.json",
            "https://raw.githubusercontent.com/$REPO/main/update.json",
        )
        for (url in urls) {
            val result = runCatching {
                val request = Request.Builder().url(url).header("User-Agent", "FriendSim").build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    val obj = json.parseToJsonElement(response.body.string()).jsonObject
                    val tag = obj["version"]?.jsonPrimitive?.contentOrNull.orEmpty().removePrefix("v")
                    val code = obj["code"]?.jsonPrimitive?.intOrNull ?: 0
                    if (tag.isBlank() || code <= BuildConfig.VERSION_CODE) return@runCatching null
                    val notes = obj["notes"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val apks = obj["apk"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
                    if (apks.isEmpty()) return@runCatching null
                    Release(obj["display"]?.jsonPrimitive?.contentOrNull ?: tag, notes, apks, obj["page"]?.jsonPrimitive?.contentOrNull.orEmpty())
                }
            }.getOrNull()
            if (result != null) return result
        }
        return null
    }

    /** release body 里的 `code=22` 行，用来做更新判断；同时从正文中隐藏。 */
    private fun codeFromNotes(notes: String): Int =
        Regex("(?m)^\\s*code\\s*=\\s*(\\d+)").find(notes)?.groupValues?.get(1)?.toIntOrNull() ?: 0

    /** release body 里的 `display=2.0.1`：用于界面展示，和兼容标签解耦。 */
    private fun displayFromNotes(notes: String): String? =
        Regex("(?m)^\\s*display\\s*=\\s*(\\S+)").find(notes)?.groupValues?.get(1)

    private fun cleanNotes(notes: String): String =
        notes.lineSequence()
            .filterNot { it.trim().matches(Regex("(?i)\\s*(code|display)\\s*=.*")) }
            .joinToString("\n")

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
            for (url in release.apkUrls) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FriendSim/${BuildConfig.VERSION_NAME}")
                .build()
            val result = runCatching {
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
            if (result != null) return@withContext result
            }
            null
        }.getOrNull()
    }
}
