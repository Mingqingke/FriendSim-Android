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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

/**
 * 一键导出全部数据（JSON）：档案、灵魂档案、记忆、聊天记录。
 * 不含 API Key —— 备份文件不该带密钥。
 */
object DataExporter {

    suspend fun export(context: Context): String? = withContext(Dispatchers.IO) {
        runCatching {
            val store = AppGraph.friends
            val memory = AppGraph.memory
            val json = Json { prettyPrint = true; encodeDefaults = true }

            val payload = buildJsonObject {
                put("app", "FriendSim")
                put("exportedAt", System.currentTimeMillis())
                put(
                    "friends",
                    buildJsonArray {
                        store.friends.value.forEach { friend ->
                            add(
                                buildJsonObject {
                                    put("id", friend.id)
                                    put("name", friend.name)
                                    put("nickname", friend.nickname)
                                    put("persona", friend.persona)
                                    put("catchphrase", friend.catchphrase)
                                    put("background", friend.background)
                                    put("mood", friend.mood)
                                    put("affinity", friend.affinity)
                                    put("anniversary", friend.anniversary)
                                    put("photo", friend.photo ?: "")
                                    put("voice", friend.voice ?: "")
                                    put(
                                        "memories",
                                        json.encodeToJsonElement(
                                            ListSerializer(MemoryItem.serializer()),
                                            memory.list(friend.id),
                                        ),
                                    )
                                    put(
                                        "messages",
                                        json.encodeToJsonElement(
                                            ListSerializer(ChatMessage.serializer()),
                                            store.messages(friend.id),
                                        ),
                                    )
                                },
                            )
                        }
                    },
                )
            }.toString()

            val name = "friendsim-backup-${System.currentTimeMillis()}.json"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/FriendSim")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching null
                resolver.openOutputStream(uri)?.use { it.write(payload.toByteArray()) }
                uri.toString()
            } else {
                val dir = File(context.getExternalFilesDir(null), "FriendSim")
                dir.mkdirs()
                val file = File(dir, name)
                file.writeText(payload)
                file.absolutePath
            }
        }.getOrNull()
    }
}
