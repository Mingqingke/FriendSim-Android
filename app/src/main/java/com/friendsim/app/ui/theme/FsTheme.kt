package com.friendsim.app.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import com.friendsim.app.core.data.SettingsStore
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme as OfficialMiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 主题：直接使用 KernelSU 同款 MIUIx [ThemeController]，
 * 不在应用层自造第二套配色（跟随系统 / Monet 动态取色）。
 */
object FsTheme {

    @Composable
    operator fun invoke(
        themeMode: String,
        dynamicColor: Boolean,
        content: @Composable () -> Unit,
    ) {
        val dark = when (themeMode) {
            SettingsStore.THEME_LIGHT -> false
            SettingsStore.THEME_DARK -> true
            else -> isSystemInDarkTheme()
        }
        val mode = when {
            dynamicColor && dark -> ColorSchemeMode.MonetDark
            dynamicColor -> ColorSchemeMode.MonetLight
            dark -> ColorSchemeMode.Dark
            else -> ColorSchemeMode.Light
        }
        val controller = remember(mode, dark) { ThemeController(colorSchemeMode = mode, isDark = dark) }
        OfficialMiuixTheme(controller = controller, content = content)
    }
}

/** 动效规格：与 KernelSU 的 UiMotion 一致。 */
object FsMotion {
    val page: SpringSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 700f)
    val pageOffset: FiniteAnimationSpec<IntOffset> = spring(dampingRatio = 0.9f, stiffness = 700f)
    val soft: SpringSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 350f)
    val snappy: SpringSpec<Float> = spring(dampingRatio = 0.7f, stiffness = 900f)
}
