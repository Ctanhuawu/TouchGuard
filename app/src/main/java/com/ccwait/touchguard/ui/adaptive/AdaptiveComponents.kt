package com.ccwait.touchguard.ui.adaptive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ccwait.touchguard.ui.AppThemeMode
import com.ccwait.touchguard.ui.LocalAppThemeMode
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 统一自适应 Scaffold，在 Miuix 与 Material 3 间无缝切换
 */
@Composable
fun AdaptiveScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    if (LocalAppThemeMode.current == AppThemeMode.Miuix) {
        top.yukonga.miuix.kmp.basic.Scaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            popupHost = {},
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
            content = content
        )
    } else {
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content
        )
    }
}

/**
 * 统一自适应 TopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    scrollBehavior: ScrollBehavior? = null,
    barColor: Color = Color.Unspecified,
    actions: @Composable RowScope.() -> Unit = {}
) {
    if (LocalAppThemeMode.current == AppThemeMode.Miuix) {
        top.yukonga.miuix.kmp.basic.TopAppBar(
            modifier = modifier,
            color = if (barColor != Color.Unspecified) barColor else MiuixTheme.colorScheme.surface,
            title = title,
            scrollBehavior = scrollBehavior,
            actions = actions
        )
    } else {
        TopAppBar(
            modifier = modifier,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (barColor != Color.Unspecified) barColor else MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            actions = actions
        )
    }
}

/**
 * 统一自适应 Card 容器 (Miuix 连续曲率圆角 ↔ Material 3 ElevatedCard)
 */
@Composable
fun AdaptiveCard(
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = PaddingValues(0.dp),
    color: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (LocalAppThemeMode.current == AppThemeMode.Miuix) {
        if (color != null) {
            val miuixColors = MiuixCardDefaults.defaultColors(color = color)
            if (onClick != null) {
                top.yukonga.miuix.kmp.basic.Card(
                    modifier = modifier,
                    insideMargin = insideMargin,
                    colors = miuixColors,
                    onClick = onClick,
                    pressFeedbackType = top.yukonga.miuix.kmp.utils.PressFeedbackType.Tilt,
                    content = content
                )
            } else {
                top.yukonga.miuix.kmp.basic.Card(
                    modifier = modifier,
                    insideMargin = insideMargin,
                    colors = miuixColors,
                    content = content
                )
            }
        } else {
            if (onClick != null) {
                top.yukonga.miuix.kmp.basic.Card(
                    modifier = modifier,
                    insideMargin = insideMargin,
                    onClick = onClick,
                    pressFeedbackType = top.yukonga.miuix.kmp.utils.PressFeedbackType.Tilt,
                    content = content
                )
            } else {
                top.yukonga.miuix.kmp.basic.Card(
                    modifier = modifier,
                    insideMargin = insideMargin,
                    content = content
                )
            }
        }
    } else {
        val containerColor = color ?: MaterialTheme.colorScheme.surfaceContainerLow
        val cardColors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        if (onClick != null) {
            ElevatedCard(
                onClick = onClick,
                modifier = modifier,
                shape = RoundedCornerShape(16.dp),
                colors = cardColors,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(insideMargin),
                    content = content
                )
            }
        } else {
            ElevatedCard(
                modifier = modifier,
                shape = RoundedCornerShape(16.dp),
                colors = cardColors,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(insideMargin),
                    content = content
                )
            }
        }
    }
}

/**
 * 统一自适应分组标题 (SmallTitle ↔ Material 3 TitleSmall)
 */
@Composable
fun AdaptiveSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
) {
    if (LocalAppThemeMode.current == AppThemeMode.Miuix) {
        top.yukonga.miuix.kmp.basic.SmallTitle(
            text = text,
            modifier = modifier,
            insideMargin = insideMargin
        )
    } else {
        Text(
            text = text,
            modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * 统一自适应开关项 (SwitchPreference ↔ Material 3 列表项)
 */
@Composable
fun AdaptiveSwitchPreference(
    title: String,
    summary: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    startAction: @Composable (() -> Unit)? = null,
    showDivider: Boolean = false
) {
    if (LocalAppThemeMode.current == AppThemeMode.Miuix) {
        top.yukonga.miuix.kmp.preference.SwitchPreference(
            modifier = modifier,
            title = title,
            summary = summary,
            checked = checked,
            onCheckedChange = onCheckedChange,
            startAction = startAction
        )
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCheckedChange(!checked) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (startAction != null) {
                    startAction()
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (summary.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    thumbContent = if (checked) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    } else null
                )
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = if (startAction != null) 44.dp else 16.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * 统一自适应下拉选项 (OverlayDropdownPreference ↔ Material 3 单选弹窗)
 */
@Composable
fun AdaptiveDropdownPreference(
    title: String,
    summary: String = "",
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    startAction: @Composable (() -> Unit)? = null,
    showDivider: Boolean = false
) {
    if (LocalAppThemeMode.current == AppThemeMode.Miuix) {
        top.yukonga.miuix.kmp.preference.OverlayDropdownPreference(
            modifier = modifier,
            title = title,
            summary = summary,
            items = items,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
            startAction = startAction
        )
    } else {
        var showDialog by remember { mutableStateOf(false) }

        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (startAction != null) {
                    startAction()
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (summary.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.UnfoldMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = if (startAction != null) 44.dp else 16.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        items.forEachIndexed { index, itemText ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectedIndexChange(index)
                                        showDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedIndex == index,
                                    onClick = {
                                        onSelectedIndexChange(index)
                                        showDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = itemText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selectedIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    }
}

/**
 * 统一自适应跳转项 (ArrowPreference ↔ Material 3 列表项)
 */
@Composable
fun AdaptiveArrowPreference(
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    startAction: @Composable (() -> Unit)? = null,
    showDivider: Boolean = false
) {
    if (LocalAppThemeMode.current == AppThemeMode.Miuix) {
        top.yukonga.miuix.kmp.preference.ArrowPreference(
            modifier = modifier,
            title = title,
            summary = summary,
            onClick = onClick,
            startAction = startAction
        )
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (startAction != null) {
                    startAction()
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!summary.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = if (startAction != null) 44.dp else 16.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * 统一自适应按钮
 */
@Composable
fun AdaptiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    if (LocalAppThemeMode.current == AppThemeMode.Miuix) {
        top.yukonga.miuix.kmp.basic.Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            content = content
        )
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            content = content
        )
    }
}
