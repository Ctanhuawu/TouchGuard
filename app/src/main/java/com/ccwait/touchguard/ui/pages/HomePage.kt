package com.ccwait.touchguard.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.BuildConfig
import com.ccwait.touchguard.strategy.StrategyReadiness
import com.ccwait.touchguard.strategy.StrategyType
import com.ccwait.touchguard.strategy.TouchLockManager
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import androidx.compose.ui.res.stringResource
import com.ccwait.touchguard.R

import com.ccwait.touchguard.ui.AppThemeMode
import com.ccwait.touchguard.ui.LocalAppThemeMode
import com.ccwait.touchguard.ui.adaptive.AdaptiveCard
import com.ccwait.touchguard.ui.adaptive.AdaptiveScaffold
import com.ccwait.touchguard.ui.adaptive.AdaptiveSectionTitle
import com.ccwait.touchguard.ui.adaptive.AdaptiveTopAppBar
import androidx.compose.material3.MaterialTheme

import com.ccwait.touchguard.ui.util.BlurredBar
import com.ccwait.touchguard.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 页面 1: 主页 (HomePage)
 * 支持 Miuix ↔ Material 3 风格自适应
 */
@Composable
fun HomePage(
    bottomInnerPadding: Dp,
    onLockToggle: () -> Unit = {}
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(true)
    val blurActive = backdrop != null
    val isMiuix = LocalAppThemeMode.current == AppThemeMode.Miuix
    val barColor = if (blurActive) Color.Transparent else if (isMiuix) colorScheme.surface else MaterialTheme.colorScheme.surface

    AdaptiveScaffold(
        topBar = {
            if (isMiuix) {
                BlurredBar(backdrop) {
                    AdaptiveTopAppBar(
                        barColor = barColor,
                        title = "TouchGuard",
                        scrollBehavior = scrollBehavior
                    )
                }
            } else {
                AdaptiveTopAppBar(
                    barColor = barColor,
                    title = "TouchGuard"
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
                    // 1. 经典工作状态看板
                    SukiStatusCard(
                        isLocked = TouchLockManager.isTouchLocked,
                        onLockToggle = onLockToggle
                    )

                    // 2. 设备与硬件信息
                    AdaptiveSectionTitle(
                        text = stringResource(R.string.info_hardware_section),
                        insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    )
                    DeviceInfoCard()

                    // 3. 软件与驱动环境
                    AdaptiveSectionTitle(
                        text = stringResource(R.string.info_software_section),
                        insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    )
                    SoftwareInfoCard()

                    Spacer(modifier = Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}
}

/**
 * 对齐 SukiSU-Ultra HomeMiuix StatusCard
 */
@Composable
private fun SukiStatusCard(
    isLocked: Boolean,
    onLockToggle: () -> Unit = {}
) {
    val currentStrategy = TouchLockManager.currentStrategy
    val readiness = currentStrategy.readiness
    val isPermMissing = readiness == StrategyReadiness.PERMISSION_MISSING
    val isUnsupported = readiness == StrategyReadiness.UNSUPPORTED
    val isDark = AppPreferences.colorMode.isDark || (AppPreferences.colorMode.isSystem && isSystemInDarkTheme())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isMiuix = LocalAppThemeMode.current == AppThemeMode.Miuix

    // 核心卡片容器色：已工作(绿色激活) / 未工作(正常待命灰，或未就绪告警色)
    val cardColor = when {
        !isMiuix -> when {
            isLocked -> MaterialTheme.colorScheme.primaryContainer
            isPermMissing -> MaterialTheme.colorScheme.errorContainer
            isUnsupported -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }
        isLocked -> {
            if (isDynamicColor) colorScheme.secondaryContainer
            else if (isDark) Color(0xFF1A3825)
            else Color(0xFFDFFAE4)
        }
        isPermMissing -> {
            if (isDynamicColor) colorScheme.errorContainer.copy(alpha = 0.55f)
            else if (isDark) Color(0xFF3E2405)
            else Color(0xFFFFF3E0)
        }
        isUnsupported -> {
            if (isDark) Color(0xFF262626) else Color(0xFFECEFF1)
        }
        else -> { // READY 或 CHECKING
            if (isDark) Color(0xFF262626) else Color(0xFFF1F3F5)
        }
    }

    // 水印大图标与半透色调
    val watermarkIcon = when {
        isLocked -> Icons.Rounded.Lock
        isPermMissing -> Icons.Rounded.Shield
        isUnsupported -> Icons.Rounded.Info
        else -> Icons.Rounded.CheckCircleOutline
    }

    val watermarkTint = when {
        !isMiuix -> when {
            isLocked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            isPermMissing -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
            isUnsupported -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        }
        isLocked -> (if (isDynamicColor) colorScheme.primary else Color(0xFF36D167)).copy(alpha = 0.8f)
        isPermMissing -> Color(0xFFFB8C00).copy(alpha = 0.85f)
        isUnsupported -> Color(0xFF78909C).copy(alpha = 0.8f)
        else -> if (isDark) Color(0xFF555555).copy(alpha = 0.45f) else Color(0xFFB0BEC5).copy(alpha = 0.5f)
    }

    // 核心状态标题：已工作 对应 未工作
    val titleText = if (isLocked) stringResource(R.string.home_status_locked) else stringResource(R.string.home_status_standby)
    val titleColor = when {
        !isMiuix -> when {
            isLocked -> MaterialTheme.colorScheme.onPrimaryContainer
            isPermMissing -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onSurface
        }
        else -> colorScheme.onSurface
    }

    // 徽章文本与颜色
    val badgeText = currentStrategy.badgeText
    val badgeBgColor = when {
        !isMiuix -> when {
            isLocked -> MaterialTheme.colorScheme.primary
            isPermMissing -> MaterialTheme.colorScheme.error
            isUnsupported -> MaterialTheme.colorScheme.outline
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        }
        isLocked -> if (isDynamicColor) colorScheme.tertiaryContainer else if (isDark) Color(0xFF315D3E) else Color(0xFFB8E8C5)
        isPermMissing -> if (isDark) Color(0xFF5D4037) else Color(0xFFFFCC80)
        isUnsupported -> if (isDark) Color(0xFF424242) else Color(0xFFCFD8DC)
        else -> if (isDark) Color(0xFF3A3A3A) else Color(0xFFE0E0E0)
    }
    val badgeTextColor = when {
        !isMiuix -> when {
            isLocked -> MaterialTheme.colorScheme.onPrimary
            isPermMissing -> MaterialTheme.colorScheme.onError
            isUnsupported -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        isLocked -> if (isDynamicColor) colorScheme.onTertiaryContainer else if (isDark) Color(0xFFB8E8C5) else Color(0xFF164A29)
        isPermMissing -> if (isDark) Color(0xFFFFCC80) else Color(0xFFE65100)
        isUnsupported -> if (isDark) Color(0xFFEEEEEE) else Color(0xFF37474F)
        else -> if (isDark) Color(0xFFDDDDDD) else Color(0xFF424242)
    }

    // 副标题文本
    val subtitleText = if (isLocked) {
        "${stringResource(R.string.home_desc_locked)} (${stringResource(AppPreferences.unlockMechanism.shortNameRes)})"
    } else {
        currentStrategy.statusSummary
    }
    val subtitleTextColor = if (isMiuix) colorScheme.onSurfaceSecondary else MaterialTheme.colorScheme.onSurfaceVariant
    val bottomTagColor = if (isMiuix) colorScheme.onSurfaceSecondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    // 底部英文标语：WORKING 对应 NOT WORKING
    val bottomTag = when {
        isLocked -> "WORKING"
        isPermMissing -> "PERMISSION REQUIRED"
        isUnsupported -> "UNSUPPORTED"
        else -> "NOT WORKING"
    }

    val onCardAction: () -> Unit = {
        if (isLocked) {
            onLockToggle()
        } else {
            when (readiness) {
                StrategyReadiness.READY -> onLockToggle()
                StrategyReadiness.PERMISSION_MISSING -> {
                    coroutineScope.launch {
                        currentStrategy.requestPermission(context)
                    }
                }
                StrategyReadiness.UNSUPPORTED -> {
                    TouchLockManager.selectStrategy(context, StrategyType.ACCESSIBILITY_OVERLAY)
                    Toast.makeText(context, "已为你切换为免 Root 悬浮窗锁定方案", Toast.LENGTH_SHORT).show()
                }
                StrategyReadiness.CHECKING -> {
                    Toast.makeText(context, "正在检测运行环境，请稍候...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val cardContent = @Composable {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            // 右下角 110dp 大号半透水印图标 (SukiSU 标志性布局)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(24.dp, 28.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    modifier = Modifier.size(110.dp),
                    imageVector = watermarkIcon,
                    tint = watermarkTint,
                    contentDescription = null
                )
            }

            // 左下角状态底标
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, bottom = 12.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = bottomTag,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = bottomTagColor
                )
            }

            // 顶部核心文字、状态徽章与快捷开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = titleText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = titleColor
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBgColor)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitleText,
                        fontSize = 14.sp,
                        color = subtitleTextColor
                    )
                }

                // 快捷开关
                if (isMiuix) {
                    top.yukonga.miuix.kmp.basic.Switch(
                        checked = isLocked,
                        onCheckedChange = { onCardAction() }
                    )
                } else {
                    androidx.compose.material3.Switch(
                        checked = isLocked,
                        onCheckedChange = { onCardAction() }
                    )
                }
            }
        }
    }

    if (isMiuix) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(color = cardColor),
            onClick = onCardAction,
            pressFeedbackType = PressFeedbackType.Tilt
        ) {
            cardContent()
        }
    } else {
        AdaptiveCard(
            modifier = Modifier.fillMaxWidth(),
            color = cardColor,
            onClick = onCardAction
        ) {
            cardContent()
        }
    }
}

/**
 * 设备与硬件信息卡
 */
@Composable
private fun DeviceInfoCard() {
    AdaptiveCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoText(
                icon = Icons.Rounded.Android,
                title = stringResource(R.string.info_android_version),
                content = "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
            )
            InfoText(
                icon = Icons.Rounded.Smartphone,
                title = stringResource(R.string.info_device_model),
                content = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (${android.os.Build.DEVICE})",
                bottomPadding = 0.dp
            )
        }
    }
}

/**
 * 软件与系统环境卡
 */
@Composable
private fun SoftwareInfoCard() {
    AdaptiveCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoText(
                icon = Icons.Rounded.Tag,
                title = stringResource(R.string.info_app_version),
                content = "v${BuildConfig.VERSION_NAME} · 稳定版"
            )
            InfoText(
                icon = Icons.Rounded.Person,
                title = stringResource(R.string.settings_developer_title),
                content = "Ctanhuawu"
            )
            val isMiuix = LocalAppThemeMode.current == AppThemeMode.Miuix
            InfoText(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.info_design_style),
                content = stringResource(if (isMiuix) R.string.info_design_content else R.string.info_design_content_m3),
                bottomPadding = 0.dp
            )
        }
    }
}

/**
 * 1:1 对齐 SukiSU-Ultra HomeMiuix InfoText 布局规范 (支持 Material 3 自适应)
 */
@Composable
private fun InfoText(
    icon: ImageVector,
    title: String,
    content: String,
    bottomPadding: Dp = 20.dp
) {
    val isMiuix = LocalAppThemeMode.current == AppThemeMode.Miuix
    val textColor = if (isMiuix) colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
    val summaryColor = if (isMiuix) colorScheme.onSurfaceVariantSummary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier
                .padding(end = 14.dp)
                .size(24.dp),
            tint = textColor
        )
        Column {
            Text(
                text = title,
                fontSize = if (isMiuix) MiuixTheme.textStyles.headline1.fontSize else MaterialTheme.typography.bodyLarge.fontSize,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Text(
                text = content,
                fontSize = if (isMiuix) MiuixTheme.textStyles.body2.fontSize else MaterialTheme.typography.bodyMedium.fontSize,
                color = summaryColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
