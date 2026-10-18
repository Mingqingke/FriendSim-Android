package com.friendsim.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** 自定义强调色（null = 跟随 MIUIx 主题）。 */
val LocalAccent = compositionLocalOf<Color?> { null }

/** 当前是否深色，供气泡默认配色使用。 */
val LocalIsDark = compositionLocalOf { false }

/** 触感总开关。 */
val LocalHapticsEnabled = compositionLocalOf { true }

/** 解析 #RRGGBB / system。 */
fun parseColor(value: String, fallback: Color): Color {
    val v = value.trim()
    if (!v.startsWith("#")) return fallback
    return runCatching { Color(android.graphics.Color.parseColor(v)) }.getOrDefault(fallback)
}

val accentChoices: List<Pair<String, String>> = listOf(
    "系统" to "system",
    "蓝" to "#0A84FF",
    "紫" to "#BF5AF2",
    "绿" to "#30D158",
    "橙" to "#FF9F0A",
    "粉" to "#FF375F",
    "青" to "#64D2FF",
)

val bubbleColorChoices: List<Pair<String, String>> = listOf(
    "蓝" to "#007AFF",
    "绿" to "#34C759",
    "灰" to "#E5E5EA",
    "紫" to "#AF52DE",
    "橙" to "#FF9500",
    "粉" to "#FF2D55",
    "靛" to "#5856D6",
)

val chatFontChoices: List<Pair<String, String>> = listOf(
    "系统" to com.friendsim.app.core.data.AppearancePrefs.FONT_SANS,
    "圆体" to com.friendsim.app.core.data.AppearancePrefs.FONT_ROUND,
    "衬线" to com.friendsim.app.core.data.AppearancePrefs.FONT_SERIF,
)

fun chatFontFamily(value: String): FontFamily = when (value) {
    com.friendsim.app.core.data.AppearancePrefs.FONT_ROUND -> FontFamily.Cursive
    com.friendsim.app.core.data.AppearancePrefs.FONT_SERIF -> FontFamily.Serif
    else -> FontFamily.Default
}
