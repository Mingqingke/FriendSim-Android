package com.friendsim.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import com.friendsim.app.AppGraph
import com.friendsim.app.core.data.AppearancePrefs
import com.friendsim.app.core.data.FriendStore
import com.friendsim.app.core.data.SettingsStore
import com.friendsim.app.ui.components.BubbleShape
import com.friendsim.app.ui.components.CardBody
import com.friendsim.app.ui.components.FsPage
import com.friendsim.app.ui.components.FsSpacing
import com.friendsim.app.ui.components.section
import com.friendsim.app.ui.motion.bouncyClickable
import com.friendsim.app.ui.theme.LocalIsDark
import com.friendsim.app.ui.theme.accentChoices
import com.friendsim.app.ui.theme.bubbleColorChoices
import com.friendsim.app.ui.theme.chatFontChoices
import com.friendsim.app.ui.theme.chatFontFamily
import com.friendsim.app.ui.theme.parseColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val themeLabels = listOf("跟随系统", "浅色", "深色")
private val themeValues = listOf(
    SettingsStore.THEME_SYSTEM,
    SettingsStore.THEME_LIGHT,
    SettingsStore.THEME_DARK,
)
private val radiusLabels = listOf("小 8", "中 14", "大 18")
private val radiusValues = listOf(8, 14, 18)
private val menuLabels = listOf("果冻弹出", "淡入淡出", "缩放弹出")
private val menuValues = listOf(
    AppearancePrefs.MENU_JELLY,
    AppearancePrefs.MENU_FADE,
    AppearancePrefs.MENU_SCALE,
)
private val bgLabels = listOf("默认", "纯色", "图片")
private val bgValues = listOf(
    AppearancePrefs.BG_NONE,
    AppearancePrefs.BG_COLOR,
    AppearancePrefs.BG_IMAGE,
)
private val iconLabels = listOf("默认", "计算器", "备忘录")
private val iconValues = listOf(
    com.friendsim.app.core.system.IconSwitcher.ICON_DEFAULT,
    com.friendsim.app.core.system.IconSwitcher.ICON_CALC,
    com.friendsim.app.core.system.IconSwitcher.ICON_NOTE,
)
private val solidColors = listOf(
    "#F2F2F7", "#FFFFFF", "#EFEFF4", "#E8F0FE", "#EAF7EE", "#FFF4E5", "#1C1C1E",
)

/** 二级页：外观与美化（全部即时生效）。 */
@Composable
fun AppearanceScreen(onBack: () -> Unit, notify: (String) -> Unit) {
    val settings = AppGraph.settings
    val scope = rememberCoroutineScope()
    val look by settings.appearance.collectAsState(initial = AppearancePrefs())
    val themeMode by settings.themeMode.collectAsState(initial = SettingsStore.THEME_SYSTEM)
    val dynamicColor by settings.dynamicColor.collectAsState(initial = true)
    val dark = LocalIsDark.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val name = AppGraph.friends.importFile(uri, FriendStore.Kind.BACKGROUND)
            if (name != null) {
                AppGraph.friends.deleteBackground(look.bgImage)
                settings.setChatBackground(name)
                settings.setBgMode(AppearancePrefs.BG_IMAGE)
                notify("背景已更换")
            } else {
                notify("图片读取失败")
            }
        }
    }

    FsPage(title = "外观与美化", onBack = onBack) {
        item(key = "preview") {
            BubblePreview(look = look, dark = dark)
        }

        section(title = "主题", key = "theme") {
            OverlayDropdownPreference(
                items = themeLabels,
                selectedIndex = themeValues.indexOf(themeMode).coerceAtLeast(0),
                title = "主题模式",
                summary = null,
                onSelectedIndexChange = { index ->
                    scope.launch {
                        settings.setThemeMode(themeValues.getOrNull(index) ?: SettingsStore.THEME_SYSTEM)
                    }
                },
            )
            HorizontalDivider()
            SwitchPreference(
                checked = dynamicColor,
                onCheckedChange = { scope.launch { settings.setDynamicColor(it) } },
                title = "动态取色",
                summary = null,
            )
            HorizontalDivider()
            CardBody {
                Text(text = "强调色", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                ColorChips(
                    pairs = accentChoices,
                    selected = look.accent,
                    onPick = { scope.launch { settings.setAccent(it) } },
                )
            }
        }

        section(title = "聊天气泡", key = "bubble") {
            SwitchPreference(
                checked = look.iMessage,
                onCheckedChange = { scope.launch { settings.setIMessage(it) } },
                title = "iMessage 气泡",
                summary = null,
            )
            HorizontalDivider()
            OverlayDropdownPreference(
                items = radiusLabels,
                selectedIndex = radiusValues.indexOf(look.radiusDp).coerceAtLeast(2),
                title = "圆角",
                summary = null,
                onSelectedIndexChange = { index ->
                    scope.launch { settings.setRadius(radiusValues.getOrNull(index) ?: 18) }
                },
            )
            HorizontalDivider()
            CardBody {
                Text(text = "我的气泡", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                ColorChips(
                    pairs = bubbleColorChoices,
                    selected = look.mineColor,
                    onPick = { scope.launch { settings.setMineColor(it) } },
                )
                Spacer(Modifier.height(16.dp))
                Text(text = "朋友气泡", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                ColorChips(
                    pairs = bubbleColorChoices,
                    selected = look.friendColor,
                    onPick = { scope.launch { settings.setFriendColor(it) } },
                )
            }
            HorizontalDivider()
            SliderRow(
                title = "字号 ${(look.fontScale * 100).toInt()}%",
                value = look.fontScale,
                range = 0.7f..2f,
                onChange = { scope.launch { settings.setFontScale(it) } },
            )
            HorizontalDivider()
            OverlayDropdownPreference(
                items = chatFontChoices.map { it.first },
                selectedIndex = chatFontChoices.indexOfFirst { it.second == look.font }.coerceAtLeast(0),
                title = "字体",
                summary = null,
                onSelectedIndexChange = { index ->
                    scope.launch {
                        settings.setFont(chatFontChoices.getOrNull(index)?.second ?: AppearancePrefs.FONT_SANS)
                    }
                },
            )
        }

        section(title = "聊天背景", key = "bg") {
            OverlayDropdownPreference(
                items = bgLabels,
                selectedIndex = bgValues.indexOf(look.bgMode).coerceAtLeast(0),
                title = "背景",
                summary = null,
                onSelectedIndexChange = { index ->
                    scope.launch {
                        settings.setBgMode(bgValues.getOrNull(index) ?: AppearancePrefs.BG_NONE)
                    }
                },
            )
            if (look.bgMode == AppearancePrefs.BG_COLOR) {
                HorizontalDivider()
                CardBody {
                    ColorChips(
                        pairs = solidColors.map { it to it },
                        selected = look.bgColor,
                        onPick = { scope.launch { settings.setBgColor(it) } },
                    )
                }
            }
            if (look.bgMode == AppearancePrefs.BG_IMAGE) {
                HorizontalDivider()
                ArrowPreference(
                    title = "选择图片",
                    summary = if (look.bgImage.isBlank()) "未选择" else "已选择",
                    onClick = { picker.launch(arrayOf("image/*")) },
                )
                HorizontalDivider()
                SliderRow(
                    title = "模糊 ${look.bgBlur}px",
                    value = look.bgBlur.toFloat(),
                    range = 0f..40f,
                    onChange = { scope.launch { settings.setBgBlur(it.toInt()) } },
                )
                HorizontalDivider()
                SliderRow(
                    title = "暗度 ${look.bgDim}%",
                    value = look.bgDim.toFloat(),
                    range = 0f..100f,
                    onChange = { scope.launch { settings.setBgDim(it.toInt()) } },
                )
            }
        }

        section(title = "动画与触感", key = "motion") {
            SwitchPreference(
                checked = look.haptics,
                onCheckedChange = { scope.launch { settings.setHaptics(it) } },
                title = "触感反馈",
                summary = null,
            )
            HorizontalDivider()
            SwitchPreference(
                checked = look.reduceMotion,
                onCheckedChange = { scope.launch { settings.setReduceMotion(it) } },
                title = "减少动画",
                summary = null,
            )
            HorizontalDivider()
            OverlayDropdownPreference(
                items = iconLabels,
                selectedIndex = iconValues.indexOf(look.appIcon).coerceAtLeast(0),
                title = "应用图标",
                summary = null,
                onSelectedIndexChange = { index ->
                    val value = iconValues.getOrNull(index) ?: iconValues.first()
                    com.friendsim.app.core.system.IconSwitcher.apply(AppGraph.app, value)
                    scope.launch { settings.setAppIcon(value) }
                    notify(if (value == iconValues.first()) "已恢复默认图标" else "图标已替换")
                },
            )
            HorizontalDivider()
            OverlayDropdownPreference(
                items = menuLabels,
                selectedIndex = menuValues.indexOf(look.menuStyle).coerceAtLeast(0),
                title = "长按菜单",
                summary = null,
                onSelectedIndexChange = { index ->
                    scope.launch {
                        settings.setMenuStyle(menuValues.getOrNull(index) ?: AppearancePrefs.MENU_JELLY)
                    }
                },
            )
        }
    }
}

@Composable
private fun ColorChips(
    pairs: List<Pair<String, String>>,
    selected: String,
    onPick: (String) -> Unit,
) {
    val ring = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        pairs.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { (label, value) ->
                    val color = when (value) {
                        "system" -> MiuixTheme.colorScheme.primary
                        else -> parseColor(value, MiuixTheme.colorScheme.primary)
                    }
                    val active = value == selected
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (value == "system") {
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF0A84FF), Color(0xFFBF5AF2),
                                                Color(0xFFFF375F), Color(0xFFFF9F0A),
                                            ),
                                        )
                                    } else {
                                        Brush.linearGradient(listOf(color, color))
                                    },
                                )
                                .border(
                                    width = if (active) 2.dp else 1.dp,
                                    color = if (active) MiuixTheme.colorScheme.onSurface else ring,
                                    shape = CircleShape,
                                )
                                .bouncyClickable { onPick(value) },
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (label.startsWith("#")) "" else label,
                            fontSize = 10.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    // 拖动时只更新本地状态（保证不卡），停手后 160ms 再落盘
    var local by remember(value) { mutableFloatStateOf(value.coerceIn(range.start, range.endInclusive)) }
    LaunchedEffect(local) {
        if (abs(local - value) > 0.001f) {
            delay(160)
            onChange(local)
        }
    }
    CardBody {
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Slider(
            value = local,
            onValueChange = { local = it.coerceIn(range.start, range.endInclusive) },
            modifier = Modifier.fillMaxWidth(),
            valueRange = range,
        )
    }
}

/** 预览：真实使用当前气泡形状与配色。 */
@Composable
private fun BubblePreview(look: AppearancePrefs, dark: Boolean) {
    val scheme = MiuixTheme.colorScheme
    val mineColor = parseColor(look.mineColor, Color(0xFF007AFF))
    val friendColor = parseColor(look.friendColor, if (dark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
    val font = chatFontFamily(look.font)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .background(scheme.onSurface.copy(alpha = 0.04f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.Start) {
            Box(
                modifier = Modifier
                    .clip(BubbleShape(mine = false, iMessage = look.iMessage, radius = look.radiusDp.dp, showTail = true))
                    .background(friendColor)
                    .padding(start = if (look.iMessage) 20.dp else 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Text(
                    text = "在呢，怎么啦？",
                    fontSize = (15 * look.fontScale).sp,
                    color = if (dark) Color.White else Color.Black,
                    fontFamily = font,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .clip(BubbleShape(mine = true, iMessage = look.iMessage, radius = look.radiusDp.dp, showTail = true))
                    .background(mineColor)
                    .padding(start = 14.dp, end = if (look.iMessage) 20.dp else 14.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Text(
                    text = "随便聊聊",
                    fontSize = (15 * look.fontScale).sp,
                    color = Color.White,
                    fontFamily = font,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "实时预览",
            fontSize = 11.sp,
            color = scheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(start = FsSpacing.PageTop),
        )
    }
}
