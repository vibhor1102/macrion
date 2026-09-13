/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.intent

import android.content.ComponentName
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.app.Dialog
import io.github.vibhor1102.macrion.core.android.application.AndroidApplicationInfo
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.domain.model.action.intent.IntentExtra
import io.github.vibhor1102.macrion.core.ui.bindings.dropdown.DropdownItem
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.OnActionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.intent.activities.ActivitySelectionDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.intent.component.ComponentSelectionDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.intent.extras.ExtraConfigDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.intent.flags.FlagsSelectionDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.intent.IntentActionsSelectionDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import kotlinx.coroutines.launch

class IntentDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.INTENT.name

    private val viewModel: IntentViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { intentViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@IntentDialog.Content() } }
    }

    override fun onDialogCreated(dialog: Dialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.isEditingAction.collect(::onActionEditingStateChanged)
            }
        }
    }

    @Composable
    private fun Content() {
        val advanced by viewModel.advanced.collectAsStateWithLifecycle(initialValue = viewModel.isAdvanced())
        val name by viewModel.name.collectAsStateWithLifecycle(initialValue = null)
        val nameError by viewModel.nameError.collectAsStateWithLifecycle(initialValue = false)
        val valid by viewModel.isValidAction.collectAsStateWithLifecycle(initialValue = false)

        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column {
                TopBar(valid)
                ModeSelector(advanced)
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MacrionTextField(
                        value = name.orEmpty(),
                        onValueChange = viewModel::setName,
                        label = context.getString(R.string.generic_name),
                        isError = nameError,
                        maxLength = context.resources.getInteger(R.integer.name_max_length),
                    )
                    if (advanced) AdvancedContent() else SimpleContent()
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    @Composable
    private fun TopBar(saveEnabled: Boolean) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
            Text(
                text = context.getString(R.string.dialog_title_intent),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            FilledTonalIconButton(onClick = ::onDeleteButtonClicked) {
                Icon(painterResource(R.drawable.ic_delete), null)
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = ::onSaveButtonClicked, enabled = saveEnabled) {
                Icon(painterResource(R.drawable.ic_save_filled), null)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ModeSelector(advanced: Boolean) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            listOf(false to R.string.menu_item_title_simple, true to R.string.menu_item_title_advanced)
                .forEachIndexed { index, (isAdvanced, title) ->
                    SegmentedButton(
                        selected = advanced == isAdvanced,
                        onClick = { viewModel.setIsAdvancedConfiguration(isAdvanced) },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                        label = { Text(stringResource(title)) },
                    )
                }
        }
    }

    @Composable
    private fun SimpleContent() {
        val activity by viewModel.activityInfo.collectAsStateWithLifecycle(initialValue = null)
        Card(
            modifier = Modifier.fillMaxWidth().clickable { showApplicationSelectionDialog() },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (activity != null) ApplicationIcon(activity!!)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = activity?.name ?: context.getString(R.string.field_application_selection_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = activity?.componentName?.packageName
                            ?: context.getString(R.string.field_application_selection_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(painterResource(R.drawable.ic_search), contentDescription = null)
            }
        }
    }

    @Composable
    private fun AdvancedContent() {
        val sendingType by viewModel.sendingType.collectAsStateWithLifecycle(initialValue = null)
        val action by viewModel.action.collectAsStateWithLifecycle(initialValue = null)
        val actionError by viewModel.actionError.collectAsStateWithLifecycle(initialValue = false)
        val flags by viewModel.flags.collectAsStateWithLifecycle(initialValue = "")
        val component by viewModel.componentName.collectAsStateWithLifecycle(initialValue = null)
        val componentError by viewModel.componentNameError.collectAsStateWithLifecycle(initialValue = false)
        val extras by viewModel.extras.collectAsStateWithLifecycle(initialValue = emptyList())
        val isBroadcast = sendingType == viewModel.sendingTypeBroadcast
        var flagsInput by rememberSaveable { mutableStateOf(flags) }
        var componentInput by rememberSaveable { mutableStateOf(component.orEmpty()) }
        LaunchedEffect(flags) { flagsInput = flags }
        LaunchedEffect(component) { component?.let { componentInput = it } }

        sendingType?.let {
            DropdownField(
                label = context.getString(R.string.dropdown_intent_sending_type_label),
                selected = it,
                items = viewModel.sendingTypeItems,
                onSelected = viewModel::setSendingType,
            )
        }
        PickerTextField(
            value = action.orEmpty(),
            onValueChange = viewModel::setIntentAction,
            label = context.getString(R.string.field_intent_action_label),
            isError = actionError,
            showPicker = !isBroadcast,
            onPickerClick = ::showActionsDialog,
        )
        PickerTextField(
            value = flagsInput,
            onValueChange = { value ->
                if (value.isEmpty() || value == "-" || value.toIntOrNull() != null) {
                    flagsInput = value
                    viewModel.setIntentFlags(value.toIntOrNull())
                }
            },
            label = context.getString(R.string.field_intent_flags_label),
            showPicker = true,
            onPickerClick = ::showFlagsDialog,
            keyboardType = KeyboardType.Number,
        )
        PickerTextField(
            value = componentInput,
            onValueChange = { value ->
                componentInput = value
                viewModel.setComponentName(value)
            },
            label = context.getString(R.string.field_intent_component_name_label),
            isError = componentError,
            showPicker = !isBroadcast,
            onPickerClick = ::showComponentNameDialog,
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = context.getString(R.string.field_intent_extra_title),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(extras, key = { item ->
                        when (item) {
                            ExtraListItem.AddExtraItem -> "add"
                            is ExtraListItem.ExtraItem -> item.extra.id.let { id ->
                                "extra:${id.databaseId}:${id.tempId.orEmpty()}"
                            }
                        }
                    }) { item ->
                        when (item) {
                            ExtraListItem.AddExtraItem -> AddExtraCard()
                            is ExtraListItem.ExtraItem -> ExtraCard(item)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PickerTextField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        isError: Boolean = false,
        showPicker: Boolean,
        onPickerClick: () -> Unit,
        keyboardType: KeyboardType = KeyboardType.Text,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = isError,
            keyboardOptions = macrionDoneKeyboardOptions(keyboardType),
            keyboardActions = macrionDoneKeyboardActions(),
            trailingIcon = if (showPicker) ({
                IconButton(onClick = onPickerClick) { Icon(painterResource(R.drawable.ic_search), null) }
            }) else null,
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DropdownField(
        label: String,
        selected: DropdownItem,
        items: List<DropdownItem>,
        onSelected: (DropdownItem) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = stringResource(selected.title),
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(stringResource(item.title)) },
                        onClick = { onSelected(item); expanded = false },
                    )
                }
            }
        }
    }

    @Composable
    private fun AddExtraCard() {
        OutlinedCard(
            modifier = Modifier.size(width = 128.dp, height = 104.dp)
                .clickable { showExtraDialog(viewModel.createNewExtra()) },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null)
            }
        }
    }

    @Composable
    private fun ExtraCard(item: ExtraListItem.ExtraItem) {
        OutlinedCard(
            modifier = Modifier.size(width = 128.dp, height = 104.dp)
                .clickable { showExtraDialog(item.extra) },
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                HorizontalDivider()
                Box(Modifier.weight(1f).fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    Text(item.value, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    @Composable
    private fun ApplicationIcon(info: AndroidApplicationInfo) {
        val bitmap = remember(info.componentName) { info.icon.toBitmap().asImageBitmap() }
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            contentScale = ContentScale.Fit,
        )
    }

    override fun back() {
        if (viewModel.hasUnsavedModifications()) {
            context.showCloseWithoutSavingDialog { listener.onDismissClicked(); super.back() }
            return
        }
        listener.onDismissClicked(); super.back()
    }

    private fun onSaveButtonClicked() {
        viewModel.saveLastConfig(); listener.onConfirmClicked(); super.back()
    }
    private fun onDeleteButtonClicked() { listener.onDeleteClicked(); super.back() }

    private fun showApplicationSelectionDialog() {
        overlayManager.navigateTo(context, ActivitySelectionDialog(viewModel::setActivitySelected), hideCurrent = false)
    }

    private fun showActionsDialog() {
        overlayManager.navigateTo(
            context,
            IntentActionsSelectionDialog(viewModel.getConfiguredIntentAction(), { viewModel.setIntentAction(it.orEmpty()) }),
            hideCurrent = true,
        )
    }

    private fun showFlagsDialog() {
        overlayManager.navigateTo(
            context,
            FlagsSelectionDialog(
                viewModel.getConfiguredIntentFlags(),
                !viewModel.isConfiguredIntentBroadcast(),
                viewModel::setIntentFlags,
            ),
            hideCurrent = true,
        )
    }

    private fun showComponentNameDialog() {
        overlayManager.navigateTo(
            context,
            ComponentSelectionDialog(viewModel::setComponentName),
            hideCurrent = true,
        )
    }

    private fun showExtraDialog(extra: IntentExtra<out Any>) {
        viewModel.startIntentExtraEdition(extra)
        overlayManager.navigateTo(
            context,
            ExtraConfigDialog(
                viewModel::saveIntentExtraEdition,
                viewModel::deleteIntentExtraEvent,
                viewModel::dismissIntentExtraEvent,
            ),
            hideCurrent = false,
        )
    }

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) {
            Log.e(TAG, "Closing IntentDialog because there is no action edited")
            finish()
        }
    }
}

private const val TAG = "IntentDialog"

private fun Long?.orEmpty(): String = this?.toString().orEmpty()
