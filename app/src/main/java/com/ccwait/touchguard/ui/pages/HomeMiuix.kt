package com.ccwait.touchguard.ui.pages

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.BuildConfig
import com.ccwait.touchguard.R
import com.ccwait.touchguard.strategy.StrategyReadiness
import com.ccwait.touchguard.strategy.StrategyType
import com.ccwait.touchguard.strategy.TouchLockManager
import com.ccwait.touchguard.ui.util.BlurredBar
import com.ccwait.touchguard.ui.util.rememberBlurBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 页面 1: 主页 Miuix 纯净实现 (HomeMiuix)
 * 1:1 对齐 SukiSU-Ultra HomeMiuix 风格规范，纯 Miuix 组件渲染，零 Material 依赖
 */
@Composable
fun HomeMiuix(
    bottomInnerPadding: Dp,
    onLockToggle: () -> Unit = {}
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(true)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = "TouchGuard",
                    scrollBehavior = scrollBehavior
                )
            }
        },
    ) { innerPadding ->
        val lazyModifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(horizontal = 12.dp)

        val boxModifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier

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
                        // 1. 经典工作状态看板 (SukiSU-Ultra 1:1 对齐)
                        MiuixStatusCard(
                            isLocked = TouchLockManager.isTouchLocked,
                            onLockToggle = onLockToggle
                        )

                        // 2. 设备与硬件信息
                        SmallTitle(
                            text = stringResource(R.string.info_hardware_section),
                            insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        )
                        MiuixDeviceInfoCard()

                        // 3. 软件与驱动环境
                        SmallTitle(
                            text = stringResource(R.string.info_software_section),
                            insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        )
                        MiuixSoftwareInfoCard()

                        Spacer(modifier = Modifier.height(bottomInnerPadding))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixStatusCard(
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

    val cardColor = when {
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
        else -> {
            if (isDark) colorScheme.primaryContainer.copy(alpha = 0.25f)
            else colorScheme.primaryContainer.copy(alpha = 0.40f)
        }
    }

    val watermarkIcon = when {
        isLocked -> Icons.Rounded.Lock
        isPermMissing -> Icons.Rounded.Block
        isUnsupported -> Icons.Rounded.Info
        else -> Icons.Rounded.CheckCircleOutline
    }

    val watermarkTint = when {
        isLocked -> (if (isDynamicColor) colorScheme.primary else Color(0xFF36D167)).copy(alpha = 0.8f)
        isPermMissing -> Color(0xFFFB8C00).copy(alpha = 0.85f)
        isUnsupported -> Color(0xFF78909C).copy(alpha = 0.8f)
        else -> colorScheme.primary.copy(alpha = if (isDark) 0.35f else 0.25f)
    }

    val titleText = if (isLocked) stringResource(R.string.home_status_locked) else stringResource(R.string.home_status_standby)
    val badgeText = currentStrategy.getBadgeText(context)

    val badgeBgColor = when {
        isLocked -> if (isDynamicColor) colorScheme.tertiaryContainer else if (isDark) Color(0xFF315D3E) else Color(0xFFB8E8C5)
        isPermMissing -> if (isDark) Color(0xFF5D4037) else Color(0xFFFFCC80)
        isUnsupported -> if (isDark) Color(0xFF424242) else Color(0xFFCFD8DC)
        else -> colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.18f)
    }
    val badgeTextColor = when {
        isLocked -> if (isDynamicColor) colorScheme.onTertiaryContainer else if (isDark) Color(0xFFB8E8C5) else Color(0xFF164A29)
        isPermMissing -> if (isDark) Color(0xFFFFCC80) else Color(0xFFE65100)
        isUnsupported -> if (isDark) Color(0xFFEEEEEE) else Color(0xFF37474F)
        else -> colorScheme.primary
    }

    val subtitleText = currentStrategy.getStatusSummary(context)

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
                    if (currentStrategy.type == StrategyType.SHIZUKU_PINNING) {
                        coroutineScope.launch {
                            currentStrategy.requestPermission(context)
                        }
                    } else {
                        TouchLockManager.selectStrategy(context, StrategyType.ACCESSIBILITY_OVERLAY)
                        Toast.makeText(context, context.getString(R.string.toast_switched_to_overlay), Toast.LENGTH_SHORT).show()
                    }
                }
                StrategyReadiness.CHECKING -> {
                    Toast.makeText(context, context.getString(R.string.toast_checking_environment), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = cardColor),
        onClick = onCardAction,
        pressFeedbackType = PressFeedbackType.Tilt
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            // 水印图标
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

            // 状态底标
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
                    color = if (isLocked || isPermMissing) colorScheme.onSurfaceSecondary else colorScheme.primary.copy(alpha = 0.85f)
                )
            }

            // 状态标题、徽章与开关
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
                            color = colorScheme.onSurface
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
                        color = colorScheme.onSurfaceSecondary
                    )
                }

                Switch(
                    checked = isLocked,
                    onCheckedChange = { onCardAction() }
                )
            }
        }
    }
}

@Composable
private fun MiuixDeviceInfoCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            MiuixInfoText(
                icon = Icons.Rounded.Android,
                title = stringResource(R.string.info_android_version),
                content = "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
            )
            MiuixInfoText(
                icon = Icons.Rounded.Smartphone,
                title = stringResource(R.string.info_device_model),
                content = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (${android.os.Build.DEVICE})",
                bottomPadding = 0.dp
            )
        }
    }
}

@Composable
private fun MiuixSoftwareInfoCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            MiuixInfoText(
                icon = Icons.Rounded.Tag,
                title = stringResource(R.string.info_app_version),
                content = stringResource(R.string.status_version_beta, BuildConfig.VERSION_NAME)
            )
            MiuixInfoText(
                icon = Icons.Rounded.Person,
                title = stringResource(R.string.settings_developer_title),
                content = "Ctanhuawu",
                bottomPadding = 0.dp
            )
        }
    }
}

@Composable
private fun MiuixInfoText(
    icon: ImageVector,
    title: String,
    content: String,
    bottomPadding: Dp = 20.dp
) {
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
            tint = colorScheme.onSurface
        )
        Column {
            Text(
                text = title,
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface
            )
            Text(
                text = content,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
