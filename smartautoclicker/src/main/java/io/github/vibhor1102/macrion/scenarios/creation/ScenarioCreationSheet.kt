/* Copyright (C) 2023 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.scenarios.creation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.R
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioCreationSheet(
    viewModel: ScenarioCreationViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun dismiss() {
        viewModel.reset()
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = ::dismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        MacrionDialogSurface {
            ScenarioCreationContent(viewModel, ::dismiss)
        }
    }
}

@Composable
private fun ScenarioCreationContent(viewModel: ScenarioCreationViewModel, onDismiss: () -> Unit) {
    val name by viewModel.name.collectAsStateWithLifecycle("")
    val nameError by viewModel.nameError.collectAsStateWithLifecycle(false)
    val selection by viewModel.scenarioTypeSelectionState.collectAsStateWithLifecycle(null)
    val creationState by viewModel.creationState.collectAsStateWithLifecycle(CreationState.CONFIGURING)
    val showWarning by viewModel.showPaidLimitationWarning.collectAsStateWithLifecycle(false)
    LaunchedEffect(creationState) {
        if (creationState == CreationState.SAVED) onDismiss()
    }

    Column(Modifier.fillMaxWidth()) {
        ScenarioCreationTopBar(
            saveEnabled = creationState == CreationState.CONFIGURING,
            onDismiss = onDismiss,
            onSave = viewModel::createScenario,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            MacrionTextField(
                value = name,
                onValueChange = viewModel::setName,
                label = stringResource(R.string.input_field_label_scenario_name),
                isError = nameError,
                maxLength = 60,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    ScenarioTypeCard(
                        item = ScenarioTypeItem.Dumb,
                        selected = selection?.selectedItem == ScenarioTypeSelection.DUMB,
                        showWarning = false,
                        onClick = { viewModel.setSelectedType(ScenarioTypeSelection.DUMB) },
                    )
                    Spacer(Modifier.height(12.dp))
                    ScenarioTypeCard(
                        item = ScenarioTypeItem.Smart,
                        selected = selection?.selectedItem == ScenarioTypeSelection.SMART,
                        showWarning = showWarning,
                        onClick = { viewModel.setSelectedType(ScenarioTypeSelection.SMART) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScenarioCreationTopBar(
    saveEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp)
            .shadow(3.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(UiR.drawable.ic_cancel),
                    contentDescription = stringResource(android.R.string.cancel),
                )
            }
            Text(
                text = stringResource(R.string.dialog_title_add_scenario),
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(
                onClick = onSave,
                enabled = saveEnabled,
                colors = IconButtonDefaults.filledIconButtonColors(),
            ) {
                Icon(
                    painter = painterResource(UiR.drawable.ic_save_filled),
                    contentDescription = stringResource(android.R.string.ok),
                )
            }
        }
    }
}

@Composable
private fun ScenarioTypeCard(item: ScenarioTypeItem, selected: Boolean, showWarning: Boolean, onClick: () -> Unit) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = contentColor,
                )
                Text(
                    stringResource(item.titleRes),
                    modifier = Modifier.weight(1f).padding(start = 8.dp, end = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor,
                )
                if (selected) {
                    Surface(
                        modifier = Modifier.size(22.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("✓", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Text(
                stringResource(item.descriptionText),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
            if (showWarning && item is ScenarioTypeItem.Smart) {
                Text(
                    stringResource(R.string.item_desc_smart_scenario_pro_mode),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
