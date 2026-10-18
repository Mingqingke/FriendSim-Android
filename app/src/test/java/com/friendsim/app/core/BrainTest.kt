package com.friendsim.app.core

import com.friendsim.app.core.llm.OfflineBrain
import com.friendsim.app.core.llm.OpenAiClient
import com.friendsim.app.core.model.Friend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrainTest {

    @Test
    fun chatUrl_normalizes_v1_base() {
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            OpenAiClient.chatUrl("https://api.deepseek.com/v1"),
        )
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            OpenAiClient.chatUrl("https://api.deepseek.com/v1/"),
        )
        assertEquals(
            "https://text.pollinations.ai/openai",
            OpenAiClient.chatUrl("https://text.pollinations.ai/openai"),
        )
        assertEquals(
            "https://x.y/chat/completions",
            OpenAiClient.chatUrl("https://x.y/chat/completions"),
        )
    }

    @Test
    fun offline_personal_script_wins() {
        val friend = Friend(id = "1", name = "阿杰", scripts = "早|又起这么早啊\n累|那就先歇着")
        val reply = OfflineBrain.reply(friend, "早啊", "青")
        assertEquals("又起这么早啊", reply)
    }

    @Test
    fun offline_builtin_fallback_never_blank() {
        val friend = Friend(id = "2", name = "阿杰")
        repeat(30) {
            val reply = OfflineBrain.reply(friend, "今天天气还行吧", "青")
            assertTrue(reply.isNotBlank())
        }
    }

    @Test
    fun offline_placeholder_replaced() {
        val friend = Friend(id = "3", name = "阿杰")
        val reply = OfflineBrain.reply(friend, "我成功了", "青")
        assertTrue(reply.isNotBlank())
        assertTrue(!reply.contains("{name}"))
    }
}
