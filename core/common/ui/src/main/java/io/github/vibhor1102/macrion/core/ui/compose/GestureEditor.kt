/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalDensity
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
    deleteEnabled: Boolean = true,
    waitBefore: String = "",
    waitAfter: String = "",
    maxWaitBeforeMs: Long? = null,
    onNameChanged: (String) -> Unit,
    onDurationChanged: (String) -> Unit,
    onPositionClicked: () -> Unit,
    onWaitBeforeChanged: (String) -> Unit = {},
    onWaitAfterChanged: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    headerActions: @Composable (() -> Unit)? = null,
) {
    Surface(
        shape = OverlayDialogShape,
        modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        MacrionDialogSurface {
            Column(Modifier.fillMaxWidth()) {
                GestureEditorTopBar(title, saveEnabled, onDismiss, onDelete, onSave, deleteEnabled, headerActions)
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PositionGestureFields(name, duration, positionTitle, positionDescription,
                        nameLabel, durationLabel, nameError, durationError, positionError, maxNameLength,
                        waitBefore, waitAfter, onNameChanged, onDurationChanged, onPositionClicked,
                        onWaitBeforeChanged, onWaitAfterChanged, maxWaitBeforeMs)
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
    deleteEnabled: Boolean = true,
    waitBefore: String = "", waitAfter: String = "",
    maxWaitBeforeMs: Long? = null,
    showRepetition: Boolean = true,
    onNameChanged: (String) -> Unit,
    onDurationChanged: (String) -> Unit, onRepeatCountChanged: (String) -> Unit,
    onRepeatDelayChanged: (String) -> Unit, onInfiniteRepeatChanged: () -> Unit,
    onPositionClicked: () -> Unit,
    onWaitBeforeChanged: (String) -> Unit = {},
    onWaitAfterChanged: (String) -> Unit = {},
    onDismiss: () -> Unit, onDelete: () -> Unit, onSave: () -> Unit,
    headerActions: @Composable (() -> Unit)? = null,
) {
    Surface(
        shape = OverlayDialogShape,
        modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        MacrionDialogSurface {
            Column(Modifier.fillMaxWidth()) {
                GestureEditorTopBar(title, saveEnabled, onDismiss, onDelete, onSave, deleteEnabled, headerActions)
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GestureFields(name, duration, repeatCount, repeatDelay, positionTitle,
                        positionDescription, nameLabel, durationLabel, repeatCountLabel, repeatDelayLabel,
                        nameError, durationError, repeatCountError, repeatDelayError, infiniteRepeat,
                        maxNameLength, infiniteRepeatIcon, showRepetition, waitBefore, waitAfter,
                        onNameChanged = onNameChanged, onDurationChanged = onDurationChanged,
                        onRepeatCountChanged = onRepeatCountChanged, onRepeatDelayChanged = onRepeatDelayChanged,
                        onInfiniteRepeatChanged = onInfiniteRepeatChanged,
                        onPositionClicked = onPositionClicked,
                        onWaitBeforeChanged = onWaitBeforeChanged,
                        onWaitAfterChanged = onWaitAfterChanged,
                        maxWaitBeforeMs = maxWaitBeforeMs)
                }
            }
        }
    }
}

@Composable
fun ColumnScope.PositionGestureFields(
    name: String, duration: String, positionTitle: String, positionDescription: String,
    nameLabel: String, durationLabel: String, nameError: Boolean, durationError: Boolean,
    positionError: Boolean, maxNameLength: Int, waitBefore: String = "", waitAfter: String = "",
    onNameChanged: (String) -> Unit, onDurationChanged: (String) -> Unit,
    onPositionClicked: () -> Unit, onWaitBeforeChanged: (String) -> Unit = {},
    onWaitAfterChanged: (String) -> Unit = {},
    maxWaitBeforeMs: Long? = null,
) {
    MacrionTextField(name, onNameChanged, nameLabel, isError = nameError, maxLength = maxNameLength)
    NumericField(duration, durationLabel, durationError, onDurationChanged)
    PositionCard(positionTitle, positionDescription, positionError, onPositionClicked)
    ActionDelaysCard(waitBefore, waitAfter, onWaitBeforeChanged, onWaitAfterChanged,
        maxWaitBeforeMs = maxWaitBeforeMs)
}

@Composable
fun ColumnScope.GestureFields(
    name: String, duration: String, repeatCount: String, repeatDelay: String,
    positionTitle: String, positionDescription: String, nameLabel: String, durationLabel: String,
    repeatCountLabel: String, repeatDelayLabel: String, nameError: Boolean, durationError: Boolean,
    repeatCountError: Boolean, repeatDelayError: Boolean, infiniteRepeat: Boolean,
    maxNameLength: Int, @DrawableRes infiniteRepeatIcon: Int, showRepetition: Boolean = true,
    waitBefore: String = "", waitAfter: String = "", positionError: Boolean = false,
    onNameChanged: (String) -> Unit, onDurationChanged: (String) -> Unit,
    onRepeatCountChanged: (String) -> Unit, onRepeatDelayChanged: (String) -> Unit,
    onInfiniteRepeatChanged: () -> Unit, onPositionClicked: () -> Unit,
    onWaitBeforeChanged: (String) -> Unit = {}, onWaitAfterChanged: (String) -> Unit = {},
    maxWaitBeforeMs: Long? = null,
) {
    MacrionTextField(name, onNameChanged, nameLabel, isError = nameError, maxLength = maxNameLength)
    NumericField(duration, durationLabel, durationError, onDurationChanged)
    if (showRepetition) {
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
    }
    PositionCard(positionTitle, positionDescription, positionError, onPositionClicked)
    ActionDelaysCard(waitBefore, waitAfter, onWaitBeforeChanged, onWaitAfterChanged,
        maxWaitBeforeMs = maxWaitBeforeMs)
}

@Composable
private fun GestureEditorTopBar(
    title: String, saveEnabled: Boolean, onDismiss: () -> Unit,
    onDelete: () -> Unit, onSave: () -> Unit, deleteEnabled: Boolean,
    headerActions: @Composable (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDismiss) { Icon(painterResource(R.drawable.ic_cancel), null) }
        Text(title, Modifier.weight(1f).padding(horizontal = 8.dp), style = MaterialTheme.typography.titleLarge)
        headerActions?.invoke()
        FilledTonalIconButton(onClick = onDelete, enabled = deleteEnabled) { Icon(painterResource(R.drawable.ic_delete), null) }
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
fun NumericField(
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

@Composable
fun ActionDelaysCard(
    waitBefore: String,
    waitAfter: String,
    onWaitBeforeChanged: (String) -> Unit,
    onWaitAfterChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxWaitBeforeMs: Long? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var beforeText by rememberSaveable { mutableStateOf(waitBefore) }
    var afterText by rememberSaveable { mutableStateOf(waitAfter) }
    var beforeEdited by rememberSaveable { mutableStateOf(false) }
    var afterEdited by rememberSaveable { mutableStateOf(false) }
    var beforeField by remember { mutableStateOf(TextFieldValue(beforeText)) }
    var afterField by remember { mutableStateOf(TextFieldValue(afterText)) }
    val bottomRequester = remember { BringIntoViewRequester() }
    val fieldsVisibility = remember { MutableTransitionState(expanded) }
    var scrollCheckpoint by remember { mutableIntStateOf(0) }
    val scrollStep = with(LocalDensity.current) { 32.dp.roundToPx() }
    fieldsVisibility.targetState = expanded

    LaunchedEffect(waitBefore) {
        if (!beforeEdited) {
            beforeText = waitBefore
            beforeField = TextFieldValue(waitBefore)
        }
    }
    LaunchedEffect(waitAfter) {
        if (!afterEdited) {
            afterText = waitAfter
            afterField = TextFieldValue(waitAfter)
        }
    }
    LaunchedEffect(expanded, fieldsVisibility.isIdle, scrollCheckpoint) {
        if (expanded) {
            withFrameNanos { }
            bottomRequester.bringIntoView()
        }
    }
    val beforeError = beforeText.isNotBlank() && beforeText.toLongOrNull()?.let {
        it >= 0L && (maxWaitBeforeMs == null || it <= maxWaitBeforeMs)
    } != true
    val afterError = afterText.isNotBlank() && afterText.toLongOrNull()?.let { it >= 0L } != true
    val hasError = beforeError || afterError
    val expandedState = stringResource(if (expanded) R.string.field_delays_expanded else R.string.field_delays_collapsed)
    val beforeSummary = beforeText.takeIf(String::isNotBlank)?.let {
        stringResource(R.string.field_wait_before_summary, it)
    }
    val afterSummary = afterText.takeIf(String::isNotBlank)?.let {
        stringResource(R.string.field_wait_after_summary, it)
    }
    val collapsedSummary = when {
        hasError -> stringResource(R.string.field_delays_check_values)
        beforeText.isNotBlank() || afterText.isNotBlank() -> buildString {
            if (beforeSummary != null) append(beforeSummary)
            if (beforeText.isNotBlank() && afterText.isNotBlank()) append(" • ")
            if (afterSummary != null) append(afterSummary)
        }
        else -> null
    }

    Column(modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button) {
                            expanded = !expanded
                            if (expanded) scrollCheckpoint = 0
                        }
                        .semantics { stateDescription = expandedState },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_duration),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.field_delays_title),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        AnimatedVisibility(
                            visible = !expanded && collapsedSummary != null,
                            enter = expandVertically(
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
                                expandFrom = Alignment.Top,
                            ),
                            exit = shrinkVertically(
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
                                shrinkTowards = Alignment.Top,
                            ),
                        ) {
                            Text(
                                text = collapsedSummary.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasError) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        painter = painterResource(if (expanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }

                AnimatedVisibility(
                    visibleState = fieldsVisibility,
                    modifier = Modifier.onSizeChanged { size ->
                        if (expanded && !fieldsVisibility.isIdle && size.height >= scrollCheckpoint + scrollStep) {
                            scrollCheckpoint = size.height
                        }
                    },
                    enter = expandVertically(
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
                        expandFrom = Alignment.Top,
                    ),
                    exit = shrinkVertically(
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
                        shrinkTowards = Alignment.Top,
                    ),
                ) {
                    BoxWithConstraints(Modifier.padding(top = 12.dp)) {
                        val before: @Composable (Modifier) -> Unit = { fieldModifier ->
                            DelayNumberField(beforeField, stringResource(R.string.field_wait_before_title),
                                maxWaitBeforeMs, beforeError, fieldModifier) { input ->
                                beforeField = input
                                beforeEdited = true
                                if (input.text != beforeText) {
                                    beforeText = input.text
                                    onWaitBeforeChanged(input.text)
                                }
                            }
                        }
                        val after: @Composable (Modifier) -> Unit = { fieldModifier ->
                            DelayNumberField(afterField, stringResource(R.string.field_wait_after_title),
                                null, afterError, fieldModifier) { input ->
                                afterField = input
                                afterEdited = true
                                if (input.text != afterText) {
                                    afterText = input.text
                                    onWaitAfterChanged(input.text)
                                }
                            }
                        }
                        if (maxWidth >= 280.dp * LocalDensity.current.fontScale) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                before(Modifier.weight(1f))
                                after(Modifier.weight(1f))
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                before(Modifier.fillMaxWidth())
                                after(Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp).bringIntoViewRequester(bottomRequester))
    }
}

@Composable
private fun DelayNumberField(
    value: TextFieldValue,
    label: String,
    maxValue: Long?,
    isError: Boolean,
    modifier: Modifier,
    onValueChanged: (TextFieldValue) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.text.all { it in '0'..'9' }) onValueChanged(input)
        },
        modifier = modifier,
        label = { Text(label) },
        isError = isError,
        supportingText = if (isError) {
            {
                Text(if (maxValue != null) stringResource(R.string.field_delay_range_error, maxValue)
                    else stringResource(R.string.field_delay_number_error))
            }
        } else null,
        singleLine = true,
        keyboardOptions = macrionDoneKeyboardOptions(KeyboardType.Number),
        keyboardActions = macrionDoneKeyboardActions(),
    )
}
