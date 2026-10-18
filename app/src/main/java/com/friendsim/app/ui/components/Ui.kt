package com.friendsim.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.friendsim.app.ui.motion.rememberIosFling
import com.friendsim.app.ui.motion.rubberBand
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.io.File

/** 统一间距令牌。 */
object FsSpacing {
    val PageHorizontal = 12.dp
    val PageTop = 8.dp
    val PageBottom = 24.dp
    val Item = 10.dp
    val CardPadding = 16.dp
}

/** 页面骨架：MIUIx Scaffold + TopAppBar（可返回）+ LazyColumn。 */
@Composable
fun FsPage(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    bottomPadding: Dp = 0.dp,
    onBack: (() -> Unit)? = null,
    toolbarActions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = title,
                subtitle = subtitle,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = "返回",
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
                actions = toolbarActions,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        LazyColumn(
            flingBehavior = rememberIosFling(),
            modifier = Modifier
                .fillMaxSize()
                .rubberBand()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = FsSpacing.PageHorizontal,
                end = FsSpacing.PageHorizontal,
                top = innerPadding.calculateTopPadding() + FsSpacing.PageTop,
                bottom = innerPadding.calculateBottomPadding() + bottomPadding + FsSpacing.PageBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(FsSpacing.Item),
            content = content,
        )
    }
}

/** 章节：小标题 + 一张卡承载整组内容。 */
fun LazyListScope.section(
    title: String,
    key: String = title,
    cardContent: @Composable ColumnScope.() -> Unit,
) {
    item(key = key) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SmallTitle(text = title)
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = CardDefaults.InsideMargin,
                colors = CardDefaults.defaultColors(),
                content = cardContent,
            )
        }
    }
}

/** 单张独立卡片。 */
fun LazyListScope.cardBlock(
    key: String,
    onClick: (() -> Unit)? = null,
    cardContent: @Composable ColumnScope.() -> Unit,
) {
    item(key = key) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            insideMargin = CardDefaults.InsideMargin,
            colors = CardDefaults.defaultColors(),
            pressFeedbackType = if (onClick != null) PressFeedbackType.Sink else PressFeedbackType.None,
            showIndication = false,
            onClick = onClick,
            content = cardContent,
        )
    }
}

/** 卡片内边距包装。 */
@Composable
fun CardBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(FsSpacing.CardPadding),
        content = content,
    )
}

/** 输入框（自绘外观 + BasicTextField，避免各页面样式分裂）。 */
@Composable
fun FsInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    singleLine: Boolean = true,
    minHeight: Dp = 44.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val scheme = MiuixTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(text = label, fontSize = 13.sp, color = scheme.onSurfaceVariantSummary)
            Spacer(Modifier.height(6.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.onSurface.copy(alpha = 0.06f))
                .heightIn(min = minHeight)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(text = placeholder, fontSize = 15.sp, color = scheme.onSurfaceVariantSummary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 15.sp, color = scheme.onSurface),
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                cursorBrush = SolidColor(scheme.primary),
            )
        }
    }
}

/** 只读行（标签 + 值）。 */
@Composable
fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = FsSpacing.CardPadding, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 13.sp)
        Spacer(Modifier.width(12.dp))
        Spacer(Modifier.weight(1f))
        Text(text = value, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** 小标签（引擎标记 / 状态）。 */
@Composable
fun Pill(text: String, color: Color = MiuixTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

/** 读一张本地图片（后台解码），供聊天背景等使用。 */
@Composable
fun rememberFileBitmap(file: File?): androidx.compose.ui.graphics.ImageBitmap? {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, file?.path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                file?.takeIf { it.exists() }?.let { f ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeFile(f.absolutePath, opts)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    return bitmap
}

/** 头像：有照片就显示照片，没有就显示名字首字。 */
@Composable
fun FriendAvatar(file: File?, name: String, size: Dp, modifier: Modifier = Modifier) {
    val scheme = MiuixTheme.colorScheme
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, file?.path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                file?.takeIf { it.exists() }?.let { f ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeFile(f.absolutePath, opts)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(scheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = name.take(1).ifBlank { "友" },
                fontSize = (size.value / 2.6f).sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 空状态卡片。 */
@Composable
fun EmptyCard(title: String, description: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.defaultColors()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
            )
            if (action != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAction, insideMargin = PaddingValues(horizontal = 22.dp, vertical = 10.dp)) {
                    Text(text = action)
                }
            }
        }
    }
}

/** 确认弹窗。 */
@Composable
fun ConfirmDialog(
    title: String,
    summary: String,
    confirmText: String = "确定",
    danger: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    OverlayDialog(show = true, title = title, summary = summary, onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(6.dp))
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = onConfirm, insideMargin = PaddingValues(horizontal = 18.dp, vertical = 10.dp)) {
                    Text(
                        text = confirmText,
                        color = if (danger) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}
