/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.key
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.BuildConfig
import io.github.vibhor1102.macrion.R
import io.github.vibhor1102.macrion.core.common.quality.ui.AccessibilityTroubleshootingDialog
import io.github.vibhor1102.macrion.core.ui.compose.MacrionActionField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionSwitchField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme

private fun groupedListItemShape(index: Int, itemCount: Int): Shape = when {
    itemCount == 1 -> RoundedCornerShape(16.dp)
    index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    index == itemCount - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    else -> RoundedCornerShape(4.dp)
}

@Composable
internal fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onShowPrivacySettings: () -> Unit,
    onShowPurchase: () -> Unit,
    onShowCrashReports: () -> Unit,
    onOpenGithub: () -> Unit,
    onJoinDiscord: () -> Unit,
    onReportBug: () -> Unit,
) {
    val isScenarioFiltersEnabled by viewModel.isScenarioFiltersUiEnabled.collectAsStateWithLifecycle(false)
    val isScenarioSwitcherEnabled by viewModel.isScenarioSwitcherEnabled.collectAsStateWithLifecycle(false)
    val isHomeButtonEnabled by viewModel.isHomeButtonEnabled.collectAsStateWithLifecycle(false)
    val isStopConfirmationEnabled by viewModel.isStopConfirmationEnabled.collectAsStateWithLifecycle(false)
    val isLegacyNotificationUiEnabled by viewModel.isLegacyNotificationUiEnabled.collectAsStateWithLifecycle(false)
    val isEntireScreenCaptureForced by viewModel.isEntireScreenCaptureForced.collectAsStateWithLifecycle(false)
    val isInputWorkaroundEnabled by viewModel.isInputWorkaroundEnabled.collectAsStateWithLifecycle(false)
    val shouldShowEntireScreenCapture by viewModel.shouldShowEntireScreenCapture.collectAsStateWithLifecycle(false)
    val shouldShowInputBlockWorkaround by viewModel.shouldShowInputBlockWorkaround.collectAsStateWithLifecycle(false)
    val shouldShowPrivacySettings by viewModel.shouldShowPrivacySettings.collectAsStateWithLifecycle(false)
    val shouldShowPurchase by viewModel.shouldShowPurchase.collectAsStateWithLifecycle(false)
    val toolbarScalePercent by viewModel.toolbarScalePercent.collectAsStateWithLifecycle(100)
    val isToolbarAutoHideEnabled by viewModel.isToolbarAutoHideEnabled.collectAsStateWithLifecycle(false)
    val allowPreviewHandleEditing by viewModel.allowPreviewHandleEditing.collectAsStateWithLifecycle(true)
    val toolbarAutoHideDelaySeconds by viewModel.toolbarAutoHideDelaySeconds.collectAsStateWithLifecycle(60)
    val areAdvancedSettingsEnabled by viewModel.areAdvancedSettingsEnabled.collectAsStateWithLifecycle(false)
    val hasSeenAdvancedWarning by viewModel.hasSeenAdvancedWarning.collectAsStateWithLifecycle(false)
    val maxToleratedDifference by viewModel.maxToleratedDifference.collectAsStateWithLifecycle(20)

    var displayedMaxDifference by remember { mutableIntStateOf(maxToleratedDifference) }
    LaunchedEffect(maxToleratedDifference, areAdvancedSettingsEnabled) {
        if (areAdvancedSettingsEnabled) {
            displayedMaxDifference = maxToleratedDifference
        }
    }

    val screenshotRateLimit by viewModel.screenshotRateLimitPerMinute.collectAsStateWithLifecycle(10)
    var displayedScreenshotRateLimit by remember { mutableIntStateOf(screenshotRateLimit) }
    LaunchedEffect(screenshotRateLimit, areAdvancedSettingsEnabled) {
        if (areAdvancedSettingsEnabled) {
            displayedScreenshotRateLimit = screenshotRateLimit
        }
    }

    var showTroubleshooting by rememberSaveable { mutableStateOf(false) }
    var showToolbarSizeDialog by rememberSaveable { mutableStateOf(false) }
    var showToolbarAutoHideDelayDialog by rememberSaveable { mutableStateOf(false) }
    var showAdvancedNoticeDialog by rememberSaveable { mutableStateOf(false) }
    var showMaxDifferenceDialog by rememberSaveable { mutableStateOf(false) }
    var showScreenshotRateLimitDialog by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val onCopyVersion = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Macrion version", BuildConfig.VERSION_NAME))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.version_copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
    }
    val installationSource = remember { detectInstallationSource(context) }
    val aboutSection = SettingsSection(
        R.string.settings_section_about,
        listOf(
            SettingsItem.Info(
                title = R.string.settings_version_title,
                value = BuildConfig.VERSION_NAME,
                showCopyIcon = true,
                onClick = onCopyVersion,
            ),
            SettingsItem.Info(
                title = R.string.settings_install_source_title,
                value = installationSource.getLabel(context),
            ),
        ),
    )

    MacrionTheme {
        SettingsScreen(
            sections = buildList {
                add(
                    SettingsSection(
                        R.string.settings_section_scenario_list,
                        listOf(SettingsItem.Switch(R.string.field_show_scenario_filters_ui_title, R.string.field_show_scenario_filters_ui_desc, isScenarioFiltersEnabled, viewModel::toggleScenarioFiltersUi)),
                    ),
                )
                add(
                    SettingsSection(
                        R.string.settings_section_scenario_editing,
                        listOf(
                            SettingsItem.Switch(
                                title = R.string.settings_preview_handle_editing_title,
                                description = R.string.settings_preview_handle_editing_desc,
                                checked = allowPreviewHandleEditing,
                                onClick = viewModel::togglePreviewHandleEditing,
                            ),
                        ),
                    ),
                )
                val autoHideDelayLabel = formatAutoHideDelay(toolbarAutoHideDelaySeconds)
                add(
                    SettingsSection(
                        R.string.settings_section_overlay,
                        listOf(
                            SettingsItem.Action(
                                title = R.string.settings_toolbar_size_title,
                                value = "$toolbarScalePercent%",
                                onClick = { showToolbarSizeDialog = true },
                            ),
                            SettingsItem.Switch(
                                title = R.string.settings_toolbar_auto_hide_title,
                                description = R.string.settings_toolbar_auto_hide_desc,
                                checked = isToolbarAutoHideEnabled,
                                childItem = SettingsItem.Action(
                                    title = R.string.settings_toolbar_auto_hide_delay_title,
                                    value = autoHideDelayLabel,
                                    onClick = { showToolbarAutoHideDelayDialog = true },
                                ),
                                isChildVisible = isToolbarAutoHideEnabled,
                                onClick = viewModel::toggleToolbarAutoHide,
                            ),
                            SettingsItem.Switch(R.string.field_scenario_switcher_title, R.string.field_scenario_switcher_desc, isScenarioSwitcherEnabled, viewModel::toggleScenarioSwitcher),
                            SettingsItem.Switch(R.string.field_home_button_title, R.string.field_home_button_desc, isHomeButtonEnabled, viewModel::toggleHomeButton),
                            SettingsItem.Switch(R.string.field_stop_confirmation_title, R.string.field_stop_confirmation_desc, isStopConfirmationEnabled, viewModel::toggleStopConfirmation),
                        ),
                    ),
                )
                add(
                    SettingsSection(
                        R.string.settings_section_compatibility,
                        listOf(
                            SettingsItem.Switch(R.string.field_legacy_notification_ui_title, R.string.field_legacy_notification_ui_desc, isLegacyNotificationUiEnabled, viewModel::toggleLegacyNotificationUi),
                        ),
                    ),
                )
                buildList {
                    if (shouldShowEntireScreenCapture) add(SettingsItem.Switch(R.string.field_force_entire_screen_title, R.string.field_force_entire_screen_desc, isEntireScreenCaptureForced, viewModel::toggleForceEntireScreenCapture))
                    if (shouldShowInputBlockWorkaround) add(SettingsItem.Switch(R.string.field_input_block_workaround_title, R.string.field_input_block_workaround_desc, isInputWorkaroundEnabled, viewModel::toggleInputBlockWorkaround))
                }.takeIf { it.isNotEmpty() }?.let { add(SettingsSection(R.string.settings_section_device_compatibility, it)) }
                buildList {
                    if (shouldShowPrivacySettings) add(SettingsItem.Action(R.string.field_privacy, onShowPrivacySettings))
                    if (shouldShowPurchase) add(SettingsItem.Action(R.string.field_remove_ads, onShowPurchase))
                }.takeIf { it.isNotEmpty() }?.let { add(SettingsSection(R.string.settings_section_account, it)) }
                add(
                    SettingsSection(
                        R.string.settings_section_help,
                        listOf(
                            SettingsItem.Action(R.string.field_troubleshooting) { showTroubleshooting = true },
                            SettingsItem.Action(R.string.crash_reports_title, onShowCrashReports),
                        ),
                    ),
                )
                val maxDiffLabel = if (displayedMaxDifference == 20) {
                    stringResource(R.string.settings_max_difference_item_default, 20)
                } else {
                    stringResource(R.string.settings_max_difference_item, displayedMaxDifference)
                }
                val screenshotRateLimitLabel = formatScreenshotRateLimit(displayedScreenshotRateLimit)
                add(
                    SettingsSection(
                        R.string.settings_section_advanced,
                        listOf(
                            SettingsItem.Switch(
                                title = R.string.settings_enable_advanced_title,
                                description = R.string.settings_enable_advanced_desc,
                                checked = areAdvancedSettingsEnabled,
                                childItems = listOf(
                                    SettingsItem.Action(
                                        title = R.string.settings_max_difference_title,
                                        value = maxDiffLabel,
                                        onClick = { showMaxDifferenceDialog = true },
                                    ),
                                    SettingsItem.Action(
                                        title = R.string.settings_screenshot_rate_limit_title,
                                        value = screenshotRateLimitLabel,
                                        onClick = { showScreenshotRateLimitDialog = true },
                                    ),
                                ),
                                isChildVisible = areAdvancedSettingsEnabled,
                                onClick = {
                                    if (!areAdvancedSettingsEnabled) {
                                        if (!hasSeenAdvancedWarning) {
                                            showAdvancedNoticeDialog = true
                                        } else {
                                            viewModel.setAdvancedSettingsEnabled(true)
                                        }
                                    } else {
                                        viewModel.setAdvancedSettingsEnabled(false)
                                        viewModel.setMaxToleratedDifference(20)
                                        viewModel.setScreenshotRateLimit(10)
                                        Toast.makeText(context, R.string.toast_advanced_settings_restored_defaults, Toast.LENGTH_SHORT).show()
                                    }
                                },
                            ),
                        ),
                    ),
                )
            },
            aboutSection = aboutSection,
            onNavigateBack = onNavigateBack,
            onOpenGithub = onOpenGithub,
            onJoinDiscord = onJoinDiscord,
            onReportBug = onReportBug,
        )

        if (showTroubleshooting) {
            AccessibilityTroubleshootingDialog(
                onDismiss = { showTroubleshooting = false },
            )
        }

        if (showToolbarSizeDialog) {
            ToolbarSizeDialog(
                currentPercent = toolbarScalePercent,
                onDismiss = { showToolbarSizeDialog = false },
                onConfirm = { percent ->
                    viewModel.setToolbarScalePercent(percent)
                    showToolbarSizeDialog = false
                },
            )
        }

        if (showToolbarAutoHideDelayDialog) {
            ToolbarAutoHideDelayDialog(
                currentDelaySeconds = toolbarAutoHideDelaySeconds,
                isAutoHideEnabled = isToolbarAutoHideEnabled,
                onDismiss = { showToolbarAutoHideDelayDialog = false },
                onConfirm = { seconds ->
                    if (seconds == AUTO_HIDE_DELAY_NEVER) {
                        viewModel.setToolbarAutoHideEnabled(false)
                    } else {
                        viewModel.setToolbarAutoHideDelaySeconds(seconds)
                        viewModel.setToolbarAutoHideEnabled(true)
                    }
                    showToolbarAutoHideDelayDialog = false
                },
            )
        }

        if (showAdvancedNoticeDialog) {
            AlertDialog(
                onDismissRequest = { showAdvancedNoticeDialog = false },
                title = {
                    Text(stringResource(R.string.settings_advanced_dialog_title))
                },
                text = {
                    Text(stringResource(R.string.settings_advanced_dialog_message))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.setHasSeenAdvancedWarning(true)
                            viewModel.setAdvancedSettingsEnabled(true)
                            showAdvancedNoticeDialog = false
                        },
                    ) {
                        Text(stringResource(R.string.settings_advanced_dialog_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdvancedNoticeDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }

        if (showMaxDifferenceDialog) {
            MaxDifferenceDialog(
                currentDifference = maxToleratedDifference,
                onDismiss = { showMaxDifferenceDialog = false },
                onConfirm = { diff ->
                    viewModel.setMaxToleratedDifference(diff)
                    showMaxDifferenceDialog = false
                },
            )
        }

        if (showScreenshotRateLimitDialog) {
            ScreenshotRateLimitDialog(
                currentLimit = screenshotRateLimit,
                onDismiss = { showScreenshotRateLimitDialog = false },
                onConfirm = { limit ->
                    viewModel.setScreenshotRateLimit(limit)
                    showScreenshotRateLimitDialog = false
                },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsScreen(
    sections: List<SettingsSection>,
    aboutSection: SettingsSection,
    onNavigateBack: () -> Unit,
    onOpenGithub: () -> Unit,
    onJoinDiscord: () -> Unit,
    onReportBug: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activity_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.content_desc_back),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            sections.forEach { section ->
                item {
                    SettingsSection(section)
                }
            }
            item {
                SupportCards(
                    onOpenGithub = onOpenGithub,
                    onJoinDiscord = onJoinDiscord,
                    onReportBug = onReportBug,
                )
            }
            item {
                SettingsSection(aboutSection)
            }
            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(section: SettingsSection) {
    Text(
        text = stringResource(section.title),
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
    )
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        section.items.forEachIndexed { index, item ->
            key(item.title) {
                val isOnlyOrLastItem = index == section.items.lastIndex
                val hasChild = item is SettingsItem.Switch && item.childItems.isNotEmpty()
                val isChildVisible = item is SettingsItem.Switch && item.isChildVisible

                val bottomCorners by animateDpAsState(
                    targetValue = if (isChildVisible && isOnlyOrLastItem) 4.dp else if (isOnlyOrLastItem) 16.dp else 4.dp,
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "cardBottomCorners_${item.title}",
                )

                val itemShape = when {
                    section.items.size == 1 -> RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = bottomCorners,
                        bottomEnd = bottomCorners,
                    )
                    index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    isOnlyOrLastItem -> RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = bottomCorners,
                        bottomEnd = bottomCorners,
                    )
                    else -> RoundedCornerShape(4.dp)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = itemShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    SettingsRow(item)
                }

                if (item is SettingsItem.Switch && item.childItems.isNotEmpty()) {
                    AnimatedVisibility(
                        visible = isChildVisible,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMedium,
                            ),
                            expandFrom = Alignment.Top,
                        ) + fadeIn(
                            animationSpec = tween(200),
                        ),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMedium,
                            ),
                            shrinkTowards = Alignment.Top,
                        ) + fadeOut(
                            animationSpec = tween(150),
                        ),
                    ) {
                        Column {
                            item.childItems.forEachIndexed { childIndex, child ->
                                Spacer(Modifier.height(4.dp))
                                val isLastChild = childIndex == item.childItems.lastIndex
                                val childShape = if (isOnlyOrLastItem && isLastChild) {
                                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                                } else {
                                    RoundedCornerShape(4.dp)
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = childShape,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                ) {
                                    SettingsRow(child)
                                }
                            }
                        }
                    }
                }

                if (index != section.items.lastIndex) {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun SupportCards(onOpenGithub: () -> Unit, onJoinDiscord: () -> Unit, onReportBug: () -> Unit) {
    Text(
        text = stringResource(R.string.settings_support_title),
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
    )
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SupportCard(stringResource(R.string.settings_github), R.drawable.ic_github, onOpenGithub, groupedListItemShape(0, 3))
        Spacer(Modifier.height(4.dp))
        SupportCard(stringResource(R.string.settings_discord), R.drawable.ic_discord, onJoinDiscord, groupedListItemShape(1, 3))
        Spacer(Modifier.height(4.dp))
        SupportCard(stringResource(R.string.settings_report_bug), R.drawable.ic_bug_report, onReportBug, groupedListItemShape(2, 3))
    }
}

@Composable
private fun SupportCard(
    title: String,
    icon: Int,
    onClick: () -> Unit,
    shape: Shape,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsRow(item: SettingsItem) {
    when (item) {
        is SettingsItem.Switch -> MacrionSwitchField(
            title = stringResource(item.title),
            description = stringResource(item.description),
            checked = item.checked,
            onClick = item.onClick,
        )
        is SettingsItem.Action -> MacrionActionField(
            title = stringResource(item.title),
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.value != null) {
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                    )
                }
            },
            onClick = item.onClick,
        )
        is SettingsItem.Info -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (item.showCopyIcon) Modifier.clickable(role = Role.Button, onClick = item.onClick) else Modifier)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(item.title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = item.value,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (item.showCopyIcon) {
                    Icon(
                        painter = painterResource(R.drawable.ic_copy),
                        contentDescription = stringResource(R.string.crash_report_copy),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private sealed interface SettingsItem {
    val title: Int
    val onClick: () -> Unit

    data class Switch(
        @param:StringRes override val title: Int,
        @param:StringRes val description: Int,
        val checked: Boolean,
        override val onClick: () -> Unit,
        val childItems: List<SettingsItem> = emptyList(),
        val isChildVisible: Boolean = false,
    ) : SettingsItem {
        constructor(
            @StringRes title: Int,
            @StringRes description: Int,
            checked: Boolean,
            onClick: () -> Unit,
            childItem: SettingsItem,
            isChildVisible: Boolean = false,
        ) : this(
            title = title,
            description = description,
            checked = checked,
            onClick = onClick,
            childItems = listOf(childItem),
            isChildVisible = isChildVisible,
        )
    }

    data class Action(
        @param:StringRes override val title: Int,
        val value: String? = null,
        override val onClick: () -> Unit,
    ) : SettingsItem {
        constructor(
            @StringRes title: Int,
            onClick: () -> Unit,
        ) : this(title = title, value = null, onClick = onClick)
    }

    data class Info(
        @param:StringRes override val title: Int,
        val value: String,
        val showCopyIcon: Boolean = false,
        override val onClick: () -> Unit = {},
    ) : SettingsItem
}

private data class SettingsSection(
    @param:StringRes val title: Int,
    val items: List<SettingsItem>,
)
