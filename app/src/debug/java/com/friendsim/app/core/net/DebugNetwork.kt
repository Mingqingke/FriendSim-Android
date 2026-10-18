package com.friendsim.app.core.net

import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.friendsim.app.AppGraph
import android.content.Context
import android.content.Intent
import okhttp3.Interceptor

/**
 * 仅 debug：把每次 API 请求（URL / Body / 返回 JSON）送进 Chucker，
 * 通知栏可直接看到请求，确认真的打到了你写的端点。release 包中为空实现。
 */
fun debugInterceptor(): Interceptor? = runCatching<Interceptor?> {
    ChuckerInterceptor.Builder(AppGraph.app)
        .maxContentLength(250_000L)
        .build()
}.getOrNull()

/** 仅 debug：直接打开 Chucker 的请求列表。 */
fun openDebugConsole(context: Context): Boolean = runCatching {
    context.startActivity(
        Intent().apply {
            setClassName(context, "com.chuckerteam.chucker.internal.ui.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
    true
}.getOrDefault(false)
