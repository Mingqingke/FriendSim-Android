package com.friendsim.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.friendsim.app.ui.components.FsPage
import com.friendsim.app.ui.components.section
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.preference.ArrowPreference

/** 一级页：设置（层级入口，不再堆设置项）。 */
@Composable
fun SettingsScreen(
    bottomPadding: Dp,
    onOpenAppearance: () -> Unit,
    onOpenChatSettings: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onOpenPrivacyData: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    FsPage(title = "设置", bottomPadding = bottomPadding) {
        section(title = "偏好", key = "prefs") {
            ArrowPreference(
                title = "外观与美化",
                summary = "气泡 · 背景 · 字体",
                onClick = onOpenAppearance,
            )
            HorizontalDivider()
            ArrowPreference(
                title = "对话",
                summary = "公共端点 · 仅离线 · 高级",
                onClick = onOpenChatSettings,
            )
            HorizontalDivider()
            ArrowPreference(
                title = "语音",
                summary = "朗读方式 · 克隆引擎",
                onClick = onOpenVoiceSettings,
            )
        }

        section(title = "其他", key = "other") {
            ArrowPreference(
                title = "隐私与数据",
                summary = null,
                onClick = onOpenPrivacyData,
            )
            HorizontalDivider()
            ArrowPreference(
                title = "关于 FriendSim",
                summary = null,
                onClick = onOpenAbout,
            )
        }
    }
}
