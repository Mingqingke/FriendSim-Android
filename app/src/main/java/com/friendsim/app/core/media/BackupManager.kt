package com.friendsim.app.core.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.friendsim.app.AppGraph
import com.friendsim.app.core.data.MemoryItem
import com.friendsim.app.core.model.ChatMessage
import com.friendsim.app.core.model.Friend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 一键备份 / 恢复：档案、照片、录音、记忆、聊天记录打成一个 zip。
 * 换手机时选这个文件即可恢复；照片与语音都在包里。
 */
object BackupManager {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun export(context: Context): String? = withContext(Dispatchers.IO) {
        runCatching {
            val store = AppGraph.friends
            val memory = AppGraph.memory
            val tmp = File(context.cacheDir, "friendsim-backup.zip")

            ZipOutputStream(tmp.outputStream().buffered()).use { zip ->
                val index = store.friends.value.map { friend ->
                    FriendBackup(
                        friend = friend,
                        messages = store.messages(friend.id),
                        memories = memory.list(friend.id),
                    )
                }
                zip.putNextEntry(ZipEntry("index.json"))
                zip.write(json.encodeToString(ListSerializer(FriendBackup.serializer()), index).toByteArray())
                zip.closeEntry()

                val root = File(context.filesDir, "friends")
                listOf("photos", "voice", "backgrounds", "memories").forEach { dirName ->
                    File(root, dirName).listFiles()?.forEach { file ->
                        zip.putNextEntry(ZipEntry("media/$dirName/${file.name}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }

            val name = "friendsim-backup-${System.currentTimeMillis()}.zip"
            val saved = saveToDownloads(context, name, tmp)
            tmp.delete()
            saved
        }.getOrNull()
    }

    private fun saveToDownloads(context: Context, name: String, file: File): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/FriendSim")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            uri.toString()
        } else {
            val dir = File(context.getExternalFilesDir(null), "FriendSim")
            dir.mkdirs()
            val target = File(dir, name)
            file.copyTo(target, overwrite = true)
            target.absolutePath
        }
    }

    /** 从系统文件选择器恢复备份，返回恢复的档案数量。 */
    suspend fun restore(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        runCatching {
            val root = File(context.filesDir, "friends")
            listOf("photos", "voice", "backgrounds", "memories", "chats").forEach { File(root, it).mkdirs() }

            var restored = 0
            var indexJson: String? = null
            val stream = context.contentResolver.openInputStream(uri) ?: return@withContext 0

            ZipInputStream(stream.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    when {
                        name == "index.json" -> indexJson = zip.readBytes().toString(Charsets.UTF_8)
                        name.startsWith("media/") -> {
                            val rel = name.removePrefix("media/")
                            if (!rel.contains("..")) {
                                val target = File(root, rel)
                                target.parentFile?.mkdirs()
                                target.outputStream().use { out -> zip.copyTo(out) }
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }

            val list = indexJson?.let {
                json.decodeFromString(ListSerializer(FriendBackup.serializer()), it)
            } ?: return@withContext 0

            list.forEach { item ->
                AppGraph.friends.upsert(item.friend)
                item.messages.forEach { m -> AppGraph.friends.append(item.friend.id, m) }
                item.memories.forEach { m -> AppGraph.memory.add(item.friend.id, m.text, m.kind) }
                restored++
            }
            restored
        }.getOrDefault(0)
    }
}

@kotlinx.serialization.Serializable
data class FriendBackup(
    val friend: Friend,
    val messages: List<ChatMessage> = emptyList(),
    val memories: List<MemoryItem> = emptyList(),
)
