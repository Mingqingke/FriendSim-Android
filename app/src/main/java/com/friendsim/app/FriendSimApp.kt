package com.friendsim.app

import android.app.Application
import com.friendsim.app.core.data.FriendStore
import com.friendsim.app.core.data.MemoryStore
import com.friendsim.app.core.data.SettingsStore
import com.friendsim.app.core.llm.ChatEngine
import com.friendsim.app.core.voice.VoiceEngine

class FriendSimApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.attach(this)
        AppGraph.voice.prepareSystemTts()
        AppVisibility.attach(this)
        com.friendsim.app.core.notify.ChatNotifier.ensureChannel(this)
    }
}

/** 前后台判定：只有后台收到回复才发通知。 */
object AppVisibility {
    @Volatile
    var foreground: Boolean = false
        private set

    private var started = 0

    fun attach(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: android.app.Activity) {
                started++
                foreground = true
            }

            override fun onActivityStopped(activity: android.app.Activity) {
                started = (started - 1).coerceAtLeast(0)
                foreground = started > 0
            }

            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) = Unit
            override fun onActivityResumed(activity: android.app.Activity) = Unit
            override fun onActivityPaused(activity: android.app.Activity) = Unit
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) = Unit
            override fun onActivityDestroyed(activity: android.app.Activity) = Unit
        })
    }
}

/**
 * 极简依赖图（与 KernelSU manager 的 AppGraph 同思路，不引入 DI 框架）：
 * 每个单例都是 lazy，UI 与 core 分层清晰。
 */
object AppGraph {
    private lateinit var application: Application

    fun attach(app: Application) {
        application = app
    }

    val app: Application get() = application

    val settings: SettingsStore by lazy { SettingsStore(application) }
    val friends: FriendStore by lazy { FriendStore(application) }
    val engine: ChatEngine by lazy { ChatEngine() }
    val memory: MemoryStore by lazy { MemoryStore(application) }
    val voice: VoiceEngine by lazy { VoiceEngine(application) }
}
