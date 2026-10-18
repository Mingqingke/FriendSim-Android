package com.friendsim.app.core.data

import android.content.Context
import android.net.Uri
import com.friendsim.app.core.model.ChatMessage
import com.friendsim.app.core.model.Friend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * 档案与对话的本地存储（纯文件 + JSON，零数据库依赖）。
 * 照片 / 音频 / 对话历史全部留在应用私有目录，任何情况下都不上传。
 */
class FriendStore(private val context: Context) {

    private val root: File = File(context.filesDir, "friends")
    private val photoDir: File = File(root, "photos")
    private val voiceDir: File = File(root, "voice")
    private val chatDir: File = File(root, "chats")
    private val bgDir: File = File(root, "backgrounds")
    private val indexFile: File = File(root, "friends.json")

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    init {
        listOf(root, photoDir, voiceDir, chatDir, bgDir).forEach { it.mkdirs() }
        load()
    }

    private fun load() {
        _friends.value = runCatching {
            if (!indexFile.exists()) emptyList()
            else json.decodeFromString<List<Friend>>(indexFile.readText())
        }.getOrDefault(emptyList()).sortedByDescending { maxOf(it.lastActive, it.createdAt) }
    }

    private suspend fun persist() = withContext(Dispatchers.IO) {
        val text = json.encodeToString(_friends.value)
        val tmp = File(root, "friends.json.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(indexFile)) {
            indexFile.writeText(text)
            tmp.delete()
        }
    }

    fun find(id: String): Friend? = _friends.value.firstOrNull { it.id == id }

    suspend fun upsert(friend: Friend) {
        val next = _friends.value.toMutableList()
        val i = next.indexOfFirst { it.id == friend.id }
        if (i >= 0) next[i] = friend else next.add(friend)
        _friends.value = next.sortedByDescending { maxOf(it.lastActive, it.createdAt) }
        persist()
    }

    suspend fun delete(id: String) {
        _friends.value.firstOrNull { it.id == id }?.let { f ->
            f.photo?.let { File(photoDir, it).delete() }
            f.voice?.let { File(voiceDir, it).delete() }
        }
        File(chatDir, "$id.json").delete()
        _friends.value = _friends.value.filterNot { it.id == id }
        persist()
    }

    /** 级联清除：档案、照片、录音、聊天记录全部删除。 */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        runCatching { photoDir.deleteRecursively() }
        runCatching { voiceDir.deleteRecursively() }
        runCatching { chatDir.deleteRecursively() }
        listOf(photoDir, voiceDir, chatDir).forEach { it.mkdirs() }
        runCatching { indexFile.delete() }
        _friends.value = emptyList()
    }

    /** 好感度：每轮互动 +1，上限 100。 */
    suspend fun bumpAffinity(id: String, delta: Int = 1) {
        val f = find(id) ?: return
        f.affinity = (f.affinity + delta).coerceIn(0, 100)
        upsert(f)
    }

    suspend fun touch(id: String) {
        val f = find(id) ?: return
        f.lastActive = System.currentTimeMillis()
        upsert(f)
    }

    fun photoFile(friend: Friend): File? = photoFile(friend.photo)

    fun photoFile(name: String?): File? =
        name?.takeIf { it.isNotBlank() }?.let { File(photoDir, it) }?.takeIf { it.exists() }

    fun voiceFile(friend: Friend): File? = voiceFile(friend.voice)

    fun voiceFile(name: String?): File? =
        name?.takeIf { it.isNotBlank() }?.let { File(voiceDir, it) }?.takeIf { it.exists() }

    /** 聊天语音条与参考音频共用 voice 目录。 */
    fun chatVoiceFile(name: String?): File? = voiceFile(name)

    fun backgroundFile(name: String?): File? =
        name?.takeIf { it.isNotBlank() }?.let { File(bgDir, it) }?.takeIf { it.exists() }

    fun deleteBackground(name: String?) {
        name?.takeIf { it.isNotBlank() }?.let { File(bgDir, it).delete() }
    }

    /** 把外部 Uri（相册 / 文件选择器）复制进私有目录，返回文件名。 */
    suspend fun importFile(uri: Uri, kind: Kind): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = when (kind) {
                Kind.PHOTO -> photoDir
                Kind.AUDIO -> voiceDir
                Kind.BACKGROUND -> bgDir
            }
            val ext = context.contentResolver.getType(uri)?.substringAfter('/', "").let { mime ->
                when {
                    mime.isNullOrBlank() -> guessExt(uri)
                    mime.contains("jpeg") -> "jpg"
                    mime.contains("png") -> "png"
                    mime.contains("webp") -> "webp"
                    mime.contains("mpeg") && kind == Kind.AUDIO -> "mp3"
                    mime.contains("mp4") -> "m4a"
                    mime.contains("wav") -> "wav"
                    else -> mime
                }
            }
            val name = "${UUID.randomUUID()}.$ext"
            val target = File(dir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            if (target.length() <= 0L) {
                target.delete()
                null
            } else {
                name
            }
        }.getOrNull()
    }

    /** 把应用内已有的文件（例如刚录好的 WAV）收进档案目录。 */
    suspend fun adoptFile(file: File, kind: Kind): String? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        val dir = when (kind) {
            Kind.PHOTO -> photoDir
            Kind.AUDIO -> voiceDir
            Kind.BACKGROUND -> bgDir
        }
        val ext = file.extension.ifBlank { if (kind == Kind.AUDIO) "wav" else "jpg" }
        val name = "${UUID.randomUUID()}.$ext"
        runCatching { file.copyTo(File(dir, name), overwrite = true); name }.getOrNull()
    }

    private fun guessExt(uri: Uri): String {
        val p = uri.lastPathSegment.orEmpty()
        return p.substringAfterLast('.', "dat").take(5).ifBlank { "dat" }
    }

    enum class Kind { PHOTO, AUDIO, BACKGROUND }

    // —— 对话历史 ——

    private fun chatFile(id: String) = File(chatDir, "$id.json")

    /** 聊天记录整体加密落盘（AES-256/GCM，密钥在 Keystore）；老版本明文文件仍可读。 */
    fun messages(friendId: String): List<ChatMessage> = runCatching {
        val f = chatFile(friendId)
        if (!f.exists()) return emptyList()
        val raw = f.readBytes()
        val plain = SecretBox.decrypt(raw) ?: raw
        json.decodeFromString<List<ChatMessage>>(String(plain, Charsets.UTF_8))
    }.getOrDefault(emptyList())

    suspend fun append(friendId: String, message: ChatMessage) = withContext(Dispatchers.IO) {
        val next = messages(friendId).toMutableList().apply { add(message) }
        writeChat(friendId, next)
        touch(friendId)
    }

    suspend fun replaceAll(friendId: String, list: List<ChatMessage>) = withContext(Dispatchers.IO) {
        writeChat(friendId, list)
    }

    /** 回到某条消息之后重来：保留到该条（含），后面全部丢弃。 */
    suspend fun truncateAfter(friendId: String, messageId: String) = withContext(Dispatchers.IO) {
        val all = messages(friendId)
        val index = all.indexOfFirst { it.id == messageId }
        if (index >= 0) writeChat(friendId, all.take(index + 1))
    }

    suspend fun deleteMessage(friendId: String, messageId: String) = withContext(Dispatchers.IO) {
        val next = messages(friendId).filterNot { it.id == messageId }
        writeChat(friendId, next)
    }

    private fun writeChat(friendId: String, list: List<ChatMessage>) {
        val plain = json.encodeToString(list.takeLast(400)).toByteArray(Charsets.UTF_8)
        val sealed = SecretBox.encrypt(plain)
        val file = chatFile(friendId)
        if (sealed != null) file.writeBytes(sealed) else file.writeBytes(plain)
    }

    suspend fun clearChat(friendId: String) = withContext(Dispatchers.IO) {
        chatFile(friendId).delete()
        Unit
    }

    fun lastMessage(friendId: String): ChatMessage? = messages(friendId).lastOrNull()

    /** 列表预览用：跳过技术性 notice，只取真正说出口的话。 */
    fun lastVisibleMessage(friendId: String): ChatMessage? =
        messages(friendId).lastOrNull { it.role != "notice" }
}
