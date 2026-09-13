/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.ui.R

@Composable
fun MacrionPositionGestureEditor(
    title: String,
    name: String,
    duration: String,
    positionTitle: String,
    positionDescription: String,
    nameLabel: String,
    durationLabel: String,
    nameError: Boolean,
    durationError: Boolean,
    positionError: Boolean,
    saveEnabled: Boolean,
    maxNameLength: Int,
    onNameChanged: (String) -> Unit,
    onDurationChanged: (String) -> Unit,
    onPositionClicked: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        MacrionDialogSurface {
            Column(Modifier.fillMaxWidth()) {
                GestureEditorTopBar(title, saveEnabled, onDismiss, onDelete, onSave)
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MacrionTextField(name, onNameChanged, nameLabel, isError = nameError, maxLength = maxNameLength)
                    NumericField(duration, durationLabel, durationError, onDurationChanged)
                    PositionCard(positionTitle, positionDescription, positionError, onPositionClicked)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun MacrionGestureEditor(
    title: String, name: String, duration: String, repeatCount: String, repeatDelay: String,
    positionTitle: String, positionDescription: String, nameLabel: String, durationLabel: String,
    repeatCountLabel: String, repeatDelayLabel: String, nameError: Boolean, durationError: Boolean,
    repeatCountError: Boolean, repeatDelayError: Boolean, infiniteRepeat: Boolean,
    saveEnabled: Boolean, maxNameLength: Int, @DrawableRes infiniteRepeatIcon: Int,
    onNameChanged: (String) -> Unit,
    onDurationChanged: (String) -> Unit, onRepeatCountChanged: (String) -> Unit,
    onRepeatDelayChanged: (String) -> Unit, onInfiniteRepeatChanged: () -> Unit,
    onPositionClicked: () -> Unit, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        MacrionDialogSurface {
            Column(Modifier.fillMaxWidth()) {
                GestureEditorTopBar(title, saveEnabled, onDismiss, onDelete, onSave)
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MacrionTextField(name, onNameChanged, nameLabel, isError = nameError, maxLength = maxNameLength)
                    NumericField(duration, durationLabel, durationError, onDurationChanged)
                    Row(verticalAlignment = Alignment.Top) {
                        NumericField(repeatCount, repeatCountLabel, repeatCountError, onRepeatCountChanged,
                            Modifier.weight(1f), enabled = !infiniteRepeat)
                        Spacer(Modifier.width(16.dp))
                        OutlinedIconToggleButton(
                            checked = infiniteRepeat,
                            onCheckedChange = { onInfiniteRepeatChanged() },
                            modifier = Modifier.padding(top = 8.dp).size(48.dp),
                        ) {
                            Icon(painterResource(infiniteRepeatIcon), repeatCountLabel, Modifier.size(24.dp))
                        }
                    }
                    NumericField(repeatDelay, repeatDelayLabel, repeatDelayError, onRepeatDelayChanged)
                    PositionCard(positionTitle, positionDescription, false, onPositionClicked)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun GestureEditorTopBar(
    title: String, saveEnabled: Boolean, onDismiss: () -> Unit,
    onDelete: () -> Unit, onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDismiss) { Icon(painterResource(R.drawable.ic_cancel), null) }
        Text(title, Modifier.weight(1f).padding(horizontal = 8.dp), style = MaterialTheme.typography.titleLarge)
        FilledTonalIconButton(onClick = onDelete) { Icon(painterResource(R.drawable.ic_delete), null) }
        Spacer(Modifier.width(8.dp))
        FilledIconButton(onClick = onSave, enabled = saveEnabled) {
            Icon(painterResource(R.drawable.ic_save_filled), null)
        }
    }
}

@Composable
private fun PositionCard(
    title: String, description: String, isError: Boolean, onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            .clickable(role = Role.Button, onClick = onClick),
        border = if (isError) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NumericField(
    value: String, label: String, isError: Boolean, onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value, onValueChange = { onValueChanged(it.filter(Char::isDigit)) },
        label = { Text(label) }, modifier = modifier.fillMaxWidth(), isError = isError,
        enabled = enabled, singleLine = true,
        keyboardOptions = macrionDoneKeyboardOptions(KeyboardType.Number),
        keyboardActions = macrionDoneKeyboardActions(),
    )
}
