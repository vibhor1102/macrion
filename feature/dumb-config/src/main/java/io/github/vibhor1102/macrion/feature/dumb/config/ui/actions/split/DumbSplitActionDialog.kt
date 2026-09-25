/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.split

import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import io.github.vibhor1102.macrion.core.ui.R as UiR
import io.github.vibhor1102.macrion.core.ui.compose.ActionDelaysCard
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.NumericField
import io.github.vibhor1102.macrion.core.ui.compose.OverlayDialogShape
import io.github.vibhor1102.macrion.core.ui.compose.ExistingActionPicker
import io.github.vibhor1102.macrion.feature.dumb.config.R
import io.github.vibhor1102.macrion.feature.dumb.config.di.DumbConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.click.DumbClickDialog
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.swipe.DumbSwipeDialog
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.DumbCombinationOptions
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.DumbActionHeaderMenu
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.toPickerOptions

class DumbSplitActionDialog(
    private val dumbSplitAction: DumbAction.DumbSplitAction,
    private val onConfirmClicked: (DumbAction.DumbSplitAction) -> Unit,
    private val onDeleteClicked: (DumbAction.DumbSplitAction) -> Unit,
    private val onDismissClicked: () -> Unit,
    private val onUnsplitClicked: ((DumbAction.DumbSplitAction) -> Unit)? = null,
    private val onConfigureSubAction: ((parent: DumbAction.DumbSplitAction, subIndex: Int) -> Unit)? = null,
    private val combinationOptions: DumbCombinationOptions? = null,
    private val closeSourceEditorOnSave: Boolean = false,
) : OverlayDialog(R.style.AppTheme) {

    private var showDiscardConfirmation by mutableStateOf(false)
    private var showingExistingPicker by mutableStateOf(false)

    private val viewModel: DumbSplitActionViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbSplitActionViewModel() },
    )

    override fun onCreateView(): ViewGroup {
        viewModel.setEditedDumbSplit(dumbSplitAction)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@DumbSplitActionDialog.Content() } }
        }
    }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val ui = state ?: return
        val existing = combinationOptions?.existingActions.orEmpty().filter { details ->
            ui.subActions.size + ((details.action as? DumbAction.DumbSplitAction)?.subActions?.size ?: 1) <= 10
        }
        if (showingExistingPicker) {
            ExistingActionPicker(existing.toPickerOptions(), onBack = { showingExistingPicker = false }) { other ->
                val source = viewModel.getEditedDumbSplit() ?: return@ExistingActionPicker
                combinationOptions?.onExisting?.invoke(source, other)
            }
            return
        }
        if (showDiscardConfirmation) {
            AlertDialog(
                onDismissRequest = { showDiscardConfirmation = false },
                title = { Text(stringResource(R.string.split_discard_title)) },
                text = { Text(stringResource(R.string.split_discard_message)) },
                confirmButton = { TextButton(onClick = { showDiscardConfirmation = false; dismissDraft() }) {
                    Text(stringResource(R.string.split_discard_confirm))
                } },
                dismissButton = { TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text(stringResource(R.string.split_keep_editing))
                } },
            )
        }
        var name by rememberSaveable { mutableStateOf(ui.name) }
        var count by rememberSaveable { mutableStateOf(ui.repeatCount) }
        var delay by rememberSaveable { mutableStateOf(ui.repeatDelay) }
        var waitBefore by rememberSaveable { mutableStateOf(ui.waitBefore) }
        var waitAfter by rememberSaveable { mutableStateOf(ui.waitAfter) }

        LaunchedEffect(ui.name) { if (ui.name != name) name = ui.name }
        LaunchedEffect(ui.repeatCount) { if (ui.repeatCount != count) count = ui.repeatCount }
        LaunchedEffect(ui.repeatDelay) { if (ui.repeatDelay != delay) delay = ui.repeatDelay }
        LaunchedEffect(ui.waitBefore) { if (ui.waitBefore != waitBefore) waitBefore = ui.waitBefore }
        LaunchedEffect(ui.waitAfter) { if (ui.waitAfter != waitAfter) waitAfter = ui.waitAfter }

        Surface(
            shape = OverlayDialogShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            MacrionDialogSurface {
                Column(Modifier.fillMaxWidth()) {
                    TopBar(
                        title = ui.name.ifBlank { stringResource(R.string.dialog_title_split_action) },
                        saveEnabled = ui.canBeSaved,
                        onDismiss = ::onDismissDialog,
                        onDelete = ::onDeleteDialog,
                        onSave = ::onSaveDialog,
                        headerActions = {
                            if (combinationOptions != null) DumbActionHeaderMenu(
                                showNewOptions = false,
                                canAdd = false,
                                canCombineExisting = existing.isNotEmpty(),
                                canUnsplit = ui.canUnsplit && ui.canBeSaved,
                                onExisting = { showingExistingPicker = true },
                                onUnsplit = ::onUnsplitDialog,
                            )
                        },
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MacrionTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                viewModel.setName(it)
                            },
                            label = stringResource(R.string.input_field_label_name),
                            isError = ui.nameError,
                            maxLength = context.resources.getInteger(R.integer.name_max_length),
                        )

                        Row(verticalAlignment = Alignment.Top) {
                            NumericField(
                                value = count,
                                label = stringResource(R.string.input_field_label_repeat_count),
                                isError = ui.repeatCountError,
                                onValueChanged = {
                                    count = it
                                    viewModel.setRepeatCount(it.toIntOrNull() ?: 0)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !ui.isRepeatInfinite,
                            )
                            Spacer(Modifier.width(16.dp))
                            OutlinedIconToggleButton(
                                checked = ui.isRepeatInfinite,
                                onCheckedChange = { viewModel.toggleInfiniteRepeat() },
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(48.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_infinite),
                                    contentDescription = stringResource(R.string.item_desc_dumb_repeat_infinite),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }

                        NumericField(
                            value = delay,
                            label = stringResource(R.string.input_field_label_repeat_delay),
                            isError = ui.repeatDelayError,
                            onValueChanged = {
                                delay = it
                                viewModel.setRepeatDelay(it.toLongOrNull() ?: 0L)
                            },
                        )

                        ActionDelaysCard(
                            waitBefore = waitBefore,
                            waitAfter = waitAfter,
                            onWaitBeforeChanged = {
                                waitBefore = it
                                viewModel.setWaitBeforeMs(it.takeIf { it.isNotBlank() }?.let { it.toLongOrNull() ?: -1L })
                            },
                            onWaitAfterChanged = {
                                waitAfter = it
                                viewModel.setWaitAfterMs(it.takeIf { it.isNotBlank() }?.let { it.toLongOrNull() ?: -1L })
                            },
                        )

                        Text(stringResource(R.string.split_repeat_help), style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${stringResource(R.string.split_action_touch_actions_header)} (${ui.subActions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                        )

                        io.github.vibhor1102.macrion.core.ui.compose.CombinedGestureSummary(
                            configuredCount = ui.subActions.count { it.isComplete },
                            touchCount = ui.subActions.size,
                            durationMs = ui.durationMs,
                        )
                        Text(
                            text = stringResource(R.string.split_action_timing_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ui.subActions.forEach { subItem ->
                            SubActionCard(
                                item = subItem,
                                canDelete = ui.canDeleteSubAction,
                                onClick = {
                                    val currentParent = viewModel.getEditedDumbSplit() ?: return@SubActionCard
                                    if (onConfigureSubAction != null) {
                                        onConfigureSubAction.invoke(currentParent, subItem.index)
                                    } else {
                                        configureSubActionInternally(subItem.action, subItem.index, ui.canDeleteSubAction)
                                    }
                                },
                                onDelete = { viewModel.removeSubAction(subItem.index) },
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = viewModel::addSwipe,
                                enabled = ui.subActions.size < 10,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    painter = painterResource(UiR.drawable.ic_add),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.split_action_add_swipe), maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = viewModel::addClick,
                                enabled = ui.subActions.size < 10,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    painter = painterResource(UiR.drawable.ic_add),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.split_action_add_click), maxLines = 1)
                            }
                        }

                    }
                }
            }
        }
    }

    @Composable
    private fun TopBar(
        title: String,
        saveEnabled: Boolean,
        onDismiss: () -> Unit,
        onDelete: () -> Unit,
        onSave: () -> Unit,
        headerActions: @Composable () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(painterResource(UiR.drawable.ic_cancel), null)
            }
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            headerActions()
            FilledTonalIconButton(onClick = onDelete) {
                Icon(painterResource(UiR.drawable.ic_delete), null)
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = onSave, enabled = saveEnabled) {
                Icon(painterResource(UiR.drawable.ic_save_filled), null)
            }
        }
    }

    @Composable
    private fun SubActionCard(
        item: DumbSubActionItemUiState,
        canDelete: Boolean,
        onClick: () -> Unit,
        onDelete: () -> Unit,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            border = if (!item.isComplete) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${item.index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Spacer(Modifier.width(10.dp))

                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!item.isComplete) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            painter = painterResource(UiR.drawable.ic_delete),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(UiR.drawable.ic_chevron_right),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    private fun configureSubActionInternally(action: DumbAction, subIndex: Int, canDelete: Boolean) {
        when (action) {
            is DumbAction.DumbSwipe -> {
                overlayManager.navigateTo(
                    context = context,
                    newOverlay = DumbSwipeDialog(
                        dumbSwipe = action,
                        onConfirmClicked = { updated -> viewModel.updateSubAction(subIndex, updated) },
                        onDeleteClicked = { if (canDelete) viewModel.removeSubAction(subIndex) },
                        onDismissClicked = {},
                        isCombinedChild = true,
                        canDelete = canDelete,
                    ),
                    hideCurrent = true,
                )
            }
            is DumbAction.DumbClick -> {
                overlayManager.navigateTo(
                    context = context,
                    newOverlay = DumbClickDialog(
                        dumbClick = action,
                        onConfirmClicked = { updated -> viewModel.updateSubAction(subIndex, updated) },
                        onDeleteClicked = { if (canDelete) viewModel.removeSubAction(subIndex) },
                        onDismissClicked = {},
                        isCombinedChild = true,
                        canDelete = canDelete,
                    ),
                    hideCurrent = true,
                )
            }
            else -> {}
        }
    }

    override fun back() {
        if (showingExistingPicker) { showingExistingPicker = false; return }
        if (viewModel.hasUnsavedModifications()) showDiscardConfirmation = true
        else dismissDraft()
    }

    private fun dismissDraft() {
        onDismissClicked()
        super.back()
    }

    private fun onDismissDialog() = back()

    private fun onSaveDialog() {
        viewModel.getEditedDumbSplit()?.let {
            onConfirmClicked(it)
            super.back()
            if (closeSourceEditorOnSave) overlayManager.navigateUp(context)
        }
    }

    private fun onDeleteDialog() {
        viewModel.getEditedDumbSplit()?.let {
            onDeleteClicked(it)
            super.back()
        }
    }

    private fun onUnsplitDialog() {
        viewModel.getEditedDumbSplit()?.let {
            onUnsplitClicked?.invoke(it)
            super.back()
        }
    }
}
