package com.ccwait.touchguard.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccwait.touchguard.model.AppLogManager
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

import com.ccwait.touchguard.ui.util.BlurredBar
import com.ccwait.touchguard.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop

import androidx.compose.ui.res.stringResource
import com.ccwait.touchguard.R

import com.ccwait.touchguard.ui.AppThemeMode
import com.ccwait.touchguard.ui.LocalAppThemeMode
import com.ccwait.touchguard.ui.adaptive.AdaptiveButton
import com.ccwait.touchguard.ui.adaptive.AdaptiveCard
import com.ccwait.touchguard.ui.adaptive.AdaptiveScaffold
import com.ccwait.touchguard.ui.adaptive.AdaptiveSectionTitle
import com.ccwait.touchguard.ui.adaptive.AdaptiveTopAppBar
import androidx.compose.material3.MaterialTheme

/**
 * 页面 3: 日志 (LogsPage)
 * 1:1 对齐 SukiSU-Ultra 安全审计日志规范
 */
@Composable
fun LogsPage(bottomInnerPadding: Dp) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(true)
    val blurActive = backdrop != null
    val isMiuix = LocalAppThemeMode.current == AppThemeMode.Miuix
    val barColor = if (blurActive) Color.Transparent else if (isMiuix) MiuixTheme.colorScheme.surface else MaterialTheme.colorScheme.surface
    val primaryColor = if (isMiuix) MiuixTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    val onSurfaceColor = if (isMiuix) MiuixTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
    val summaryTextColor = if (isMiuix) MiuixTheme.colorScheme.onSurfaceVariantSummary else MaterialTheme.colorScheme.onSurfaceVariant

    AdaptiveScaffold(
        topBar = {
            if (isMiuix) {
                BlurredBar(backdrop) {
                    AdaptiveTopAppBar(
                        barColor = barColor,
                        title = stringResource(R.string.logs_title),
                        scrollBehavior = scrollBehavior
                    )
                }
            } else {
                AdaptiveTopAppBar(
                    barColor = barColor,
                    title = stringResource(R.string.logs_title)
                )
            }
        },
    ) { innerPadding ->
        val lazyModifier = Modifier
            .fillMaxSize()
            .then(if (isMiuix) Modifier.scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier)
            .padding(horizontal = 12.dp)

        val boxModifier = if (isMiuix && backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier

        Box(modifier = boxModifier) {
            LazyColumn(
                modifier = lazyModifier,
                contentPadding = innerPadding,
                overscrollEffect = null
            ) {
            item {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. 运行状态与统计卡片
                AdaptiveCard(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = primaryColor
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.logs_title),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = onSurfaceColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.logs_summary_format, AppLogManager.logs.size),
                                fontSize = 12.sp,
                                color = summaryTextColor
                            )
                        }
                        AdaptiveButton(
                            onClick = { AppLogManager.clear() }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.action_clear),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.action_clear), fontSize = 12.sp)
                        }
                    }
                }

                // 2. 日志条目列表
                AdaptiveSectionTitle(
                    text = stringResource(R.string.logs_stream_title),
                    insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                )
                AdaptiveCard(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (AppLogManager.logs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.logs_empty),
                                fontSize = 14.sp,
                                color = summaryTextColor
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppLogManager.logs.forEach { item ->
                                val badgeBg = when {
                                    item.isWarning -> Color(0xFFD32F2F).copy(alpha = 0.12f)
                                    item.isSuccess -> Color(0xFF2E7D32).copy(alpha = 0.12f)
                                    else -> primaryColor.copy(alpha = 0.10f)
                                }
                                val badgeText = when {
                                    item.isWarning -> Color(0xFFD32F2F)
                                    item.isSuccess -> Color(0xFF2E7D32)
                                    else -> primaryColor
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 时间戳
                                    Text(
                                        text = item.timestamp,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = summaryTextColor
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))

                                    // 标签 Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.tag,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = badgeText
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))

                                    // 内容文本
                                    Text(
                                        text = item.message,
                                        fontSize = 13.sp,
                                        color = onSurfaceColor,
                                        modifier = Modifier.weight(1f),
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(bottomInnerPadding))
            }
            }
        }
    }
}
}


