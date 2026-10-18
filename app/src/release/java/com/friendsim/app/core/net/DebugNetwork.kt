package com.friendsim.app.core.net

import android.content.Context
import okhttp3.Interceptor

/** release：不挂任何调试拦截器。 */
fun debugInterceptor(): Interceptor? = null

/** release：无调试控制台。 */
fun openDebugConsole(context: Context): Boolean = false
