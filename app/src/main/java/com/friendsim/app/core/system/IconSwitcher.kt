package com.friendsim.app.core.system

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * 桌面图标伪装：主入口与两个 ActivityAlias 之间切换，
 * 只改组件启用状态，不碰应用数据。
 */
object IconSwitcher {

    const val ICON_DEFAULT = "DEFAULT"
    const val ICON_CALC = "CALC"
    const val ICON_NOTE = "NOTE"

    fun apply(context: Context, mode: String) {
        val pkg = context.packageName
        fun set(cls: String, enabled: Boolean) {
            runCatching {
                context.packageManager.setComponentEnabledSetting(
                    ComponentName(pkg, cls),
                    if (enabled) {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    },
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
        val main = "$pkg.MainActivity"
        val calc = "$pkg.LauncherCalculator"
        val note = "$pkg.LauncherNotes"
        when (mode) {
            ICON_CALC -> {
                set(main, false)
                set(calc, true)
                set(note, false)
            }

            ICON_NOTE -> {
                set(main, false)
                set(calc, false)
                set(note, true)
            }

            else -> {
                set(main, true)
                set(calc, false)
                set(note, false)
            }
        }
    }
}
