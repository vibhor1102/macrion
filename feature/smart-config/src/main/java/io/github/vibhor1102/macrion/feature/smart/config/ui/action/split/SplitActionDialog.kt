/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.split

import android.app.Dialog
import android.util.Log
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.domain.model.action.SplitAction
import io.github.vibhor1102.macrion.core.ui.R as UiR
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.OverlayDialogShape
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.OnActionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import kotlinx.coroutines.launch

class SplitActionDialog(
    private val listener: OnActionConfigCompleteListener,
    private val onConfigureSubAction: ((SplitAction, Int) -> Unit)? = null,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: SplitActionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { splitActionViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@SplitActionDialog.Content() } }
    }

    override fun onDialogCreated(dialog: Dialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.isEditingAction.collect { isEditing ->
                    if (!isEditing) {
                        Log.i("SplitActionDialog", "Action edition stopped, closing dialog")
                        finish()
                    }
                }
            }
        }
    }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val ui = state ?: return
        var name by rememberSaveable { mutableStateOf(ui.name) }
        LaunchedEffect(ui.name) {
            if (ui.name != name) name = ui.name
        }

        Surface(
            shape = OverlayDialogShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            MacrionDialogSurface {
                Column(Modifier.fillMaxWidth()) {
                    TopBar(
                        title = ui.name.ifBlank { stringResource(R.string.dialog_title_split_action) },
                        saveEnabled = ui.canBeSaved,
                        onDismiss = ::back,
                        onDelete = ::onDeleteClicked,
                        onSave = ::onSaveClicked,
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
                            label = stringResource(R.string.generic_name),
                            isError = ui.nameError,
                            maxLength = context.resources.getInteger(R.integer.name_max_length),
                        )

                        Text(
                            text = "${stringResource(R.string.split_action_touch_actions_header)} (${ui.subActions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                        )

                        ui.subActions.forEach { subItem ->
                            SubActionCard(
                                item = subItem,
                                canDelete = ui.canDeleteSubAction,
                                onClick = {
                                    val currentParent = viewModel.getEditedSplit() ?: return@SubActionCard
                                    onConfigureSubAction?.invoke(currentParent, subItem.index)
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

                        TextButton(
                            onClick = ::onUnsplitClicked,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Icon(
                                painter = painterResource(UiR.drawable.ic_drag_indicator),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.split_action_unsplit))
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
        item: SubActionItemUiState,
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
                        modifier = Modifier.size(32.dp),
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

    override fun back() {
        if (viewModel.hasUnsavedModifications()) {
            context.showCloseWithoutSavingDialog {
                listener.onDismissClicked()
                super.back()
            }
            return
        }
        listener.onDismissClicked()
        super.back()
    }

    private fun onSaveClicked() {
        viewModel.save()
        listener.onConfirmClicked()
        super.back()
    }

    private fun onDeleteClicked() {
        viewModel.delete()
        listener.onDeleteClicked()
        super.back()
    }

    private fun onUnsplitClicked() {
        viewModel.unsplit()
        listener.onDismissClicked()
        super.back()
    }
}
