package com.friendsim.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.friendsim.app.core.data.SettingsStore
import com.friendsim.app.core.model.Route
import com.friendsim.app.ui.motion.LocalMotionPrefs
import com.friendsim.app.ui.motion.MotionTokens
import com.friendsim.app.ui.motion.rememberMotionPrefs
import com.friendsim.app.ui.screens.AboutScreen
import com.friendsim.app.ui.screens.AdvancedScreen
import com.friendsim.app.ui.screens.AppearanceScreen
import com.friendsim.app.ui.screens.ChatSettingsScreen
import com.friendsim.app.ui.screens.PrivacyDataScreen
import com.friendsim.app.ui.screens.SoulScreen
import com.friendsim.app.ui.screens.VoiceSettingsScreen
import com.friendsim.app.ui.theme.LocalAccent
import com.friendsim.app.ui.theme.LocalHapticsEnabled
import com.friendsim.app.ui.theme.LocalIsDark
import com.friendsim.app.ui.theme.parseColor
import com.friendsim.app.ui.screens.ChatScreen
import com.friendsim.app.ui.screens.FriendEditScreen
import com.friendsim.app.ui.screens.FriendListScreen
import com.friendsim.app.ui.screens.OnboardingScreen
import com.friendsim.app.ui.screens.SettingsScreen
import com.friendsim.app.ui.screens.SplashScreen
import com.friendsim.app.ui.theme.FsTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Settings

/**
 * 应用外壳：Splash → Onboarding → 朋友列表 / 设置 →（二级）聊天 / 编辑 / 关于 →（三级）高级设置。
 * 一个 sealed [Route] 返回栈 + 共享元素（SharedTransitionLayout），页面之间只有 fade + 共享元素两种主角。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FsApp(initialChatId: String? = null) {
    val app = com.friendsim.app.AppGraph
    val themeMode by app.settings.themeMode.collectAsState(initial = SettingsStore.THEME_SYSTEM)
    val dynamicColor by app.settings.dynamicColor.collectAsState(initial = true)
    val look by app.settings.appearance.collectAsState(initial = com.friendsim.app.core.data.AppearancePrefs())
    val isDark = when (themeMode) {
        SettingsStore.THEME_LIGHT -> false
        SettingsStore.THEME_DARK -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    FsTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
        val systemMotion = rememberMotionPrefs()
        val motionPrefs = remember(systemMotion, look.reduceMotion) {
            systemMotion.copy(reduced = systemMotion.reduced || look.reduceMotion)
        }
        CompositionLocalProvider(
            LocalMotionPrefs provides motionPrefs,
            LocalHapticsEnabled provides look.haptics,
            LocalIsDark provides isDark,
            LocalAccent provides if (look.accent == com.friendsim.app.core.data.AppearancePrefs.ACCENT_SYSTEM) null else parseColor(look.accent, androidx.compose.ui.graphics.Color(0xFF0A84FF)),
        ) {
            SharedTransitionLayout {
                val scope = rememberCoroutineScope()
                var toast by remember { mutableStateOf<String?>(null) }
                var toastText by remember { mutableStateOf("") }
                var toastTick by remember { mutableIntStateOf(0) }
                val notify: (String) -> Unit = { message ->
                    toastText = message
                    toast = message
                    toastTick++
                }
                androidx.compose.runtime.LaunchedEffect(toastTick) {
                    if (toast != null) {
                        kotlinx.coroutines.delay(1600)
                        toast = null
                    }
                }

                // 启动时静默检查更新（GitHub release）
                var updateRelease by remember {
                    mutableStateOf<com.friendsim.app.core.update.UpdateChecker.Release?>(null)
                }
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    runCatching {
                        updateRelease = com.friendsim.app.core.update.UpdateChecker.check()
                    }
                }

                val stack = remember { mutableStateListOf<Route>(Route.Splash) }
                androidx.compose.runtime.LaunchedEffect(initialChatId) {
                    if (!initialChatId.isNullOrBlank()) {
                        // 从通知点进来：等启动页结束再跳
                        kotlinx.coroutines.delay(1600)
                        stack.clear()
                        stack.add(Route.Friends)
                        stack.add(Route.Chat(initialChatId))
                    }
                }
                val current = stack.last()
                val atRoot = current is Route.Friends || current is Route.Settings

                BackHandler(enabled = stack.size > 1 && current !is Route.Splash && current !is Route.Onboarding) {
                    stack.removeAt(stack.lastIndex)
                }

                val push: (Route) -> Unit = { route -> stack.add(route) }
                val pop: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
                val switchRoot: (Route) -> Unit = { route ->
                    stack.clear()
                    stack.add(route)
                }

                Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    bottomBar = {
                        if (atRoot) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = current is Route.Friends,
                                    onClick = { switchRoot(Route.Friends) },
                                    icon = MiuixIcons.Contacts,
                                    label = "朋友",
                                )
                                NavigationBarItem(
                                    selected = current is Route.Settings,
                                    onClick = { switchRoot(Route.Settings) },
                                    icon = MiuixIcons.Settings,
                                    label = "设置",
                                )
                            }
                        }
                    },
                    snackbarHost = {},
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = current,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            (
                                slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 3 } +
                                    fadeIn(tween(300))
                                ) togetherWith (
                                slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 6 } +
                                    fadeOut(tween(240))
                                )
                        },
                        label = "nav",
                    ) { route ->
                        val bottom = innerPadding.calculateBottomPadding()
                        val sharedScope: SharedTransitionScope? = this@SharedTransitionLayout
                        val visibility: AnimatedVisibilityScope? = this

                        when (route) {
                            is Route.Splash -> SplashScreen(
                                onDone = {
                                    scope.launch {
                                        val cfg = com.friendsim.app.AppGraph.settings.snapshot()
                                        val hasFriend = com.friendsim.app.AppGraph.friends.friends.value.isNotEmpty()
                                        stack.clear()
                                        stack.add(if (cfg.onboarded || hasFriend) Route.Friends else Route.Onboarding)
                                    }
                                },
                            )

                            is Route.Onboarding -> OnboardingScreen(
                                onFinish = {
                                    stack.clear()
                                    stack.add(Route.Friends)
                                },
                            )

                            is Route.Friends -> FriendListScreen(
                                bottomPadding = bottom,
                                shared = sharedScope,
                                visibility = visibility,
                                onOpenChat = { friend -> push(Route.Chat(friend.id)) },
                                onCreate = { push(Route.Edit(null)) },
                                onEdit = { friend -> push(Route.Edit(friend.id)) },
                            )

                            is Route.Settings -> SettingsScreen(
                                bottomPadding = bottom,
                                onOpenAppearance = { push(Route.Appearance) },
                                onOpenChatSettings = { push(Route.ChatSettings) },
                                onOpenVoiceSettings = { push(Route.VoiceSettings) },
                                onOpenPrivacyData = { push(Route.PrivacyData) },
                                onOpenAbout = { push(Route.About) },
                            )

                            is Route.Appearance -> AppearanceScreen(onBack = pop, notify = notify)

                            is Route.ChatSettings -> ChatSettingsScreen(
                                onBack = pop,
                                onOpenAdvanced = { push(Route.Advanced) },
                                notify = notify,
                            )

                            is Route.VoiceSettings -> VoiceSettingsScreen(onBack = pop, notify = notify)

                            is Route.PrivacyData -> PrivacyDataScreen(
                                onBack = pop,
                                onWiped = {
                                    stack.clear()
                                    stack.add(Route.Friends)
                                },
                                notify = notify,
                            )

                            is Route.Advanced -> AdvancedScreen(onBack = pop, notify = notify)

                            is Route.About -> AboutScreen(onBack = pop)

                            is Route.Edit -> FriendEditScreen(
                                friendId = route.friendId,
                                onBack = pop,
                                onSaved = { pop() },
                                notify = notify,
                            )

                            is Route.Chat -> ChatScreen(
                                friendId = route.friendId,
                                shared = sharedScope,
                                visibility = visibility,
                                onBack = pop,
                                onEdit = { id -> push(Route.Edit(id)) },
                                onOpenSoul = { id -> push(Route.Soul(id)) },
                                notify = notify,
                            )

                            is Route.Soul -> SoulScreen(
                                friendId = route.friendId,
                                onBack = pop,
                                notify = notify,
                            )
                        }
                    }
                }

                updateRelease?.let { release ->
                    com.friendsim.app.ui.components.UpdateDialog(
                        release = release,
                        onDismiss = { updateRelease = null },
                    )
                }

                // 提示：屏幕居中，不挡输入框
                AnimatedVisibility(
                    visible = toast != null,
                    modifier = Modifier.align(Alignment.Center),
                    enter = fadeIn(tween(180)) + scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.94f),
                    exit = fadeOut(tween(160)) + scaleOut(tween(180), targetScale = 0.96f),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xE6101014))
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = toastText,
                            color = Color.White,
                            fontSize = 14.sp,
                        )
                    }
                }
                }
            }
        }
    }
}
