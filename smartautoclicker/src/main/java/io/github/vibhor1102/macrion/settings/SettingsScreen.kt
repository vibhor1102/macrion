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

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.R
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
    onShowTroubleshooting: () -> Unit,
    onShowCrashReports: () -> Unit,
    onOpenGithub: () -> Unit,
    onJoinDiscord: () -> Unit,
    onReportBug: () -> Unit,
) {
    val isScenarioFiltersEnabled by viewModel.isScenarioFiltersUiEnabled.collectAsStateWithLifecycle(false)
    val isScenarioSwitcherEnabled by viewModel.isScenarioSwitcherEnabled.collectAsStateWithLifecycle(false)
    val isHomeButtonEnabled by viewModel.isHomeButtonEnabled.collectAsStateWithLifecycle(false)
    val isStopConfirmationEnabled by viewModel.isStopConfirmationEnabled.collectAsStateWithLifecycle(false)
    val isLegacyActionUiEnabled by viewModel.isLegacyActionUiEnabled.collectAsStateWithLifecycle(false)
    val isLegacyNotificationUiEnabled by viewModel.isLegacyNotificationUiEnabled.collectAsStateWithLifecycle(false)
    val isEntireScreenCaptureForced by viewModel.isEntireScreenCaptureForced.collectAsStateWithLifecycle(false)
    val isInputWorkaroundEnabled by viewModel.isInputWorkaroundEnabled.collectAsStateWithLifecycle(false)
    val shouldShowEntireScreenCapture by viewModel.shouldShowEntireScreenCapture.collectAsStateWithLifecycle(false)
    val shouldShowInputBlockWorkaround by viewModel.shouldShowInputBlockWorkaround.collectAsStateWithLifecycle(false)
    val shouldShowPrivacySettings by viewModel.shouldShowPrivacySettings.collectAsStateWithLifecycle(false)
    val shouldShowPurchase by viewModel.shouldShowPurchase.collectAsStateWithLifecycle(false)

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
                        R.string.settings_section_overlay,
                        listOf(
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
                            SettingsItem.Switch(R.string.field_legacy_action_ui_title, R.string.field_legacy_action_ui_desc, isLegacyActionUiEnabled, viewModel::toggleLegacyActionUi),
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
                            SettingsItem.Action(R.string.field_troubleshooting, onShowTroubleshooting),
                            SettingsItem.Action(R.string.crash_reports_title, onShowCrashReports),
                        ),
                    ),
                )
            },
            onNavigateBack = onNavigateBack,
            onOpenGithub = onOpenGithub,
            onJoinDiscord = onJoinDiscord,
            onReportBug = onReportBug,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsScreen(
    sections: List<SettingsSection>,
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = groupedListItemShape(index, section.items.size),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                SettingsRow(item)
            }
            if (index != section.items.lastIndex) {
                Spacer(Modifier.height(4.dp))
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
                Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                )
            },
            onClick = item.onClick,
        )
    }
}

private sealed interface SettingsItem {
    @get:StringRes val title: Int
    val onClick: () -> Unit

    data class Switch(
        @param:StringRes override val title: Int,
        @param:StringRes val description: Int,
        val checked: Boolean,
        override val onClick: () -> Unit,
    ) : SettingsItem

    data class Action(
        @param:StringRes override val title: Int,
        override val onClick: () -> Unit,
    ) : SettingsItem
}

private data class SettingsSection(
    @param:StringRes val title: Int,
    val items: List<SettingsItem>,
)
