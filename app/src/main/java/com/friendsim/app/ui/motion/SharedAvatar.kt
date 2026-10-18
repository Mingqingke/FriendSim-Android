package com.friendsim.app.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * 共享元素：朋友照片 / 头像从列表连续过渡到聊天页头部。
 * 使用 Compose 官方 SharedTransitionLayout（1.12 仍需 OptIn），不引入第三方共享元素库。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun sharedAvatarModifier(
    shared: SharedTransitionScope?,
    visibility: AnimatedVisibilityScope?,
    friendId: String,
): Modifier {
    if (shared == null || visibility == null) return Modifier
    val state = shared.rememberSharedContentState(key = "fs-avatar-$friendId")
    return with(shared) {
        Modifier.sharedElement(
            sharedContentState = state,
            animatedVisibilityScope = visibility,
            boundsTransform = { _, _ -> MotionTokens.bounds() },
        )
    }
}
