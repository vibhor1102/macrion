/* Copyright (C) 2026 Vibhor Goel — GPLv3 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.click

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.feature.smart.config.R

/** The same click target control is used by a standalone click and a MultiTouch child. */
@Composable
fun ClickPositionEditor(
    state: ClickPositionUiState,
    onTypeSelected: (Click.PositionType) -> Unit,
    onPositionSelected: () -> Unit,
    onConditionSelected: () -> Unit,
    onOffsetSelected: () -> Unit,
    typeModifier: @Composable (Click.PositionType) -> Modifier = { Modifier },
    selectorModifier: Modifier = Modifier,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (state.isTypeFieldVisible) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.field_click_type_title), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(if (state.positionType == Click.PositionType.USER_SELECTED)
                            R.string.field_click_type_desc_on_position else R.string.field_click_type_desc_on_condition),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val shape = RoundedCornerShape(20.dp)
                    Row(Modifier.height(32.dp).clip(shape).border(1.dp, MaterialTheme.colorScheme.outline, shape)) {
                        listOf(Click.PositionType.USER_SELECTED to R.drawable.ic_click_on_condition,
                            Click.PositionType.ON_DETECTED_CONDITION to R.drawable.ic_condition).forEachIndexed { index, item ->
                            if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline))
                            Box(Modifier.width(44.dp).fillMaxHeight()
                                .background(if (state.positionType == item.first) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                .then(typeModifier(item.first)).clickable { onTypeSelected(item.first) },
                                contentAlignment = Alignment.Center) {
                                Icon(painterResource(item.second), null, Modifier.size(18.dp))
                            }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(top = 8.dp))
            }
            ClickSelectorRow(state.selectorTitle, state.selectorDescription,
                state.isSelectorEnabled, state.isSelectorInError, state.selectorVisualization,
                if (state.positionType == Click.PositionType.USER_SELECTED) onPositionSelected else onConditionSelected,
                selectorModifier)
            if (state.isClickOffsetVisible) {
                HorizontalDivider()
                ClickSelectorRow(stringResource(R.string.field_click_offset_title), state.clickOffsetDescription,
                    state.isClickOffsetEnabled, false, null, onOffsetSelected)
            }
        }
    }
}

@Composable
private fun ClickSelectorRow(
    title: String, description: String?, enabled: Boolean, error: Boolean,
    visualization: Any?, onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Row(modifier.fillMaxWidth().heightIn(min = 62.dp).clickable(enabled = enabled, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        visualization?.let { value ->
            val bitmap = remember(value) { when (value) { is Bitmap -> value; is Drawable -> value.toBitmap(); else -> null } }
            bitmap?.let { Image(it.asImageBitmap(), null, Modifier.size(40.dp).padding(end = 8.dp), contentScale = ContentScale.Fit) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (error) MaterialTheme.colorScheme.error else contentColor)
            description?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                color = if (error) MaterialTheme.colorScheme.error else contentColor.copy(alpha = 0.75f)) }
        }
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = contentColor)
    }
}

@Composable
fun ColumnScope.ClickFields(
    name: String, duration: String, nameError: Boolean, durationError: Boolean,
    positionState: ClickPositionUiState?, maxNameLength: Int,
    waitBefore: String, waitAfter: String,
    onNameChanged: (String) -> Unit, onDurationChanged: (String) -> Unit,
    onTypeSelected: (Click.PositionType) -> Unit,
    onPositionSelected: () -> Unit, onConditionSelected: () -> Unit, onOffsetSelected: () -> Unit,
    onWaitBeforeChanged: (String) -> Unit, onWaitAfterChanged: (String) -> Unit,
    typeModifier: @Composable (Click.PositionType) -> Modifier = { Modifier },
    selectorModifier: Modifier = Modifier,
) {
    io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField(name, onNameChanged,
        stringResource(R.string.generic_name), isError = nameError, maxLength = maxNameLength)
    OutlinedTextField(duration, { onDurationChanged(it.filter(Char::isDigit)) }, Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.input_field_label_click_press_duration)) },
        isError = durationError, singleLine = true,
        keyboardOptions = io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions(
            androidx.compose.ui.text.input.KeyboardType.Number),
        keyboardActions = io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions())
    positionState?.let { ClickPositionEditor(it, onTypeSelected, onPositionSelected,
        onConditionSelected, onOffsetSelected, typeModifier, selectorModifier) }
    io.github.vibhor1102.macrion.core.ui.compose.ActionDelaysCard(
        waitBefore, waitAfter, onWaitBeforeChanged, onWaitAfterChanged)
}
