package com.ccwait.touchguard.ui.pages

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.ccwait.touchguard.ui.components.material.MaterialPreferenceCard
import com.ccwait.touchguard.ui.components.material.MaterialSectionTitle
import kotlinx.coroutines.launch

/**
 * 页面 1: 主页 Material 3 纯净实现 (HomeMaterial)
 * 1:1 对齐 Material You 规范，纯 M3 规范渲染，零 Miuix 依赖
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMaterial(
    bottomInnerPadding: Dp,
    onLockToggle: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TouchGuard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = innerPadding
        ) {
            item {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. 状态指示看板
                    MaterialStatusCard(
                        isLocked = TouchLockManager.isTouchLocked,
                        onLockToggle = onLockToggle
                    )

                    // 2. 设备与硬件信息
                    MaterialSectionTitle(text = stringResource(R.string.info_hardware_section))
                    MaterialDeviceInfoCard()

                    // 3. 软件与驱动环境
                    MaterialSectionTitle(text = stringResource(R.string.info_software_section))
                    MaterialSoftwareInfoCard()

                    Spacer(modifier = Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}

@Composable
private fun MaterialStatusCard(
    isLocked: Boolean,
    onLockToggle: () -> Unit = {}
) {
    val currentStrategy = TouchLockManager.currentStrategy
    val readiness = currentStrategy.readiness
    val isPermMissing = readiness == StrategyReadiness.PERMISSION_MISSING
    val isUnsupported = readiness == StrategyReadiness.UNSUPPORTED
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val cardColor = when {
        isLocked -> MaterialTheme.colorScheme.primaryContainer
        isPermMissing -> MaterialTheme.colorScheme.errorContainer
        isUnsupported -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    }

    val watermarkIcon = when {
        isLocked -> Icons.Rounded.Lock
        isPermMissing -> Icons.Rounded.Block
        isUnsupported -> Icons.Rounded.Info
        else -> Icons.Rounded.CheckCircleOutline
    }

    val watermarkTint = when {
        isLocked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        isPermMissing -> MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
        isUnsupported -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
    }

    val titleText = if (isLocked) stringResource(R.string.home_status_locked) else stringResource(R.string.home_status_standby)
    val titleColor = when {
        isLocked -> MaterialTheme.colorScheme.onPrimaryContainer
        isPermMissing -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val badgeBgColor = when {
        isLocked -> MaterialTheme.colorScheme.primary
        isPermMissing -> MaterialTheme.colorScheme.error
        isUnsupported -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    }

    val badgeTextColor = when {
        isLocked -> MaterialTheme.colorScheme.onPrimary
        isPermMissing -> MaterialTheme.colorScheme.onError
        isUnsupported -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.primary
    }

    val subtitleText = if (isLocked) {
        "${stringResource(R.string.home_desc_locked)} (${stringResource(AppPreferences.unlockMechanism.shortNameRes)})"
    } else {
        currentStrategy.statusSummary
    }

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onCardAction),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    .offset(20.dp, 24.dp),
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
                    .padding(start = 16.dp, bottom = 14.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = bottomTag,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isLocked || isPermMissing) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
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
                            color = titleColor
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBgColor)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = currentStrategy.badgeText,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun MaterialDeviceInfoCard() {
    MaterialPreferenceCard {
        Column(modifier = Modifier.padding(16.dp)) {
            MaterialInfoText(
                icon = Icons.Rounded.Android,
                title = stringResource(R.string.info_android_version),
                content = "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
            )
            MaterialInfoText(
                icon = Icons.Rounded.Smartphone,
                title = stringResource(R.string.info_device_model),
                content = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (${android.os.Build.DEVICE})",
                bottomPadding = 0.dp
            )
        }
    }
}

@Composable
private fun MaterialSoftwareInfoCard() {
    MaterialPreferenceCard {
        Column(modifier = Modifier.padding(16.dp)) {
            MaterialInfoText(
                icon = Icons.Rounded.Tag,
                title = stringResource(R.string.info_app_version),
                content = "v${BuildConfig.VERSION_NAME} · Beta版"
            )
            MaterialInfoText(
                icon = Icons.Rounded.Person,
                title = stringResource(R.string.settings_developer_title),
                content = "Ctanhuawu",
                bottomPadding = 0.dp
            )
        }
    }
}

@Composable
private fun MaterialInfoText(
    icon: ImageVector,
    title: String,
    content: String,
    bottomPadding: Dp = 18.dp
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
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
