package com.friendsim.app.core.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** 一条长期记忆（用户画像 / 情节）。 */
@Serializable
data class MemoryItem(
    val id: String,
    val text: String,
    val at: Long = System.currentTimeMillis(),
    val kind: String = KIND_PROFILE,
) {
    companion object {
        const val KIND_PROFILE = "profile"
        const val KIND_EPISODE = "episode"
    }
}

/**
 * 分层记忆：短期上下文在聊天历史里（最近 10 轮），
 * 长期部分落在这里 —— 用户画像（喜好/经历）+ 情节记忆（发生过的事）。
 * 全部加密保存，可逐条查看、编辑、删除。
 */
class MemoryStore(private val context: Context) {

    private val dir = File(context.filesDir, "friends/memories")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    init {
        dir.mkdirs()
    }

    private fun file(friendId: String) = File(dir, "$friendId.json")

    fun list(friendId: String): List<MemoryItem> = runCatching {
        val f = file(friendId)
        if (!f.exists()) return emptyList()
        val raw = f.readBytes()
        val plain = SecretBox.decrypt(raw) ?: raw
        json.decodeFromString<List<MemoryItem>>(String(plain, Charsets.UTF_8))
    }.getOrDefault(emptyList())

    private fun write(friendId: String, items: List<MemoryItem>) {
        val plain = json.encodeToString(items).toByteArray(Charsets.UTF_8)
        val sealed = SecretBox.encrypt(plain) ?: plain
        file(friendId).writeBytes(sealed)
    }

    suspend fun add(friendId: String, text: String, kind: String = MemoryItem.KIND_PROFILE) =
        withContext(Dispatchers.IO) {
            val clean = text.trim()
            if (clean.isEmpty()) return@withContext
            val items = list(friendId).toMutableList()
            if (items.any { it.text.equals(clean, true) }) return@withContext
            items += MemoryItem(UUID.randomUUID().toString(), clean, System.currentTimeMillis(), kind)
            write(friendId, items.takeLast(60))
        }

    suspend fun remove(friendId: String, memoryId: String) = withContext(Dispatchers.IO) {
        write(friendId, list(friendId).filterNot { it.id == memoryId })
    }

    suspend fun clear(friendId: String) = withContext(Dispatchers.IO) {
        file(friendId).delete()
        Unit
    }

    private fun metaFile() = File(context.filesDir, "friends/memories/_meta.json")

    fun compactedCount(friendId: String): Int {
        val meta = runCatching {
            com.friendsim.app.core.data.SecretBox.decrypt(metaFile().readBytes())
        }.getOrNull() ?: return 0
        return runCatching {
            json.parseToJsonElement(String(meta, Charsets.UTF_8))
                .let { it as kotlinx.serialization.json.JsonObject }
                .get(friendId)?.toString()?.trim('"')?.toIntOrNull()
        }.getOrNull() ?: 0
    }

    fun markCompacted(friendId: String, count: Int) {
        runCatching {
            val map = mutableMapOf<String, String>()
            val old = com.friendsim.app.core.data.SecretBox.decrypt(metaFile().readBytes())
            if (old != null) {
                (json.parseToJsonElement(String(old, Charsets.UTF_8)) as kotlinx.serialization.json.JsonObject)
                    .forEach { (k, v) -> map[k] = v.toString().trim('"') }
            }
            map[friendId] = count.toString()
            val text = kotlinx.serialization.json.JsonObject(
                map.mapValues { kotlinx.serialization.json.JsonPrimitive(it.value) },
            ).toString()
            val bytes = text.toByteArray(Charsets.UTF_8)
            metaFile().writeBytes(com.friendsim.app.core.data.SecretBox.encrypt(bytes) ?: bytes)
        }
    }

    /** 角色改名后，把记忆里的旧名字替换掉，避免精神分裂。 */
    suspend fun replaceName(friendId: String, oldName: String, newName: String) = withContext(Dispatchers.IO) {
        if (oldName.isBlank() || newName.isBlank() || oldName == newName) return@withContext
        val items = list(friendId)
        if (items.none { it.text.contains(oldName) }) return@withContext
        write(friendId, items.map { it.copy(text = it.text.replace(oldName, newName)) })
    }

    fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    /** 注入 System Prompt 的最近若干条。 */
    fun digest(friendId: String, limit: Int = 10): List<String> =
        list(friendId).takeLast(limit).map { it.text }

    // —— 自动提取（纯本地规则，不发请求、不额外花钱） ——

    private val patterns = listOf(
        Regex("我(?:叫|是)([^，。,.!?！？\\s]{1,12})"),
        Regex("我(?:喜欢|爱|爱吃|爱喝)([^。！？\\n]{1,20})"),
        Regex("我(?:不喜欢|讨厌|不吃|怕)([^。！？\\n]{1,20})"),
        Regex("我(?:住在|家在|在)([^。！？\\n]{2,12})(?:上班|工作|上学|住)?"),
        Regex("我(?:的)?生日是?([0-9一二三四五六七八九十]{1,6}月[0-9一二三四五六七八九十]{1,6}[日号]?)"),
        Regex("我(?:最近|今天|昨天)?(?:在|要)([^。！？\\n]{2,16})(?:忙|做|准备)"),
    )

    suspend fun autoExtract(friendId: String, text: String) {
        val hits = patterns.mapNotNull { pattern ->
            pattern.find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        }
        if (hits.isNotEmpty()) {
            val sentence = text.trim().take(24)
            hits.take(2).forEach { add(friendId, sentence) }
        }
    }
}
