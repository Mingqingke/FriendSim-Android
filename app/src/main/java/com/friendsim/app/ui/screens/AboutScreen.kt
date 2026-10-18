package com.friendsim.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friendsim.app.BuildConfig
import com.friendsim.app.R
import com.friendsim.app.ui.components.CardBody
import com.friendsim.app.ui.components.FsPage
import com.friendsim.app.ui.components.section
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 二级页：关于（头像居中 · 开发者 Linux · 一句话说明 · 许可）。 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val scheme = MiuixTheme.colorScheme
    FsPage(title = "关于", onBack = onBack) {
        item(key = "hero") {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.defaultColors()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 26.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.developer_avatar),
                        contentDescription = "开发者头像",
                        modifier = Modifier.size(132.dp).clip(RoundedCornerShape(28.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "FriendSim",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "开发者: Linux",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = scheme.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = scheme.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "一款可以让你跟朋友对话的简易聊天工具",
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                        color = scheme.onSurface.copy(alpha = 0.8f),
                    )
                }
            }
        }

        section(title = "许可", key = "license") {
            CardBody {
                Text(
                    text = "GPL-3.0。源码与许可随应用分发，可自由使用与再发布。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = scheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}
