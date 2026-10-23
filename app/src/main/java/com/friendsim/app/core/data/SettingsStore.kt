package com.friendsim.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.fsStore: DataStore<Preferences> by preferencesDataStore(name = "friendsim_settings")

/** 一次性快照，供对话引擎使用。 */
data class EngineConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com/v1",
    val model: String = "deepseek-chat",
    val offlineOnly: Boolean = false,
    val allowPublic: Boolean = true,
    val privacyAck: Boolean = false,
    val userName: String = "",
    val bridgeUrl: String = "http://127.0.0.1:9880",
    val ttsMode: String = "BRIDGE",
    val autoSpeak: Boolean = false,
    val themeMode: String = "SYSTEM",
    val dynamicColor: Boolean = false,
    val bubbleStyle: Boolean = true,
    val onboarded: Boolean = false,
    val chatBackground: String = "",
    val weatherOn: Boolean = true,
    val proactive: Boolean = true,
    val quietHours: Boolean = true,
    val keyValid: Boolean = true,
    val voiceReply: Boolean = false,
    val chatLocked: Boolean = false,
    val safetyStrikes: Int = 0,
) {
    val hasKey: Boolean get() = apiKey.isNotBlank()
}

/** 外观与美化（独立一门设置，改动即时生效）。 */
data class AppearancePrefs(
    val iMessage: Boolean = true,
    val radiusDp: Int = 18,
    val mineColor: String = "#007AFF",
    val friendColor: String = "#E5E5EA",
    val fontScale: Float = 1f,
    val font: String = FONT_SANS,
    val bgMode: String = BG_NONE,
    val bgColor: String = "#F2F2F7",
    val bgImage: String = "",
    val bgBlur: Int = 0,
    val bgDim: Int = 20,
    val accent: String = ACCENT_SYSTEM,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val menuStyle: String = MENU_JELLY,
    val appIcon: String = "DEFAULT",
    /** 高级材质：半透明/模糊层 */
    val advancedMaterial: Boolean = false,
    /** 高级动画：弹性与共享元素过渡 */
    val advancedMotion: Boolean = true,
) {
    companion object {
        const val FONT_SANS = "SANS"
        const val FONT_ROUND = "ROUND"
        const val FONT_SERIF = "SERIF"

        const val BG_NONE = "NONE"
        const val BG_COLOR = "COLOR"
        const val BG_IMAGE = "IMAGE"

        const val MENU_JELLY = "JELLY"
        const val MENU_FADE = "FADE"
        const val MENU_SCALE = "SCALE"

        const val ACCENT_SYSTEM = "system"
    }
}

/**
 * 设置存储（DataStore）。Key 只在 L1 使用且只存本机；
 * 外观偏好集中在这里，UI 通过 [appearance] 订阅。
 */
class SettingsStore(private val context: Context) {

    private object Key {
        val apiKey = stringPreferencesKey("api_key")
        val baseUrl = stringPreferencesKey("base_url")
        val model = stringPreferencesKey("model")
        val offlineOnly = booleanPreferencesKey("offline_only")
        val allowPublic = booleanPreferencesKey("allow_public")
        val privacyAck = booleanPreferencesKey("privacy_ack")
        val userName = stringPreferencesKey("user_name")
        val bridgeUrl = stringPreferencesKey("bridge_url")
        val ttsMode = stringPreferencesKey("tts_mode")
        val autoSpeak = booleanPreferencesKey("auto_speak")
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val onboarded = booleanPreferencesKey("onboarded")

        // 外观
        val bubbleStyle = booleanPreferencesKey("bubble_style")
        val radiusDp = intPreferencesKey("bubble_radius")
        val mineColor = stringPreferencesKey("bubble_mine")
        val friendColor = stringPreferencesKey("bubble_friend")
        val fontScale = floatPreferencesKey("bubble_font_scale")
        val fontFamily = stringPreferencesKey("bubble_font")
        val bgMode = stringPreferencesKey("chat_bg_mode")
        val bgColor = stringPreferencesKey("chat_bg_color")
        val bgImage = stringPreferencesKey("chat_background")
        val bgBlur = intPreferencesKey("chat_bg_blur")
        val bgDim = intPreferencesKey("chat_bg_dim")
        val accent = stringPreferencesKey("accent_color")
        val haptics = booleanPreferencesKey("haptics")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val advancedMaterial = booleanPreferencesKey("advanced_material")
        val advancedMotion = booleanPreferencesKey("advanced_motion")
        val chatLocked = booleanPreferencesKey("chat_locked")
        val safetyStrikes = intPreferencesKey("safety_strikes")
        val menuStyle = stringPreferencesKey("menu_style")
        val appIcon = stringPreferencesKey("app_icon")
        val weatherOn = booleanPreferencesKey("weather_on")
        val proactive = booleanPreferencesKey("proactive")
        val quietHours = booleanPreferencesKey("quiet_hours")
        val keyValid = booleanPreferencesKey("key_valid")
        val voiceReply = booleanPreferencesKey("voice_reply")
    }

    val apiKey: Flow<String> = context.fsStore.data.map { it[Key.apiKey] ?: "" }
    val baseUrl: Flow<String> = context.fsStore.data.map { it[Key.baseUrl] ?: "https://api.deepseek.com/v1" }
    val model: Flow<String> = context.fsStore.data.map { it[Key.model] ?: "deepseek-chat" }
    val offlineOnly: Flow<Boolean> = context.fsStore.data.map { it[Key.offlineOnly] ?: false }
    val allowPublic: Flow<Boolean> = context.fsStore.data.map { it[Key.allowPublic] ?: true }
    val privacyAck: Flow<Boolean> = context.fsStore.data.map { it[Key.privacyAck] ?: false }
    val userName: Flow<String> = context.fsStore.data.map { it[Key.userName] ?: "" }
    val bridgeUrl: Flow<String> = context.fsStore.data.map { it[Key.bridgeUrl] ?: "http://127.0.0.1:9880" }
    val ttsMode: Flow<String> = context.fsStore.data.map { it[Key.ttsMode] ?: "BRIDGE" }
    val autoSpeak: Flow<Boolean> = context.fsStore.data.map { it[Key.autoSpeak] ?: false }
    val voiceReply: Flow<Boolean> = context.fsStore.data.map { it[Key.voiceReply] ?: false }
    val themeMode: Flow<String> = context.fsStore.data.map { it[Key.themeMode] ?: "SYSTEM" }
    val dynamicColor: Flow<Boolean> = context.fsStore.data.map { it[Key.dynamicColor] ?: false }

    /** 外观偏好总入口。 */
    val appearance: Flow<AppearancePrefs> = context.fsStore.data.map { p ->
        AppearancePrefs(
            iMessage = p[Key.bubbleStyle] ?: true,
            radiusDp = p[Key.radiusDp] ?: 18,
            mineColor = p[Key.mineColor] ?: "#007AFF",
            friendColor = p[Key.friendColor] ?: "#E5E5EA",
            fontScale = p[Key.fontScale] ?: 1f,
            font = p[Key.fontFamily] ?: AppearancePrefs.FONT_SANS,
            bgMode = p[Key.bgMode] ?: AppearancePrefs.BG_NONE,
            bgColor = p[Key.bgColor] ?: "#F2F2F7",
            bgImage = p[Key.bgImage] ?: "",
            bgBlur = p[Key.bgBlur] ?: 0,
            bgDim = p[Key.bgDim] ?: 20,
            accent = p[Key.accent] ?: AppearancePrefs.ACCENT_SYSTEM,
            haptics = p[Key.haptics] ?: true,
            reduceMotion = p[Key.reduceMotion] ?: false,
            advancedMaterial = p[Key.advancedMaterial] ?: false,
            advancedMotion = p[Key.advancedMotion] ?: true,
            menuStyle = p[Key.menuStyle] ?: AppearancePrefs.MENU_JELLY,
            appIcon = p[Key.appIcon] ?: "DEFAULT",
        )
    }

    suspend fun setApiKey(v: String) = context.fsStore.edit { it[Key.apiKey] = v.trim() }
    suspend fun setBaseUrl(v: String) = context.fsStore.edit { it[Key.baseUrl] = v.trim() }
    suspend fun setModel(v: String) = context.fsStore.edit { it[Key.model] = v.trim() }
    suspend fun setOfflineOnly(v: Boolean) = context.fsStore.edit { it[Key.offlineOnly] = v }
    suspend fun setAllowPublic(v: Boolean) = context.fsStore.edit { it[Key.allowPublic] = v }
    suspend fun setPrivacyAck(v: Boolean) = context.fsStore.edit { it[Key.privacyAck] = v }
    suspend fun setUserName(v: String) = context.fsStore.edit { it[Key.userName] = v }
    suspend fun setBridgeUrl(v: String) = context.fsStore.edit { it[Key.bridgeUrl] = v.trim() }
    suspend fun setTtsMode(v: String) = context.fsStore.edit { it[Key.ttsMode] = v }
    suspend fun setAutoSpeak(v: Boolean) = context.fsStore.edit { it[Key.autoSpeak] = v }
    suspend fun setThemeMode(v: String) = context.fsStore.edit { it[Key.themeMode] = v }
    suspend fun setDynamicColor(v: Boolean) = context.fsStore.edit { it[Key.dynamicColor] = v }
    suspend fun setOnboarded(v: Boolean) = context.fsStore.edit { it[Key.onboarded] = v }
    suspend fun setChatBackground(v: String) = context.fsStore.edit { it[Key.bgImage] = v }

    // —— 外观 ——
    suspend fun setIMessage(v: Boolean) = context.fsStore.edit { it[Key.bubbleStyle] = v }
    suspend fun setRadius(v: Int) = context.fsStore.edit { it[Key.radiusDp] = v }
    suspend fun setMineColor(v: String) = context.fsStore.edit { it[Key.mineColor] = v }
    suspend fun setFriendColor(v: String) = context.fsStore.edit { it[Key.friendColor] = v }
    suspend fun setFontScale(v: Float) = context.fsStore.edit { it[Key.fontScale] = v }
    suspend fun setFont(v: String) = context.fsStore.edit { it[Key.fontFamily] = v }
    suspend fun setBgMode(v: String) = context.fsStore.edit { it[Key.bgMode] = v }
    suspend fun setBgColor(v: String) = context.fsStore.edit { it[Key.bgColor] = v }
    suspend fun setBgBlur(v: Int) = context.fsStore.edit { it[Key.bgBlur] = v }
    suspend fun setBgDim(v: Int) = context.fsStore.edit { it[Key.bgDim] = v }
    suspend fun setAccent(v: String) = context.fsStore.edit { it[Key.accent] = v }
    suspend fun setHaptics(v: Boolean) = context.fsStore.edit { it[Key.haptics] = v }
    suspend fun setReduceMotion(v: Boolean) = context.fsStore.edit { it[Key.reduceMotion] = v }
    suspend fun setAdvancedMaterial(v: Boolean) = context.fsStore.edit { it[Key.advancedMaterial] = v }
    suspend fun setAdvancedMotion(v: Boolean) = context.fsStore.edit { it[Key.advancedMotion] = v }
    val chatLocked: Flow<Boolean> = context.fsStore.data.map { it[Key.chatLocked] ?: false }
    val safetyStrikes: Flow<Int> = context.fsStore.data.map { it[Key.safetyStrikes] ?: 0 }
    suspend fun setChatLocked(v: Boolean) = context.fsStore.edit { it[Key.chatLocked] = v }
    suspend fun setSafetyStrikes(v: Int) = context.fsStore.edit { it[Key.safetyStrikes] = v }
    suspend fun setMenuStyle(v: String) = context.fsStore.edit { it[Key.menuStyle] = v }
    suspend fun setAppIcon(v: String) = context.fsStore.edit { it[Key.appIcon] = v }
    suspend fun setWeatherOn(v: Boolean) = context.fsStore.edit { it[Key.weatherOn] = v }
    suspend fun setProactive(v: Boolean) = context.fsStore.edit { it[Key.proactive] = v }
    suspend fun setQuietHours(v: Boolean) = context.fsStore.edit { it[Key.quietHours] = v }
    suspend fun setKeyValid(v: Boolean) = context.fsStore.edit { it[Key.keyValid] = v }
    suspend fun setVoiceReply(v: Boolean) = context.fsStore.edit { it[Key.voiceReply] = v }

    suspend fun snapshot(): EngineConfig {
        val p = context.fsStore.data.first()
        return EngineConfig(
            apiKey = p[Key.apiKey] ?: "",
            baseUrl = p[Key.baseUrl] ?: "https://api.deepseek.com/v1",
            model = p[Key.model] ?: "deepseek-chat",
            offlineOnly = p[Key.offlineOnly] ?: false,
            allowPublic = p[Key.allowPublic] ?: true,
            privacyAck = p[Key.privacyAck] ?: false,
            userName = p[Key.userName] ?: "",
            bridgeUrl = p[Key.bridgeUrl] ?: "http://127.0.0.1:9880",
            ttsMode = p[Key.ttsMode] ?: "BRIDGE",
            autoSpeak = p[Key.autoSpeak] ?: false,
            themeMode = p[Key.themeMode] ?: "SYSTEM",
            dynamicColor = p[Key.dynamicColor] ?: false,
            bubbleStyle = p[Key.bubbleStyle] ?: true,
            onboarded = p[Key.onboarded] ?: false,
            chatBackground = p[Key.bgImage] ?: "",
            weatherOn = p[Key.weatherOn] ?: true,
            proactive = p[Key.proactive] ?: true,
            quietHours = p[Key.quietHours] ?: true,
            keyValid = p[Key.keyValid] ?: true,
            voiceReply = p[Key.voiceReply] ?: false,
            chatLocked = p[Key.chatLocked] ?: false,
            safetyStrikes = p[Key.safetyStrikes] ?: 0,
        )
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.deepseek.com/v1"
        const val DEFAULT_MODEL = "deepseek-chat"
        const val DEFAULT_BRIDGE = "http://127.0.0.1:9880"

        const val TTS_BRIDGE = "BRIDGE"
        const val TTS_SYSTEM = "SYSTEM"
        const val TTS_OFF = "OFF"

        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
    }
}
